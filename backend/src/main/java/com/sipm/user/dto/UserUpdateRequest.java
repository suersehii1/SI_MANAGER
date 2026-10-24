package com.sipm.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** password: null or empty = unchanged; otherwise 8..64 (checked in service so empty string is allowed). */
public record UserUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) @Pattern(regexp = UserFieldRules.ENGLISH_NAME, message = UserFieldRules.ENGLISH_NAME_MESSAGE) String englishName,
        @Email @Size(max = 200) String email,
        @Size(max = 20) @Pattern(regexp = UserFieldRules.PHONE, message = UserFieldRules.PHONE_MESSAGE) String phone,
        @Size(max = 50) String jobTitle,
        @NotEmpty List<String> roles,
        Boolean enabled,
        @Size(max = 64) String password) {
}
