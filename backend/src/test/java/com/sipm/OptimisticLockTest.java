package com.sipm;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 낙관적 잠금(덮어쓰기 방지): 프로젝트 / 이슈 / 인원 할당 / WBS
 * 1) 불러온 version으로 저장 → 200, version 1 증가
 * 2) 그 예전 version으로 다시 저장(= 다른 사람이 먼저 저장한 상황) → 409 CONCURRENT_MODIFICATION
 * 3) version 없이 저장(예전 클라이언트) → 검사하지 않고 200
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OptimisticLockTest extends ApiTestSupport {

    /** GET 응답(JSON)을 수정 요청 본문(Map)으로 바꾼다 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toBody(JsonNode node) {
        return new HashMap<>(om.convertValue(node, Map.class));
    }

    /** 공통 시나리오: 같은 version으로 두 번 저장 → 두 번째는 409 */
    private void assertStaleSaveRejected(String token, String url, Map<String, Object> body, long version,
                                         String field, Object firstValue, Object secondValue) throws Exception {
        body.put("version", version);
        body.put(field, firstValue);
        JsonNode saved = json(mvc.perform(putJson(token, url, body)).andExpect(status().isOk()));
        assertThat(saved.get("version").asLong()).isEqualTo(version + 1);

        // 다른 사람이 위에서 먼저 저장했는데, 예전 version으로 저장 시도 → 덮어쓰지 않고 409
        body.put(field, secondValue);
        mvc.perform(putJson(token, url, body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"))
                .andExpect(jsonPath("$.message").value("다른 사람이 먼저 수정했습니다. 새로고침 후 다시 시도하세요."));

        // version 없이 보내면(예전 화면) 검사하지 않고 저장
        body.remove("version");
        mvc.perform(putJson(token, url, body)).andExpect(status().isOk());
    }

    /**
     * 삭제 공통 시나리오: 다른 사람이 수정해서 버전을 올린 뒤
     * 예전 버전으로 삭제 → 409, 데이터는 그대로 / 최신 버전으로 삭제 → 204
     * (내용이 하나도 안 바뀐 수정은 UPDATE가 실행되지 않아 버전이 그대로이므로 field 값을 실제로 바꾼다)
     */
    private void assertStaleDeleteRejected(String token, String putUrl, Map<String, Object> body, long oldVersion,
                                           String field, Object changedValue,
                                           String deleteUrl, String getUrl) throws Exception {
        body.put("version", oldVersion);
        body.put(field, changedValue);
        long newVersion = json(mvc.perform(putJson(token, putUrl, body)).andExpect(status().isOk())).get("version").asLong();
        assertThat(newVersion).isEqualTo(oldVersion + 1);

        mvc.perform(deleteAs(token, deleteUrl + "?version=" + oldVersion))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));
        mvc.perform(getAs(token, getUrl)).andExpect(status().isOk()); // 삭제되지 않고 남아 있음

        mvc.perform(deleteAs(token, deleteUrl + "?version=" + newVersion)).andExpect(status().isNoContent());
    }

    @Test
    void projectStaleDeleteIsRejected() throws Exception {
        String admin = admin();
        long p2 = projectId("SI-2026-002");
        String url = "/api/admin/projects/" + p2;
        JsonNode project = json(mvc.perform(getAs(admin, url)).andExpect(status().isOk()));
        assertStaleDeleteRejected(admin, url, toBody(project), project.get("version").asLong(),
                "name", "다른 사람이 바꾼 이름", url, url);
        mvc.perform(getAs(admin, url)).andExpect(status().isNotFound());
    }

    @Test
    void issueStaleDeleteIsRejected() throws Exception {
        String admin = admin();
        long issueId = json(mvc.perform(getAs(admin, "/api/issues?page=0&size=1")).andExpect(status().isOk()))
                .get("content").get(0).get("id").asLong();
        String url = "/api/issues/" + issueId;
        JsonNode issue = json(mvc.perform(getAs(admin, url)).andExpect(status().isOk()));
        Map<String, Object> body = toBody(issue);
        body.put("assigneeId", issue.get("assignee").isNull() ? null : issue.get("assignee").get("id").asLong());
        assertStaleDeleteRejected(admin, url, body, issue.get("version").asLong(),
                "title", "다른 사람이 바꾼 제목", url, url);
        mvc.perform(getAs(admin, url)).andExpect(status().isNotFound());
    }

    @Test
    void assignmentStaleDeleteIsRejected() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        String listUrl = "/api/admin/projects/" + p1 + "/assignments";
        JsonNode first = json(mvc.perform(getAs(admin, listUrl)).andExpect(status().isOk())).get(0);
        String url = listUrl + "/" + first.get("id").asLong();
        Map<String, Object> body = toBody(first);
        body.put("userId", first.get("user").get("id").asLong());
        // 할당은 단건 조회 API가 없으므로 "남아 있음"은 목록 조회 성공으로만 확인하고, 마지막에 건수로 삭제 확인
        int before = json(mvc.perform(getAs(admin, listUrl))).size();
        assertStaleDeleteRejected(admin, url, body, first.get("version").asLong(),
                "note", "다른 사람이 바꾼 메모", url, listUrl);
        assertThat(json(mvc.perform(getAs(admin, listUrl))).size()).isEqualTo(before - 1);
    }

    @Test
    void wbsStaleDeleteIsRejected() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        String treeUrl = "/api/projects/" + p1 + "/wbs";
        JsonNode leaf = null;
        for (JsonNode item : json(mvc.perform(getAs(admin, treeUrl))).get("items")) {
            if (!item.get("rolledUp").asBoolean()) {
                leaf = item;
                break;
            }
        }
        assertThat(leaf).isNotNull();
        long leafId = leaf.get("id").asLong();
        int before = json(mvc.perform(getAs(admin, treeUrl))).get("items").size();
        Map<String, Object> body = toBody(leaf);
        body.put("assigneeId", leaf.get("assignee").isNull() ? null : leaf.get("assignee").get("id").asLong());
        assertStaleDeleteRejected(admin, "/api/wbs/" + leafId, body, leaf.get("version").asLong(),
                "deliverable", "다른 사람이 바꾼 산출물", "/api/wbs/" + leafId, treeUrl);
        assertThat(json(mvc.perform(getAs(admin, treeUrl))).get("items").size()).isEqualTo(before - 1);
    }

    @Test
    void projectStaleUpdateIsRejected() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        String url = "/api/admin/projects/" + p1;
        JsonNode project = json(mvc.perform(getAs(admin, url)).andExpect(status().isOk()));
        assertStaleSaveRejected(admin, url, toBody(project), project.get("version").asLong(),
                "name", "차세대 ERP 구축 (A 수정)", "차세대 ERP 구축 (B 수정)");
    }

    @Test
    void issueStaleUpdateIsRejected() throws Exception {
        String admin = admin();
        long issueId = json(mvc.perform(getAs(admin, "/api/issues?page=0&size=1")).andExpect(status().isOk()))
                .get("content").get(0).get("id").asLong();
        String url = "/api/issues/" + issueId;
        JsonNode issue = json(mvc.perform(getAs(admin, url)).andExpect(status().isOk()));
        Map<String, Object> body = toBody(issue);
        body.put("assigneeId", issue.get("assignee").isNull() ? null : issue.get("assignee").get("id").asLong());
        assertStaleSaveRejected(admin, url, body, issue.get("version").asLong(), "title", "A가 고친 제목", "B가 고친 제목");
    }

    @Test
    void assignmentStaleUpdateIsRejected() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        JsonNode first = json(mvc.perform(getAs(admin, "/api/admin/projects/" + p1 + "/assignments"))
                .andExpect(status().isOk())).get(0);
        String url = "/api/admin/projects/" + p1 + "/assignments/" + first.get("id").asLong();
        Map<String, Object> body = toBody(first);
        body.put("userId", first.get("user").get("id").asLong());
        assertStaleSaveRejected(admin, url, body, first.get("version").asLong(), "note", "A 메모", "B 메모");
    }

    @Test
    void wbsStaleUpdateIsRejected() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        JsonNode leaf = null;
        for (JsonNode item : json(mvc.perform(getAs(admin, "/api/projects/" + p1 + "/wbs"))).get("items")) {
            if (!item.get("rolledUp").asBoolean()) {
                leaf = item;
                break;
            }
        }
        assertThat(leaf).isNotNull();
        Map<String, Object> body = toBody(leaf);
        body.put("assigneeId", leaf.get("assignee").isNull() ? null : leaf.get("assignee").get("id").asLong());
        assertStaleSaveRejected(admin, "/api/wbs/" + leaf.get("id").asLong(), body, leaf.get("version").asLong(),
                "deliverable", "A 산출물", "B 산출물");
    }
}
