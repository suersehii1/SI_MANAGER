package com.sipm;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WBS API (#30~#35) 검증 — specs-wbs §W4, W7.
 * 시드(V4): SI-2026-001에 10개 항목(W3.1). 멤버 = kim(PM), lee(DEVELOPER). 'user'는 비멤버 USER.
 * WBS 쓰기 권한: ADMIN 또는 프로젝트 PM (kim). lee는 멤버지만 PM이 아니라 조회만.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WbsControllerTest extends ApiTestSupport {
    private static final String AS_OF = "2026-03-15";

    // ===================== 조회 =====================

    @Test
    void seedTreeMatchesWorkedExample() throws Exception {
        long p1 = projectId("SI-2026-001");
        mvc.perform(getAs(user(), "/api/projects/" + p1 + "/wbs?asOf=" + AS_OF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.code").value("SI-2026-001"))
                .andExpect(jsonPath("$.asOf").value(AS_OF))
                .andExpect(jsonPath("$.itemCount").value(10))
                .andExpect(jsonPath("$.overallProgress").value(53.8))
                // DFS 순서 + 코드
                .andExpect(jsonPath("$.items[0].wbsCode").value("1"))
                .andExpect(jsonPath("$.items[0].name").value("분석"))
                .andExpect(jsonPath("$.items[1].wbsCode").value("1.1"))
                .andExpect(jsonPath("$.items[2].wbsCode").value("1.2"))
                .andExpect(jsonPath("$.items[3].wbsCode").value("2"))
                .andExpect(jsonPath("$.items[4].wbsCode").value("2.1"))
                .andExpect(jsonPath("$.items[5].wbsCode").value("2.2"))
                .andExpect(jsonPath("$.items[6].wbsCode").value("3"))
                .andExpect(jsonPath("$.items[7].wbsCode").value("3.1"))
                .andExpect(jsonPath("$.items[8].wbsCode").value("3.2"))
                .andExpect(jsonPath("$.items[9].wbsCode").value("4"))
                // 1 분석 (집계)
                .andExpect(jsonPath("$.items[0].progress").value(100.0))
                .andExpect(jsonPath("$.items[0].status").value("DONE"))
                .andExpect(jsonPath("$.items[0].plannedStart").value("2026-01-01"))
                .andExpect(jsonPath("$.items[0].plannedEnd").value("2026-01-31"))
                .andExpect(jsonPath("$.items[0].actualStart").value("2026-01-15"))
                .andExpect(jsonPath("$.items[0].actualEnd").value("2026-01-31"))
                .andExpect(jsonPath("$.items[0].rolledUp").value(true))
                .andExpect(jsonPath("$.items[0].childCount").value(2))
                .andExpect(jsonPath("$.items[0].plannedDays").value(31))
                // 2 설계 (집계, 지연)
                .andExpect(jsonPath("$.items[3].progress").value(90.0))
                .andExpect(jsonPath("$.items[3].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.items[3].plannedEnd").value("2026-02-28"))
                .andExpect(jsonPath("$.items[3].actualEnd").value(nullValue()))
                .andExpect(jsonPath("$.items[3].delayed").value(true))
                // 2.2 DB 설계 (리프, 지연)
                .andExpect(jsonPath("$.items[5].name").value("DB 설계"))
                .andExpect(jsonPath("$.items[5].level").value(2))
                .andExpect(jsonPath("$.items[5].progress").value(80.0))
                .andExpect(jsonPath("$.items[5].rolledUp").value(false))
                .andExpect(jsonPath("$.items[5].delayed").value(true))
                .andExpect(jsonPath("$.items[5].plannedDays").value(28))
                .andExpect(jsonPath("$.items[5].weight").value(1.00))
                // 3 개발 (집계)
                .andExpect(jsonPath("$.items[6].progress").value(25.0))
                .andExpect(jsonPath("$.items[6].plannedStart").value("2026-03-01"))
                .andExpect(jsonPath("$.items[6].plannedEnd").value("2026-03-25"))
                .andExpect(jsonPath("$.items[6].actualStart").value("2026-03-02"))
                // 3.1 plannedEnd == asOf → 지연 아님
                .andExpect(jsonPath("$.items[7].delayed").value(false))
                .andExpect(jsonPath("$.items[9].progress").value(0.0))
                .andExpect(jsonPath("$.items[9].status").value("NOT_STARTED"));
    }

    @Test
    void readIsAllowedForEveryoneButCanEditOnlyForAdminOrPm() throws Exception {
        String url = "/api/projects/" + projectId("SI-2026-001") + "/wbs";
        mvc.perform(getAs(user(), url)).andExpect(status().isOk()).andExpect(jsonPath("$.canEdit").value(false));
        // kim = SI-2026-001의 PM → 편집 가능, lee = 같은 프로젝트의 DEVELOPER → 조회만
        mvc.perform(getAs(kim(), url)).andExpect(status().isOk()).andExpect(jsonPath("$.canEdit").value(true));
        mvc.perform(getAs(login("lee", "user1234"), url)).andExpect(status().isOk()).andExpect(jsonPath("$.canEdit").value(false));
        mvc.perform(getAs(admin(), url)).andExpect(status().isOk()).andExpect(jsonPath("$.canEdit").value(true));
        mvc.perform(getAs(user(), "/api/projects/999999/wbs")).andExpect(status().isNotFound());
    }

    @Test
    void membersEndpointListsAssignedUsers() throws Exception {
        mvc.perform(getAs(user(), "/api/projects/" + projectId("SI-2026-001") + "/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.username == 'kim')]").exists())
                .andExpect(jsonPath("$[?(@.username == 'lee')]").exists());
    }

    // ===================== 권한 =====================

    @Test
    void nonMemberUserCannotWrite() throws Exception {
        String user = user();
        long p1 = projectId("SI-2026-001");
        long dbDesign = itemId(p1, "DB 설계");
        mvc.perform(postJson(user, "/api/projects/" + p1 + "/wbs", item("x")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(putJson(user, "/api/wbs/" + dbDesign, item("x"))).andExpect(status().isForbidden());
        mvc.perform(deleteAs(user, "/api/wbs/" + dbDesign)).andExpect(status().isForbidden());
        mvc.perform(postJson(user, "/api/wbs/" + dbDesign + "/move", Map.of("index", 0)))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonPmMemberCannotWrite() throws Exception {
        // lee는 SI-2026-001에 DEVELOPER로 할당된 멤버지만 PM이 아니므로 추가/수정/삭제/이동 모두 403
        String lee = login("lee", "user1234");
        long p1 = projectId("SI-2026-001");
        long dbDesign = itemId(p1, "DB 설계");
        mvc.perform(postJson(lee, "/api/projects/" + p1 + "/wbs", item("x")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(putJson(lee, "/api/wbs/" + dbDesign, item("x"))).andExpect(status().isForbidden());
        mvc.perform(deleteAs(lee, "/api/wbs/" + dbDesign)).andExpect(status().isForbidden());
        mvc.perform(postJson(lee, "/api/wbs/" + dbDesign + "/move", Map.of("index", 0)))
                .andExpect(status().isForbidden());
    }

    @Test
    void pmUserCanCreate() throws Exception {
        long p1 = projectId("SI-2026-001");
        mvc.perform(postJson(kim(), "/api/projects/" + p1 + "/wbs", item("PM이 추가")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/wbs/")))
                .andExpect(jsonPath("$.wbsCode").value("5"))
                .andExpect(jsonPath("$.level").value(1));
    }

    // ===================== 생성 =====================

    @Test
    void createRootChildAndInsertAfter() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        long design = itemId(p1, "설계");
        long screen = itemId(p1, "화면 설계");

        // 하위 마지막에 추가 → 2.3
        Map<String, Object> child = item("API 설계");
        child.put("parentId", design);
        mvc.perform(postJson(admin, "/api/projects/" + p1 + "/wbs", child))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wbsCode").value("2.3"))
                .andExpect(jsonPath("$.parentId").value(design))
                .andExpect(jsonPath("$.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.weight").value(1.00));

        // 2.1 바로 뒤에 삽입 → 새 항목이 2.2, 기존 DB 설계는 2.3으로 밀림
        Map<String, Object> inserted = item("UI 가이드");
        inserted.put("parentId", design);
        inserted.put("insertAfterId", screen);
        mvc.perform(postJson(admin, "/api/projects/" + p1 + "/wbs", inserted))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.wbsCode").value("2.2"));

        JsonNode tree = tree(p1);
        assertThat(codeOf(tree, "DB 설계")).isEqualTo("2.3");
        assertThat(codeOf(tree, "API 설계")).isEqualTo("2.4");
        // 2 설계 진척률: (100 + 0 + 80 + 0) / 4 = 45.0
        assertThat(nodeOf(tree, "설계").get("progress").decimalValue()).isEqualByComparingTo("45.0");
        assertThat(tree.get("itemCount").asInt()).isEqualTo(12);
    }

    @Test
    void leafInputRulesForStatusAndProgress() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        Map<String, Object> done = item("완료 처리");
        done.put("status", "DONE");
        done.put("progress", 10);
        mvc.perform(postJson(admin, "/api/projects/" + p1 + "/wbs", done))
                .andExpect(jsonPath("$.progress").value(100.0));

        Map<String, Object> started = item("진행 처리");
        started.put("progress", 50);
        mvc.perform(postJson(admin, "/api/projects/" + p1 + "/wbs", started))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        Map<String, Object> full = item("100 처리");
        full.put("progress", 100);
        mvc.perform(postJson(admin, "/api/projects/" + p1 + "/wbs", full))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    /** O-04: 완료(DONE) 리프의 진척률만 낮추면 IN_PROGRESS로 전환, 상태를 DONE으로 바꾸면 100 강제. */
    @Test
    void loweringProgressOfDoneLeafSwitchesToInProgress() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        JsonNode req = nodeOf(tree(p1), "요구사항 정의"); // 시드: DONE, 100
        long id = req.get("id").asLong();

        // 그리드 인라인 편집처럼 행 전체(status=DONE 그대로) + progress 80
        Map<String, Object> lowered = fromNode(req);
        lowered.put("progress", 80);
        mvc.perform(putJson(admin, "/api/wbs/" + id, lowered))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.progress").value(80.0));
        // 부모 1 분석 = (80 + 100) / 2 = 90.0
        assertThat(nodeOf(tree(p1), "분석").get("progress").decimalValue()).isEqualByComparingTo("90.0");

        // 상태를 IN_PROGRESS → DONE으로 바꾸면 진척률 입력과 무관하게 100
        Map<String, Object> done = fromNode(nodeOf(tree(p1), "요구사항 정의"));
        done.put("status", "DONE");
        done.put("progress", 60);
        mvc.perform(putJson(admin, "/api/wbs/" + id, done))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.progress").value(100.0));
    }

    // ===================== 수정 =====================

    @Test
    void updatingParentIgnoresRolledUpFields() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        long design = itemId(p1, "설계");

        Map<String, Object> body = item("설계(수정)");
        body.put("status", "NOT_STARTED");
        body.put("progress", 0);
        body.put("plannedStart", "2026-01-01");
        body.put("plannedEnd", "2026-01-02");
        mvc.perform(putJson(admin, "/api/wbs/" + design, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("설계(수정)"))
                .andExpect(jsonPath("$.progress").value(90.0))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.plannedStart").value("2026-02-01"))
                .andExpect(jsonPath("$.rolledUp").value(true));
    }

    @Test
    void leafWeightChangeUpdatesParentAndOverall() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        JsonNode db = nodeOf(tree(p1), "DB 설계");
        Map<String, Object> body = fromNode(db);
        body.put("weight", 3.00);
        mvc.perform(putJson(admin, "/api/wbs/" + db.get("id").asLong(), body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weight").value(3.00))
                .andExpect(jsonPath("$.progress").value(80.0));

        JsonNode tree = tree(p1);
        // W3.1: 2 = (100×1 + 80×3) / 4 = 85.0, overall = 52.5
        assertThat(nodeOf(tree, "설계").get("progress").decimalValue()).isEqualByComparingTo("85.0");
        assertThat(tree.get("overallProgress").decimalValue()).isEqualByComparingTo("52.5");
    }

    @Test
    void leafProgressChangeRollsUp() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        JsonNode db = nodeOf(tree(p1), "DB 설계");
        Map<String, Object> body = fromNode(db);
        body.put("progress", 100);
        body.put("status", "DONE");
        body.put("actualEnd", "2026-02-27");
        mvc.perform(putJson(admin, "/api/wbs/" + db.get("id").asLong(), body)).andExpect(status().isOk());

        JsonNode design = nodeOf(tree(p1), "설계");
        assertThat(design.get("progress").decimalValue()).isEqualByComparingTo("100.0");
        assertThat(design.get("status").asText()).isEqualTo("DONE");
        assertThat(design.get("actualEnd").asText()).isEqualTo("2026-02-27");
        assertThat(design.get("delayed").asBoolean()).isFalse();
    }

    // ===================== 검증 오류 =====================

    @Test
    void validationErrors() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001"); // 계획 2026-01-01 ~ 2026-03-31
        String url = "/api/projects/" + p1 + "/wbs";

        Map<String, Object> reversed = item("날짜 역전");
        reversed.put("plannedStart", "2026-03-10");
        reversed.put("plannedEnd", "2026-03-01");
        mvc.perform(postJson(admin, url, reversed))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("plannedEnd"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("DATE_RANGE"));

        Map<String, Object> outside = item("기간 밖");
        outside.put("plannedStart", "2026-03-20");
        outside.put("plannedEnd", "2026-04-10");
        mvc.perform(postJson(admin, url, outside))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WBS_OUT_OF_PROJECT_PERIOD"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("plannedEnd"));

        Map<String, Object> nonMember = item("비멤버 담당자");
        nonMember.put("assigneeId", userId("user"));
        mvc.perform(postJson(admin, url, nonMember))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WBS_ASSIGNEE_NOT_MEMBER"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("assigneeId"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("NOT_MEMBER"));

        mvc.perform(postJson(admin, url, item("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));

        Map<String, Object> badProgress = item("진척 초과");
        badProgress.put("progress", 101);
        mvc.perform(postJson(admin, url, badProgress)).andExpect(status().isBadRequest());

        // 다른 프로젝트의 항목을 부모로 지정
        Map<String, Object> wrongParent = item("다른 프로젝트 부모");
        wrongParent.put("parentId", itemId(p1, "설계"));
        mvc.perform(postJson(admin, "/api/projects/" + projectId("SI-2026-002") + "/wbs", wrongParent))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WBS_INVALID_PARENT"));
    }

    @Test
    void depthSixIsRejected() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        // 1.1 (깊이 2) 아래로 3단계 더 → 깊이 5까지 허용
        Long parent = itemId(p1, "요구사항 정의");
        for (int depth = 3; depth <= 5; depth++) {
            Map<String, Object> body = item("깊이" + depth);
            body.put("parentId", parent);
            JsonNode created = json(mvc.perform(postJson(admin, "/api/projects/" + p1 + "/wbs", body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.level").value(depth)));
            parent = created.get("id").asLong();
        }
        Map<String, Object> tooDeep = item("깊이6");
        tooDeep.put("parentId", parent);
        mvc.perform(postJson(admin, "/api/projects/" + p1 + "/wbs", tooDeep))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WBS_DEPTH_EXCEEDED"));

        // 높이 4인 서브트리(1.1)를 깊이 2 노드 아래로 이동 → 2 + 4 = 6 > 5
        mvc.perform(postJson(admin, "/api/wbs/" + itemId(p1, "요구사항 정의") + "/move",
                        Map.of("parentId", itemId(p1, "DB 설계"), "index", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WBS_DEPTH_EXCEEDED"));
    }

    // ===================== 이동 =====================

    @Test
    void indentOutdentAndReorderRenumbersCodes() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        long test = itemId(p1, "테스트 및 이행"); // 4
        long dev = itemId(p1, "개발");           // 3 (자식 2개)

        // 들여쓰기: 4 → 3의 마지막 자식(3.3)
        JsonNode moved = json(mvc.perform(postJson(admin, "/api/wbs/" + test + "/move",
                        Map.of("parentId", dev, "index", 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemCount").value(10)));
        assertThat(codeOf(moved, "테스트 및 이행")).isEqualTo("3.3");
        // 개발 = (50 + 0 + 0) / 3 = 16.7, overall = (100 + 90 + 16.7) / 3 = 68.9
        assertThat(nodeOf(moved, "개발").get("progress").decimalValue()).isEqualByComparingTo("16.7");
        assertThat(moved.get("overallProgress").decimalValue()).isEqualByComparingTo("68.9");

        // 내어쓰기: 3.3 → 루트, 3 바로 뒤(index 3) → 다시 4
        moved = json(mvc.perform(postJson(admin, "/api/wbs/" + test + "/move", Map.of("index", 3)))
                .andExpect(status().isOk()));
        assertThat(codeOf(moved, "테스트 및 이행")).isEqualTo("4");

        // 위로: 4 → index 0 → 1, 기존 1·2·3은 2·3·4로 재번호
        moved = json(mvc.perform(postJson(admin, "/api/wbs/" + test + "/move", Map.of("index", 0)))
                .andExpect(status().isOk()));
        assertThat(codeOf(moved, "테스트 및 이행")).isEqualTo("1");
        assertThat(codeOf(moved, "분석")).isEqualTo("2");
        assertThat(codeOf(moved, "요구사항 정의")).isEqualTo("2.1");
        assertThat(codeOf(moved, "개발")).isEqualTo("4");
        assertThat(moved.get("items").get(0).get("name").asText()).isEqualTo("테스트 및 이행");

        // index가 형제 수보다 크면 마지막
        moved = json(mvc.perform(postJson(admin, "/api/wbs/" + test + "/move", Map.of("index", 99)))
                .andExpect(status().isOk()));
        assertThat(codeOf(moved, "테스트 및 이행")).isEqualTo("4");
    }

    @Test
    void moveIntoSelfOrDescendantIsRejected() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        long design = itemId(p1, "설계");
        mvc.perform(postJson(admin, "/api/wbs/" + design + "/move", Map.of("parentId", design, "index", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WBS_INVALID_MOVE"));
        mvc.perform(postJson(admin, "/api/wbs/" + design + "/move",
                        Map.of("parentId", itemId(p1, "DB 설계"), "index", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WBS_INVALID_MOVE"));
        mvc.perform(postJson(admin, "/api/wbs/" + design + "/move", Map.of("parentId", 999999, "index", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WBS_INVALID_PARENT"));
        mvc.perform(postJson(admin, "/api/wbs/" + design + "/move", Map.of("index", -1)))
                .andExpect(status().isBadRequest());
    }

    // ===================== 삭제 =====================

    @Test
    void deleteRemovesSubtreeAndRenumbersSiblings() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        long design = itemId(p1, "설계");
        long db = itemId(p1, "DB 설계");

        mvc.perform(deleteAs(admin, "/api/wbs/" + design)).andExpect(status().isNoContent());

        JsonNode tree = tree(p1);
        assertThat(tree.get("itemCount").asInt()).isEqualTo(7);
        assertThat(nodeOf(tree, "DB 설계")).isNull();
        assertThat(codeOf(tree, "개발")).isEqualTo("2");
        assertThat(codeOf(tree, "공통 모듈 개발")).isEqualTo("2.1");
        assertThat(codeOf(tree, "테스트 및 이행")).isEqualTo("3");
        // overall = (100 + 25 + 0) / 3 = 41.7
        assertThat(tree.get("overallProgress").decimalValue()).isEqualByComparingTo("41.7");

        mvc.perform(putJson(admin, "/api/wbs/" + db, item("삭제됨"))).andExpect(status().isNotFound());
    }

    @Test
    void projectPeriodShrinkConflictsWithWbsLeaf() throws Exception {
        String admin = admin();
        long p2 = projectId("SI-2026-002"); // 계획 03-01 ~ 06-30, 할당 A3 03-16 ~ 04-30
        Map<String, Object> leaf = item("P2 리프");
        leaf.put("plannedStart", "2026-06-01");
        leaf.put("plannedEnd", "2026-06-30");
        mvc.perform(postJson(admin, "/api/projects/" + p2 + "/wbs", leaf)).andExpect(status().isCreated());

        mvc.perform(putJson(admin, "/api/admin/projects/" + p2, Map.of(
                        "code", "SI-2026-002", "name", "물류 MES 고도화", "status", "PLANNED",
                        "plannedStartDate", "2026-03-01", "plannedEndDate", "2026-05-31")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_PERIOD_CONFLICT"))
                .andExpect(jsonPath("$.message").value(containsString("WBS 'P2 리프'")));
    }

    // ===================== 헬퍼 =====================

    private JsonNode tree(long projectId) throws Exception {
        return json(mvc.perform(getAs(admin(), "/api/projects/" + projectId + "/wbs?asOf=" + AS_OF))
                .andExpect(status().isOk()));
    }

    private long itemId(long projectId, String name) throws Exception {
        return nodeOf(tree(projectId), name).get("id").asLong();
    }

    private static JsonNode nodeOf(JsonNode tree, String name) {
        for (JsonNode n : tree.get("items")) {
            if (n.get("name").asText().equals(name)) {
                return n;
            }
        }
        return null;
    }

    private static String codeOf(JsonNode tree, String name) {
        return nodeOf(tree, name).get("wbsCode").asText();
    }

    private static Map<String, Object> item(String name) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        return m;
    }

    /** 응답 노드 → 전체 교체용 PUT 본문 (프론트 그리드와 동일하게 행 전체를 보낸다). */
    private static Map<String, Object> fromNode(JsonNode n) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", n.get("name").asText());
        m.put("deliverable", n.get("deliverable").isNull() ? null : n.get("deliverable").asText());
        m.put("assigneeId", n.get("assignee").isNull() ? null : n.get("assignee").get("id").asLong());
        m.put("status", n.get("status").asText());
        m.put("weight", n.get("weight").decimalValue());
        for (String f : new ArrayList<>(List.of("plannedStart", "plannedEnd", "actualStart", "actualEnd"))) {
            m.put(f, n.get(f).isNull() ? null : n.get(f).asText());
        }
        m.put("progress", n.get("progress").decimalValue().intValue());
        return m;
    }
}
