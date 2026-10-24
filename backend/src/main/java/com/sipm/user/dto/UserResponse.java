package com.sipm.user.dto;

import com.sipm.user.User;

import java.time.OffsetDateTime;
import java.util.List;

/** theme: 화면 스킨 LIGHT(낮) / DARK(밤) — 로그인/내 정보 응답으로 프론트가 바로 적용한다 */
public record UserResponse(Long id, String username, String name, String englishName, String email, String phone,
                           String jobTitle, boolean enabled, List<String> roles, String theme, OffsetDateTime createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getName(), u.getEnglishName(), u.getEmail(), u.getPhone(),
                u.getJobTitle(), u.isEnabled(), u.getRoleNames(), u.getTheme(), u.getCreatedAt());
    }

    /** 아이디(username)를 뺀 복사본 — ADMIN이 아닌 사용자에게 사용자 목록을 보여줄 때 사용 */
    public UserResponse withoutUsername() {
        return new UserResponse(id, null, name, englishName, email, phone, jobTitle, enabled, roles, theme, createdAt);
    }
}
