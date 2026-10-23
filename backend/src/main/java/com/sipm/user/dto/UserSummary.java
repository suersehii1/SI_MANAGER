package com.sipm.user.dto;

import com.sipm.user.User;

public record UserSummary(Long id, String username, String name) {
    public static UserSummary from(User u) {
        return u == null ? null : new UserSummary(u.getId(), u.getUsername(), u.getName());
    }
}
