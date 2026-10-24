package com.sipm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 사용자별 화면 스킨(낮/밤) 저장: PUT /api/auth/me/theme
 * - 기본값 LIGHT, DARK로 바꾸면 다음 로그인/내 정보 조회에도 유지
 * - 본인 것만 변경 (다른 사용자에게 영향 없음), LIGHT/DARK 외 값은 400, 미로그인 401
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ThemeTest extends ApiTestSupport {

    @Test
    void themeDefaultsToLightAndPersistsPerUser() throws Exception {
        String kim = kim();
        mvc.perform(getAs(kim, "/api/auth/me")).andExpect(status().isOk()).andExpect(jsonPath("$.theme").value("LIGHT"));

        mvc.perform(putJson(kim, "/api/auth/me/theme", Map.of("theme", "DARK")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("DARK"));

        // 다시 로그인해도 DARK 유지 (로그인 응답의 user.theme)
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"kim\",\"password\":\"user1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.theme").value("DARK"));

        // 다른 사용자(user)는 영향 없음
        mvc.perform(getAs(user(), "/api/auth/me")).andExpect(jsonPath("$.theme").value("LIGHT"));

        // 다시 LIGHT로
        mvc.perform(putJson(kim, "/api/auth/me/theme", Map.of("theme", "LIGHT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("LIGHT"));
    }

    @Test
    void invalidThemeIsRejectedAndLoginRequired() throws Exception {
        mvc.perform(putJson(kim(), "/api/auth/me/theme", Map.of("theme", "BLUE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("theme"));
        // Authorization 헤더 없이 호출 → 401
        mvc.perform(put("/api/auth/me/theme").contentType(MediaType.APPLICATION_JSON).content("{\"theme\":\"DARK\"}"))
                .andExpect(status().isUnauthorized());
    }
}
