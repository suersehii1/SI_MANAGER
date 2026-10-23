package com.sipm.user.dto;

import com.sipm.user.User;

import java.time.OffsetDateTime;
import java.util.List;

public record UserResponse(Long id, String username, String name, String email, String jobTitle,
                           boolean enabled, List<String> roles, OffsetDateTime createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getName(), u.getEmail(), u.getJobTitle(),
                u.isEnabled(), u.getRoleNames(), u.getCreatedAt());
    }
}
