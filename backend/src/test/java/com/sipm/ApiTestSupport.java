package com.sipm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 통합 테스트 공통 헬퍼: 시드 계정 로그인, 코드/아이디로 ID 조회, JSON 요청 빌더.
 * 시드 ID는 identity로 생성되므로 하드코딩하지 않고 API로 찾는다 (specs §10.2).
 */
public abstract class ApiTestSupport {
    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper om;

    protected String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + om.readTree(body).get("accessToken").asText();
    }

    protected String admin() throws Exception {
        return login("admin", "admin1234");
    }

    protected String user() throws Exception {
        return login("user", "user1234");
    }

    protected String kim() throws Exception {
        return login("kim", "user1234");
    }

    protected long projectId(String code) throws Exception {
        for (JsonNode p : json(mvc.perform(get("/api/projects").header("Authorization", admin())))) {
            if (p.get("code").asText().equals(code)) {
                return p.get("id").asLong();
            }
        }
        throw new IllegalStateException("project not found: " + code);
    }

    protected long userId(String username) throws Exception {
        for (JsonNode u : json(mvc.perform(get("/api/users").header("Authorization", admin())))) {
            if (u.get("username").asText().equals(username)) {
                return u.get("id").asLong();
            }
        }
        throw new IllegalStateException("user not found: " + username);
    }

    protected JsonNode json(ResultActions result) throws Exception {
        // MockMvc 기본 문자셋은 ISO-8859-1이므로 한글 응답은 UTF-8로 명시해서 읽는다
        return om.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    protected MockHttpServletRequestBuilder getAs(String token, String url) {
        return get(url).header("Authorization", token);
    }

    protected MockHttpServletRequestBuilder postJson(String token, String url, Object body) throws Exception {
        return post(url).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(toJson(body));
    }

    protected MockHttpServletRequestBuilder putJson(String token, String url, Object body) throws Exception {
        return put(url).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(toJson(body));
    }

    protected MockHttpServletRequestBuilder deleteAs(String token, String url) {
        return delete(url).header("Authorization", token);
    }

    private String toJson(Object body) throws Exception {
        return body instanceof String s ? s : om.writeValueAsString(body);
    }
}
