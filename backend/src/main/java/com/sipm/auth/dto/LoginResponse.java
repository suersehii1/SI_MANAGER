package com.sipm.auth.dto;

import com.sipm.user.dto.UserResponse;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
