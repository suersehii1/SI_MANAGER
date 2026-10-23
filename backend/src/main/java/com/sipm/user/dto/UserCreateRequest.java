package com.sipm.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserCreateRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "아이디는 영문/숫자/._- 3~50자입니다.") String username,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(max = 100) String name,
        @Email @Size(max = 200) String email,
        @Size(max = 50) String jobTitle,
        @NotEmpty List<String> roles,
        Boolean enabled) {
}
