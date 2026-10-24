package com.sipm.user.dto;

import com.sipm.user.User;

import java.time.OffsetDateTime;
import java.util.List;

/** theme: 화면 스킨 LIGHT(낮) / DARK(밤) — 로그인/내 정보 응답으로 프론트가 바로 적용한다 */
public record UserResponse(Long id, String username, String name, String email, String jobTitle,
                           boolean enabled, List<String> roles, String theme, OffsetDateTime createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getName(), u.getEmail(), u.getJobTitle(),
                u.isEnabled(), u.getRoleNames(), u.getTheme(), u.getCreatedAt());
    }
}
