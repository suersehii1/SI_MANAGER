package com.sipm;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA 체크리스트 4(CRUD) — 이슈 생성 → Grid 조회 반영, 수정 저장, 정렬/페이징/필터, 삭제 권한, 검증 오류.
 * 시드: SI-2026-001에 이슈 3건(OPEN/HIGH, IN_PROGRESS/MEDIUM, RESOLVED/LOW, reporter=admin), SI-2026-002는 0건.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IssueControllerTest extends ApiTestSupport {

    @Test
    void createdIssueAppearsAtTopOfProjectGrid() throws Exception {
        String user = user();
        long p2 = projectId("SI-2026-002");
        Map<String, Object> body = issue(p2, "  신규 이슈 제목  ");
        body.put("priority", "HIGH");
        body.put("assigneeId", userId("kim"));
        body.put("dueDate", "2026-04-10");

        JsonNode created = json(mvc.perform(postJson(user, "/api/issues", body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/issues/")))
                .andExpect(jsonPath("$.title").value("신규 이슈 제목")) // trim 후 저장
                .andExpect(jsonPath("$.status").value("OPEN"))      // 기본값
                .andExpect(jsonPath("$.progress").value(0))
                .andExpect(jsonPath("$.reporter.username").value("user"))
                .andExpect(jsonPath("$.assignee.username").value("kim"))
                .andExpect(jsonPath("$.attachments.length()").value(0)));
        long id = created.get("id").asLong();

        // 프로젝트 필터 Grid 조회
        mvc.perform(getAs(user, "/api/issues?projectId=" + p2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.content[0].projectCode").value("SI-2026-002"))
                .andExpect(jsonPath("$.content[0].attachmentCount").value(0));

        // 필터 없이 기본 정렬(createdAt desc, id desc) → 첫 행
        mvc.perform(getAs(user, "/api/issues"))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].id").value(id));
    }

    @Test
    void updatePersistsAllFields() throws Exception {
        String user = user();
        long p1 = projectId("SI-2026-001");
        long p2 = projectId("SI-2026-002");
        long id = json(mvc.perform(postJson(user, "/api/issues", issue(p1, "수정 전")))).get("id").asLong();

        Map<String, Object> body = issue(p2, "수정 후");
        body.put("description", "설명 추가");
        body.put("status", "RESOLVED");
        body.put("priority", "CRITICAL");
        body.put("progress", 80);
        body.put("assigneeId", userId("lee"));
        body.put("dueDate", "2026-05-01");
        mvc.perform(putJson(user, "/api/issues/" + id, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정 후"));

        mvc.perform(getAs(user, "/api/issues/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(p2)) // projectId 변경 허용
                .andExpect(jsonPath("$.description").value("설명 추가"))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.progress").value(80))
                .andExpect(jsonPath("$.assignee.username").value("lee"))
                .andExpect(jsonPath("$.dueDate").value("2026-05-01"));

        // 담당자 해제
        body.remove("assigneeId");
        mvc.perform(putJson(user, "/api/issues/" + id, body))
                .andExpect(jsonPath("$.assignee").value(nullValue()));
    }

    @Test
    void sortByPriorityAndStatusUsesRank() throws Exception {
        String user = user();
        long p1 = projectId("SI-2026-001");
        Map<String, Object> body = issue(p1, "치명 이슈");
        body.put("priority", "CRITICAL");
        body.put("status", "CLOSED");
        mvc.perform(postJson(user, "/api/issues", body)).andExpect(status().isCreated());

        // 알파벳 순이 아니라 CRITICAL → HIGH → MEDIUM → LOW
        mvc.perform(getAs(user, "/api/issues?projectId=" + p1 + "&sort=priority,desc"))
                .andExpect(jsonPath("$.content[0].priority").value("CRITICAL"))
                .andExpect(jsonPath("$.content[1].priority").value("HIGH"))
                .andExpect(jsonPath("$.content[2].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.content[3].priority").value("LOW"));

        // OPEN → IN_PROGRESS → RESOLVED → CLOSED
        mvc.perform(getAs(user, "/api/issues?projectId=" + p1 + "&sort=status,asc"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.content[1].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.content[2].status").value("RESOLVED"))
                .andExpect(jsonPath("$.content[3].status").value("CLOSED"));

        mvc.perform(getAs(user, "/api/issues?sort=title,asc&sort=dueDate,desc"))
                .andExpect(status().isOk());
    }

    @Test
    void pagingAndSizeClamp() throws Exception {
        String user = user();
        mvc.perform(getAs(user, "/api/issues?size=2"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
        mvc.perform(getAs(user, "/api/issues?size=2&page=1"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(getAs(user, "/api/issues?size=1000"))
                .andExpect(jsonPath("$.size").value(100));
        mvc.perform(getAs(user, "/api/issues?page=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidSortIsRejected() throws Exception {
        String user = user();
        mvc.perform(getAs(user, "/api/issues?sort=password,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
        mvc.perform(getAs(user, "/api/issues?sort=title,sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
    }

    @Test
    void filtersNarrowTheGrid() throws Exception {
        String user = user();
        mvc.perform(getAs(user, "/api/issues?status=OPEN"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("로그인 화면 오류"));
        mvc.perform(getAs(user, "/api/issues?priority=LOW"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(getAs(user, "/api/issues?assigneeId=" + userId("kim")))
                .andExpect(jsonPath("$.content[0].assignee.username").value("kim"));
        // 제목/설명 부분일치, 대소문자 무시
        mvc.perform(getAs(user, "/api/issues?keyword=api"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("주문 API 성능 개선"));
        // %, _ 는 와일드카드가 아니라 문자로 취급
        mvc.perform(getAs(user, "/api/issues").param("keyword", "%"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(getAs(user, "/api/issues").param("keyword", "_"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(getAs(user, "/api/issues?status=FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("TYPE_MISMATCH"));
        mvc.perform(getAs(user, "/api/issues?keyword=" + "a".repeat(101)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validationErrors() throws Exception {
        String user = user();
        long p1 = projectId("SI-2026-001");

        mvc.perform(postJson(user, "/api/issues", issue(p1, "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("NotBlank"));

        Map<String, Object> body = issue(p1, "진척률 초과");
        body.put("progress", 101);
        mvc.perform(postJson(user, "/api/issues", body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("progress"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("Max"));

        mvc.perform(postJson(user, "/api/issues", Map.of("title", "프로젝트 없음")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("projectId"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("NotNull"));

        mvc.perform(postJson(user, "/api/issues", issue(999999L, "없는 프로젝트")))
                .andExpect(status().isNotFound());

        mvc.perform(postJson(user, "/api/issues", "{\"projectId\":" + p1 + ",\"title\":\"x\",\"status\":\"FOO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mvc.perform(postJson(user, "/api/issues", "{not json"))
                .andExpect(status().isBadRequest());

        mvc.perform(getAs(user, "/api/issues/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void disabledAssigneeIsRejected() throws Exception {
        String admin = admin();
        long leeId = userId("lee");
        mvc.perform(putJson(admin, "/api/admin/users/" + leeId,
                        Map.of("name", "이영희", "roles", List.of("ROLE_USER"), "enabled", false)))
                .andExpect(status().isOk());
        Map<String, Object> body = issue(projectId("SI-2026-001"), "비활성 담당자");
        body.put("assigneeId", leeId);
        mvc.perform(postJson(admin, "/api/issues", body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_DISABLED"));
    }

    @Test
    void deleteIsAllowedOnlyForReporterOrAdmin() throws Exception {
        String user = user();
        String kim = kim();
        String admin = admin();
        long p1 = projectId("SI-2026-001");

        // 시드 이슈(reporter=admin)를 일반 사용자가 삭제 → 403
        long seedId = json(mvc.perform(getAs(user, "/api/issues?projectId=" + p1)))
                .get("content").get(0).get("id").asLong();
        mvc.perform(deleteAs(user, "/api/issues/" + seedId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // 본인이 등록한 이슈는 삭제 가능, 다른 USER는 불가
        long own = json(mvc.perform(postJson(user, "/api/issues", issue(p1, "내 이슈")))).get("id").asLong();
        mvc.perform(deleteAs(kim, "/api/issues/" + own)).andExpect(status().isForbidden());
        mvc.perform(deleteAs(user, "/api/issues/" + own)).andExpect(status().isNoContent());
        mvc.perform(getAs(user, "/api/issues/" + own)).andExpect(status().isNotFound());

        // ADMIN은 누구의 이슈든 삭제 가능
        mvc.perform(deleteAs(admin, "/api/issues/" + seedId)).andExpect(status().isNoContent());
        mvc.perform(deleteAs(admin, "/api/issues/" + seedId)).andExpect(status().isNotFound());
    }

    @Test
    void issuesRequireAuthentication() throws Exception {
        mvc.perform(get("/api/issues"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private static Map<String, Object> issue(long projectId, String title) {
        Map<String, Object> m = new HashMap<>();
        m.put("projectId", projectId);
        m.put("title", title);
        return m;
    }
}
