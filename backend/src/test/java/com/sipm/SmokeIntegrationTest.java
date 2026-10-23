package com.sipm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 엔지니어 스모크 테스트: H2(PostgreSQL 모드)에서 Flyway V1~V4 실행 후 주요 API와 §3.6/W3.1 기준값 확인.
 * 전체 테스트 스위트는 QA 담당.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SmokeIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;

    private String token(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(body).get("accessToken").asText();
    }

    private long projectId(String token, String code) throws Exception {
        String body = mvc.perform(get("/api/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (JsonNode p : om.readTree(body)) {
            if (p.get("code").asText().equals(code)) {
                return p.get("id").asLong();
            }
        }
        throw new IllegalStateException("project not found");
    }

    @Test
    void loginAndRoleChecks() throws Exception {
        String user = token("user", "user1234");
        mvc.perform(get("/api/admin/projects").header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/api/admin/projects")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void planVsActualMatchesWorkedExample() throws Exception {
        String admin = token("admin", "admin1234");
        long p1 = projectId(admin, "SI-2026-001");
        mvc.perform(get("/api/admin/plan-vs-actual").header("Authorization", "Bearer " + admin)
                        .param("projectId", String.valueOf(p1)).param("from", "2026-01").param("to", "2026-04")
                        .param("asOf", "2026-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.series[0].actualMM").value(0.55))
                .andExpect(jsonPath("$.series[2].cumulativeActualMM").value(2.77))
                .andExpect(jsonPath("$.series[2].cumulativeVarianceMM").value(-1.23))
                .andExpect(jsonPath("$.series[3].actualMM").doesNotExist())
                .andExpect(jsonPath("$.summary.plannedMMToDate").value(3.23))
                .andExpect(jsonPath("$.summary.scheduleVarianceMM").value(-0.45))
                .andExpect(jsonPath("$.summary.actualDurationDays").value(60));

        mvc.perform(get("/api/admin/timesheet").header("Authorization", "Bearer " + admin)
                        .param("from", "2026-01").param("to", "2026-04").param("basis", "PLAN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].name").value("김철수"))
                .andExpect(jsonPath("$.rows[0].cells[2].mm").value(1.26))
                .andExpect(jsonPath("$.rows[0].overAllocatedMonths[0]").value("2026-03"))
                .andExpect(jsonPath("$.grandTotal").value(4.76));

        mvc.perform(get("/api/dashboard/projects").header("Authorization", "Bearer " + admin)
                        .param("asOf", "2026-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalPlannedMM").value(4.00))
                .andExpect(jsonPath("$[1].totalPlannedMM").value(0.76))
                .andExpect(jsonPath("$[1].actualDurationDays").doesNotExist());
    }

    @Test
    void assignmentDateRangeValidation() throws Exception {
        String admin = token("admin", "admin1234");
        long p1 = projectId(admin, "SI-2026-001");
        mvc.perform(post("/api/admin/projects/" + p1 + "/assignments").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"plannedStart\":\"2026-03-10\",\"plannedEnd\":\"2026-03-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("plannedEnd"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("DATE_RANGE"));
    }

    @Test
    void wbsTreeMatchesWorkedExample() throws Exception {
        String user = token("user", "user1234");
        long p1 = projectId(user, "SI-2026-001");
        mvc.perform(get("/api/projects/" + p1 + "/wbs").header("Authorization", "Bearer " + user)
                        .param("asOf", "2026-03-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallProgress").value(53.8))
                .andExpect(jsonPath("$.itemCount").value(10))
                .andExpect(jsonPath("$.canEdit").value(false))
                .andExpect(jsonPath("$.items[3].wbsCode").value("2"))
                .andExpect(jsonPath("$.items[3].progress").value(90.0))
                .andExpect(jsonPath("$.items[3].delayed").value(true))
                .andExpect(jsonPath("$.items[5].wbsCode").value("2.2"))
                .andExpect(jsonPath("$.items[5].delayed").value(true));
        // 비멤버 USER 쓰기 → 403
        mvc.perform(post("/api/projects/" + p1 + "/wbs").header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadLimits() throws Exception {
        String admin = token("admin", "admin1234");
        String list = mvc.perform(get("/api/issues").header("Authorization", "Bearer " + admin))
                .andReturn().getResponse().getContentAsString();
        long issueId = om.readTree(list).get("content").get(0).get("id").asLong();

        MockMultipartFile ok = new MockMultipartFile("file", "../../spec.pdf", "application/pdf", new byte[2097152]);
        String body = mvc.perform(multipart("/api/issues/" + issueId + "/attachments").file(ok)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(om.readTree(body).get("originalFilename").asText()).isEqualTo("spec.pdf");

        MockMultipartFile big = new MockMultipartFile("file", "big.pdf", "application/pdf", new byte[2097153]);
        mvc.perform(multipart("/api/issues/" + issueId + "/attachments").file(big)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }
}
