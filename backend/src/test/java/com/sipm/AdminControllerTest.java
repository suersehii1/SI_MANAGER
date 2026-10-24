package com.sipm;

import com.fasterxml.jackson.databind.JsonNode;
import com.sipm.admin.AdminPlanActualController;
import com.sipm.admin.AdminTimesheetController;
import com.sipm.assignment.AdminAssignmentController;
import com.sipm.project.AdminProjectController;
import com.sipm.user.AdminUserController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA 체크리스트 1(권한), 3(공기/일정 로직) 검증.
 * 수치 기대값은 specs §3.6 Worked Example(시드 데이터와 동일)을 그대로 옮긴 것이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminControllerTest extends ApiTestSupport {

    // ===================== 1. 보안 및 권한 =====================

    /** specs §4.4 #7~#20: 모든 Admin 엔드포인트 */
    static Stream<Arguments> adminEndpoints() {
        return Stream.of(
                Arguments.of("GET", "/api/admin/projects"),
                Arguments.of("GET", "/api/admin/projects/1"),
                Arguments.of("POST", "/api/admin/projects"),
                Arguments.of("PUT", "/api/admin/projects/1"),
                Arguments.of("DELETE", "/api/admin/projects/1"),
                Arguments.of("GET", "/api/admin/projects/1/assignments"),
                Arguments.of("POST", "/api/admin/projects/1/assignments"),
                Arguments.of("PUT", "/api/admin/projects/1/assignments/1"),
                Arguments.of("DELETE", "/api/admin/projects/1/assignments/1"),
                Arguments.of("GET", "/api/admin/users"),
                Arguments.of("POST", "/api/admin/users"),
                Arguments.of("PUT", "/api/admin/users/1"),
                Arguments.of("GET", "/api/admin/timesheet"),
                Arguments.of("GET", "/api/admin/plan-vs-actual"));
    }

    @ParameterizedTest(name = "USER {0} {1} → 403")
    @MethodSource("adminEndpoints")
    void userTokenIsForbiddenOnEveryAdminEndpoint(String method, String url) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), url).header("Authorization", user())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value(url));
    }

    @ParameterizedTest(name = "토큰 없음 {0} {1} → 401")
    @MethodSource("adminEndpoints")
    void noTokenIsUnauthorized(String method, String url) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), url).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void forgedTokenIsUnauthorized() throws Exception {
        mvc.perform(getAs("Bearer abc.def.ghi", "/api/admin/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void tokenOfDisabledUserIsRejectedOnNextRequest() throws Exception {
        String kim = kim();
        mvc.perform(getAs(kim, "/api/auth/me")).andExpect(status().isOk());
        mvc.perform(putJson(admin(), "/api/admin/users/" + userId("kim"),
                        Map.of("name", "김철수", "roles", List.of("ROLE_USER"), "enabled", false)))
                .andExpect(status().isOk());
        // JWT 필터가 매 요청 DB에서 사용자를 다시 읽으므로 기존 토큰도 즉시 차단
        mvc.perform(getAs(kim, "/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void adminControllersHaveClassLevelPreAuthorize() {
        // 2차 방어선: URL 규칙이 바뀌어도 메서드 보안으로 차단되어야 한다 (specs §8.2)
        for (Class<?> c : List.of(AdminProjectController.class, AdminAssignmentController.class,
                AdminUserController.class, AdminTimesheetController.class, AdminPlanActualController.class)) {
            PreAuthorize pa = c.getAnnotation(PreAuthorize.class);
            assertThat(pa).as(c.getSimpleName()).isNotNull();
            assertThat(pa.value()).isEqualTo("hasRole('ADMIN')");
        }
    }

    // ===================== ADMIN 정상 CRUD =====================

    @Test
    void adminProjectCrudRoundTrip() throws Exception {
        String admin = admin();
        Map<String, Object> body = project("SI-QA-001", "2026-05-01", "2026-12-31");

        JsonNode created = json(mvc.perform(postJson(admin, "/api/admin/projects", body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/admin/projects/")))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.assignedHeadcount").value(0)));
        long id = created.get("id").asLong();

        mvc.perform(getAs(admin, "/api/admin/projects/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SI-QA-001"));

        body.put("name", "QA 수정됨");
        body.put("status", "IN_PROGRESS");
        mvc.perform(putJson(admin, "/api/admin/projects/" + id, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("QA 수정됨"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mvc.perform(getAs(admin, "/api/admin/projects?keyword=QA-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(deleteAs(admin, "/api/admin/projects/" + id)).andExpect(status().isNoContent());
        mvc.perform(getAs(admin, "/api/admin/projects/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void deletingProjectCascadesAssignmentsIssuesAndWbs() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001"); // 할당 2, 이슈 3, WBS 10
        mvc.perform(deleteAs(admin, "/api/admin/projects/" + p1)).andExpect(status().isNoContent());
        mvc.perform(getAs(admin, "/api/issues")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(getAs(admin, "/api/projects/" + p1 + "/wbs")).andExpect(status().isNotFound());
        mvc.perform(getAs(admin, "/api/dashboard/summary"))
                .andExpect(jsonPath("$.totalProjects").value(1))
                .andExpect(jsonPath("$.totalAssignments").value(1));
    }

    @Test
    void duplicateProjectCodeIsConflict() throws Exception {
        mvc.perform(postJson(admin(), "/api/admin/projects", project("SI-2026-001", "2026-05-01", "2026-12-31")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PROJECT_CODE"));
    }

    @Test
    void adminAssignmentCrudRoundTrip() throws Exception {
        String admin = admin();
        long p1 = projectId("SI-2026-001");
        Map<String, Object> body = assignment(userId("user"), "2026-02-01", "2026-03-31");
        body.put("allocationRate", 0.5);

        JsonNode created = json(mvc.perform(postJson(admin, "/api/admin/projects/" + p1 + "/assignments", body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.username").value("user"))
                .andExpect(jsonPath("$.roleInProject").value("DEVELOPER"))
                .andExpect(jsonPath("$.allocationRate").value(0.5)));
        long aid = created.get("id").asLong();

        // 투입/철수일 관리
        body.put("actualStart", "2026-02-03");
        body.put("actualEnd", "2026-03-20");
        mvc.perform(putJson(admin, "/api/admin/projects/" + p1 + "/assignments/" + aid, body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actualStart").value("2026-02-03"))
                .andExpect(jsonPath("$.actualEnd").value("2026-03-20"));

        mvc.perform(getAs(admin, "/api/admin/projects/" + p1 + "/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        // 같은 사용자 중복 할당 → 409
        mvc.perform(postJson(admin, "/api/admin/projects/" + p1 + "/assignments", body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_ASSIGNMENT"));

        // userId 변경 불가
        body.put("userId", userId("kim"));
        mvc.perform(putJson(admin, "/api/admin/projects/" + p1 + "/assignments/" + aid, body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].code").value("IMMUTABLE"));

        mvc.perform(deleteAs(admin, "/api/admin/projects/" + p1 + "/assignments/" + aid))
                .andExpect(status().isNoContent());
        mvc.perform(getAs(admin, "/api/admin/projects/" + p1 + "/assignments"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void assignmentOfOtherProjectIsNotFound() throws Exception {
        String admin = admin();
        long p2 = projectId("SI-2026-002");
        JsonNode p1Assignments = json(mvc.perform(
                getAs(admin, "/api/admin/projects/" + projectId("SI-2026-001") + "/assignments")));
        long p1AssignmentId = p1Assignments.get(0).get("id").asLong();
        mvc.perform(deleteAs(admin, "/api/admin/projects/" + p2 + "/assignments/" + p1AssignmentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminUserCreateUpdateAndRules() throws Exception {
        String admin = admin();
        Map<String, Object> body = new HashMap<>(Map.of("username", "park", "password", "park1234",
                "name", "박민수", "roles", List.of("ROLE_USER")));
        JsonNode created = json(mvc.perform(postJson(admin, "/api/admin/users", body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enabled").value(true)));
        long id = created.get("id").asLong();

        mvc.perform(postJson(admin, "/api/admin/users", body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"));

        // 비활성화된 사용자는 로그인 불가
        mvc.perform(putJson(admin, "/api/admin/users/" + id,
                        Map.of("name", "박민수", "roles", List.of("ROLE_USER"), "enabled", false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"park\",\"password\":\"park1234\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));

        // 허용되지 않은 역할
        body.put("username", "park2");
        body.put("roles", List.of("ROLE_ROOT"));
        mvc.perform(postJson(admin, "/api/admin/users", body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID_ROLE"));
    }

    @Test
    void jobTitleMustBeOneOfAllowedValues() throws Exception {
        String admin = admin();
        // 허용 목록(사원~기타)은 모두 등록 가능
        int i = 0;
        for (String title : List.of("사원", "선임", "책임", "수석", "임원", "대표", "부대표", "기타")) {
            mvc.perform(postJson(admin, "/api/admin/users", Map.of("username", "jt" + (i++), "password", "pass1234",
                            "name", "직급테스트", "jobTitle", title, "roles", List.of("ROLE_USER"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.jobTitle").value(title));
        }
        // 목록 밖의 값은 등록 불가
        mvc.perform(postJson(admin, "/api/admin/users", Map.of("username", "jtx", "password", "pass1234",
                        "name", "직급테스트", "jobTitle", "과장", "roles", List.of("ROLE_USER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("jobTitle"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID_JOB_TITLE"));

        // 기존 사용자(kim, 직급 '과장')는 직급을 그대로 두면 수정 가능, 목록 밖 다른 값으로는 변경 불가
        long kimId = userId("kim");
        mvc.perform(putJson(admin, "/api/admin/users/" + kimId,
                        Map.of("name", "김철수", "jobTitle", "과장", "roles", List.of("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("과장"));
        mvc.perform(putJson(admin, "/api/admin/users/" + kimId,
                        Map.of("name", "김철수", "jobTitle", "부장", "roles", List.of("ROLE_USER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].code").value("INVALID_JOB_TITLE"));
        mvc.perform(putJson(admin, "/api/admin/users/" + kimId,
                        Map.of("name", "김철수", "jobTitle", "책임", "roles", List.of("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobTitle").value("책임"));
    }

    @Test
    void adminCannotDisableOrDemoteSelf() throws Exception {
        String admin = admin();
        long adminId = json(mvc.perform(getAs(admin, "/api/auth/me"))).get("id").asLong();
        mvc.perform(putJson(admin, "/api/admin/users/" + adminId,
                        Map.of("name", "관리자", "roles", List.of("ROLE_USER"), "enabled", true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SELF_MODIFICATION_NOT_ALLOWED"));
    }

    // ===================== 3. 공기 및 일정 로직: 날짜 순서 =====================

    @Test
    void projectEndBeforeStartIsRejected() throws Exception {
        mvc.perform(postJson(admin(), "/api/admin/projects", project("SI-QA-002", "2026-05-01", "2026-04-30")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("plannedEndDate"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("DATE_RANGE"));
    }

    @Test
    void projectSameDayPeriodIsAllowed() throws Exception {
        mvc.perform(postJson(admin(), "/api/admin/projects", project("SI-QA-003", "2026-05-01", "2026-05-01")))
                .andExpect(status().isCreated());
    }

    @Test
    void projectActualEndWithoutStartOrBeforeStartIsRejected() throws Exception {
        Map<String, Object> body = project("SI-QA-004", "2026-05-01", "2026-12-31");
        body.put("actualEndDate", "2026-06-01");
        mvc.perform(postJson(admin(), "/api/admin/projects", body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("actualStartDate"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("REQUIRED_WITH_END"));

        body.put("actualStartDate", "2026-06-10");
        mvc.perform(postJson(admin(), "/api/admin/projects", body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("actualEndDate"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("DATE_RANGE"));
    }

    @Test
    void assignmentPlannedEndBeforeStartIsRejected() throws Exception {
        long p1 = projectId("SI-2026-001");
        // specs §5.1 QA 기대값 그대로
        mvc.perform(postJson(admin(), "/api/admin/projects/" + p1 + "/assignments",
                        assignment(userId("user"), "2026-03-10", "2026-03-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("plannedEnd"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("DATE_RANGE"));
    }

    @Test
    void assignmentReleaseBeforeDeployIsRejected() throws Exception {
        long p1 = projectId("SI-2026-001");
        Map<String, Object> body = assignment(userId("user"), "2026-02-01", "2026-03-31");
        body.put("actualStart", "2026-03-10");
        body.put("actualEnd", "2026-03-01");
        mvc.perform(postJson(admin(), "/api/admin/projects/" + p1 + "/assignments", body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("actualEnd"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("DATE_RANGE"));

        body.remove("actualStart");
        mvc.perform(postJson(admin(), "/api/admin/projects/" + p1 + "/assignments", body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("actualStart"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("REQUIRED_WITH_END"));
    }

    @Test
    void assignmentOutsideProjectPeriodIsRejected() throws Exception {
        long p1 = projectId("SI-2026-001"); // 계획 2026-01-01 ~ 2026-03-31
        mvc.perform(postJson(admin(), "/api/admin/projects/" + p1 + "/assignments",
                        assignment(userId("user"), "2025-12-31", "2026-04-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASSIGNMENT_OUT_OF_PROJECT_PERIOD"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("plannedStart"))
                .andExpect(jsonPath("$.fieldErrors[1].field").value("plannedEnd"));
    }

    @Test
    void assignmentOnProjectBoundariesIsAllowed() throws Exception {
        long p1 = projectId("SI-2026-001");
        mvc.perform(postJson(admin(), "/api/admin/projects/" + p1 + "/assignments",
                        assignment(userId("user"), "2026-01-01", "2026-03-31")))
                .andExpect(status().isCreated());
    }

    @Test
    void shrinkingProjectPeriodBelowAssignmentsIsConflict() throws Exception {
        long p1 = projectId("SI-2026-001");
        mvc.perform(putJson(admin(), "/api/admin/projects/" + p1, Map.of(
                        "code", "SI-2026-001", "name", "차세대 ERP 구축", "status", "IN_PROGRESS",
                        "plannedStartDate", "2026-01-01", "plannedEndDate", "2026-03-20",
                        "actualStartDate", "2026-01-15")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROJECT_PERIOD_CONFLICT"))
                .andExpect(jsonPath("$.message").value(containsString("김철수")));
    }

    @Test
    void invalidMonthRangeIsRejected() throws Exception {
        String admin = admin();
        mvc.perform(getAs(admin, "/api/admin/timesheet?from=2026-05&to=2026-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("to"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("DATE_RANGE"));
        mvc.perform(getAs(admin, "/api/admin/plan-vs-actual?from=2020-01&to=2026-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].code").value("RANGE_TOO_LARGE"));
        mvc.perform(getAs(admin, "/api/admin/timesheet?asOf=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].code").value("TYPE_MISMATCH"));
    }

    // ===================== 3. 계획 공기 vs 실제 투입 공기 정합성 (§3.6) =====================

    @Test
    void planVsActualForProjectMatchesWorkedExample() throws Exception {
        long p1 = projectId("SI-2026-001");
        mvc.perform(getAs(admin(), "/api/admin/plan-vs-actual?projectId=" + p1
                        + "&from=2026-01&to=2026-04&asOf=2026-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectCode").value("SI-2026-001"))
                .andExpect(jsonPath("$.series.length()").value(4))
                // 2026-01
                .andExpect(jsonPath("$.series[0].month").value("2026-01"))
                .andExpect(jsonPath("$.series[0].plannedMM").value(1.00))
                .andExpect(jsonPath("$.series[0].actualMM").value(0.55))
                .andExpect(jsonPath("$.series[0].cumulativePlannedMM").value(1.00))
                .andExpect(jsonPath("$.series[0].cumulativeActualMM").value(0.55))
                .andExpect(jsonPath("$.series[0].varianceMM").value(-0.45))
                .andExpect(jsonPath("$.series[0].cumulativeVarianceMM").value(-0.45))
                // 2026-02
                .andExpect(jsonPath("$.series[1].plannedMM").value(1.50))
                .andExpect(jsonPath("$.series[1].actualMM").value(1.50))
                .andExpect(jsonPath("$.series[1].cumulativePlannedMM").value(2.50))
                .andExpect(jsonPath("$.series[1].cumulativeActualMM").value(2.05))
                .andExpect(jsonPath("$.series[1].varianceMM").value(0.00))
                .andExpect(jsonPath("$.series[1].cumulativeVarianceMM").value(-0.45))
                // 2026-03: 표시값 합(0.55+1.50+0.73=2.78)이 아니라 반올림 전 합산 2.77이 정답
                .andExpect(jsonPath("$.series[2].plannedMM").value(1.50))
                .andExpect(jsonPath("$.series[2].actualMM").value(0.73))
                .andExpect(jsonPath("$.series[2].cumulativePlannedMM").value(4.00))
                .andExpect(jsonPath("$.series[2].cumulativeActualMM").value(2.77))
                .andExpect(jsonPath("$.series[2].varianceMM").value(-0.77))
                .andExpect(jsonPath("$.series[2].cumulativeVarianceMM").value(-1.23))
                // 2026-04: 미래 월은 실제값 null
                .andExpect(jsonPath("$.series[3].plannedMM").value(0.00))
                .andExpect(jsonPath("$.series[3].actualMM").value(nullValue()))
                .andExpect(jsonPath("$.series[3].cumulativePlannedMM").value(4.00))
                .andExpect(jsonPath("$.series[3].cumulativeActualMM").value(nullValue()))
                .andExpect(jsonPath("$.series[3].varianceMM").value(nullValue()))
                .andExpect(jsonPath("$.series[3].cumulativeVarianceMM").value(nullValue()))
                // summary
                .andExpect(jsonPath("$.summary.totalPlannedMM").value(4.00))
                .andExpect(jsonPath("$.summary.plannedMMToDate").value(3.23))
                .andExpect(jsonPath("$.summary.actualMMToDate").value(2.77))
                .andExpect(jsonPath("$.summary.scheduleVarianceMM").value(-0.45))
                .andExpect(jsonPath("$.summary.plannedDurationDays").value(90))
                .andExpect(jsonPath("$.summary.actualDurationDays").value(60));
    }

    @Test
    void planVsActualForAllProjectsMatchesWorkedExample() throws Exception {
        mvc.perform(getAs(admin(), "/api/admin/plan-vs-actual?from=2026-01&to=2026-04&asOf=2026-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(nullValue()))
                .andExpect(jsonPath("$.series[0].plannedMM").value(1.00))
                .andExpect(jsonPath("$.series[1].plannedMM").value(1.50))
                .andExpect(jsonPath("$.series[2].plannedMM").value(1.76))
                .andExpect(jsonPath("$.series[3].plannedMM").value(0.50))
                .andExpect(jsonPath("$.series[2].actualMM").value(0.73))
                .andExpect(jsonPath("$.series[3].actualMM").value(nullValue()))
                .andExpect(jsonPath("$.series[2].cumulativePlannedMM").value(4.26))
                .andExpect(jsonPath("$.series[3].cumulativePlannedMM").value(4.76))
                .andExpect(jsonPath("$.summary.totalPlannedMM").value(4.76))
                .andExpect(jsonPath("$.summary.plannedMMToDate").value(3.23))
                .andExpect(jsonPath("$.summary.actualMMToDate").value(2.77))
                .andExpect(jsonPath("$.summary.plannedDurationDays").value(nullValue()))
                .andExpect(jsonPath("$.summary.actualDurationDays").value(nullValue()));
    }

    @Test
    void timesheetPlanMatchesWorkedExample() throws Exception {
        mvc.perform(getAs(admin(), "/api/admin/timesheet?from=2026-01&to=2026-04&basis=PLAN&asOf=2026-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months.length()").value(4))
                .andExpect(jsonPath("$.rows.length()").value(2))
                // 김철수: A1(SI-2026-001) + A3(SI-2026-002)
                .andExpect(jsonPath("$.rows[0].name").value("김철수"))
                .andExpect(jsonPath("$.rows[0].cells[0].mm").value(1.00))
                .andExpect(jsonPath("$.rows[0].cells[1].mm").value(1.00))
                .andExpect(jsonPath("$.rows[0].cells[2].mm").value(1.26))
                .andExpect(jsonPath("$.rows[0].cells[3].mm").value(0.50))
                .andExpect(jsonPath("$.rows[0].total").value(3.76))
                .andExpect(jsonPath("$.rows[0].overAllocatedMonths.length()").value(1))
                .andExpect(jsonPath("$.rows[0].overAllocatedMonths[0]").value("2026-03"))
                .andExpect(jsonPath("$.rows[0].projects[0].projectCode").value("SI-2026-001"))
                .andExpect(jsonPath("$.rows[0].projects[0].total").value(3.00))
                .andExpect(jsonPath("$.rows[0].projects[1].projectCode").value("SI-2026-002"))
                .andExpect(jsonPath("$.rows[0].projects[1].cells[2].mm").value(0.26))
                .andExpect(jsonPath("$.rows[0].projects[1].cells[3].mm").value(0.50))
                .andExpect(jsonPath("$.rows[0].projects[1].total").value(0.76))
                // 이영희: A2
                .andExpect(jsonPath("$.rows[1].name").value("이영희"))
                .andExpect(jsonPath("$.rows[1].cells[0].mm").value(0.00))
                .andExpect(jsonPath("$.rows[1].cells[1].mm").value(0.50))
                .andExpect(jsonPath("$.rows[1].cells[2].mm").value(0.50))
                .andExpect(jsonPath("$.rows[1].total").value(1.00))
                .andExpect(jsonPath("$.rows[1].overAllocatedMonths.length()").value(0))
                // 월 총합
                .andExpect(jsonPath("$.monthTotals[0].mm").value(1.00))
                .andExpect(jsonPath("$.monthTotals[1].mm").value(1.50))
                .andExpect(jsonPath("$.monthTotals[2].mm").value(1.76))
                .andExpect(jsonPath("$.monthTotals[3].mm").value(0.50))
                .andExpect(jsonPath("$.grandTotal").value(4.76));
    }

    @Test
    void timesheetActualMatchesWorkedExample() throws Exception {
        mvc.perform(getAs(admin(), "/api/admin/timesheet?from=2026-01&to=2026-04&basis=ACTUAL&asOf=2026-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows.length()").value(2))
                // A3는 actual_start가 null → 제외
                .andExpect(jsonPath("$.rows[0].name").value("김철수"))
                .andExpect(jsonPath("$.rows[0].projects.length()").value(1))
                .andExpect(jsonPath("$.rows[0].cells[0].mm").value(0.55))
                .andExpect(jsonPath("$.rows[0].cells[1].mm").value(1.00))
                .andExpect(jsonPath("$.rows[0].cells[2].mm").value(0.48))
                .andExpect(jsonPath("$.rows[0].cells[3].mm").value(0.00))
                .andExpect(jsonPath("$.rows[0].total").value(2.03))
                .andExpect(jsonPath("$.rows[0].projects[0].end").value("2026-03-15"))
                .andExpect(jsonPath("$.rows[1].cells[1].mm").value(0.50))
                .andExpect(jsonPath("$.rows[1].cells[2].mm").value(0.24))
                .andExpect(jsonPath("$.rows[1].total").value(0.74))
                .andExpect(jsonPath("$.monthTotals[0].mm").value(0.55))
                .andExpect(jsonPath("$.monthTotals[1].mm").value(1.50))
                .andExpect(jsonPath("$.monthTotals[2].mm").value(0.73))
                .andExpect(jsonPath("$.monthTotals[3].mm").value(0.00))
                .andExpect(jsonPath("$.grandTotal").value(2.77));
    }

    @Test
    void dashboardMatchesWorkedExample() throws Exception {
        String user = user(); // 대시보드는 모든 인증 사용자
        mvc.perform(getAs(user, "/api/dashboard/projects?asOf=2026-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SI-2026-001"))
                .andExpect(jsonPath("$[0].assignedHeadcount").value(2))
                .andExpect(jsonPath("$[0].activeHeadcount").value(2))
                .andExpect(jsonPath("$[0].plannedActiveHeadcount").value(2))
                .andExpect(jsonPath("$[0].totalPlannedMM").value(4.00))
                .andExpect(jsonPath("$[0].plannedMMToDate").value(3.23))
                .andExpect(jsonPath("$[0].actualMMToDate").value(2.77))
                .andExpect(jsonPath("$[0].plannedDurationDays").value(90))
                .andExpect(jsonPath("$[0].actualDurationDays").value(60))
                .andExpect(jsonPath("$[1].code").value("SI-2026-002"))
                .andExpect(jsonPath("$[1].assignedHeadcount").value(1))
                .andExpect(jsonPath("$[1].activeHeadcount").value(0))
                .andExpect(jsonPath("$[1].plannedActiveHeadcount").value(0))
                .andExpect(jsonPath("$[1].totalPlannedMM").value(0.76))
                .andExpect(jsonPath("$[1].actualMMToDate").value(0.00))
                .andExpect(jsonPath("$[1].plannedDurationDays").value(122))
                .andExpect(jsonPath("$[1].actualDurationDays").value(nullValue()));

        mvc.perform(getAs(user, "/api/dashboard/summary?asOf=2026-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(2))
                .andExpect(jsonPath("$.activePersonnel").value(2))
                .andExpect(jsonPath("$.totalAssignments").value(3))
                .andExpect(jsonPath("$.openIssues").value(2))
                .andExpect(jsonPath("$.projectsByStatus.ON_HOLD").value(0));

        // 보조 케이스: asOf=2026-04-01 → A1 철수(03-31) 후
        mvc.perform(getAs(user, "/api/dashboard/projects?asOf=2026-04-01"))
                .andExpect(jsonPath("$[0].activeHeadcount").value(1))
                .andExpect(jsonPath("$[0].plannedActiveHeadcount").value(0));
    }

    // ===================== 헬퍼 =====================

    private static Map<String, Object> project(String code, String start, String end) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", code);
        m.put("name", "QA 프로젝트");
        m.put("plannedStartDate", start);
        m.put("plannedEndDate", end);
        return m;
    }

    private static Map<String, Object> assignment(long userId, String plannedStart, String plannedEnd) {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", userId);
        m.put("plannedStart", plannedStart);
        m.put("plannedEnd", plannedEnd);
        return m;
    }
}
