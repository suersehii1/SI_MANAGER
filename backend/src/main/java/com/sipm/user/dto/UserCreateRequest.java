package com.sipm.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 사용자 등록. 영어 이름·전화번호는 선택 (빈 값 허용, 입력하면 형식 검사) */
public record UserCreateRequest(
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "아이디는 영문/숫자/._- 3~50자입니다.") String username,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) @Pattern(regexp = UserFieldRules.ENGLISH_NAME, message = UserFieldRules.ENGLISH_NAME_MESSAGE) String englishName,
        @Email @Size(max = 200) String email,
        @Size(max = 20) @Pattern(regexp = UserFieldRules.PHONE, message = UserFieldRules.PHONE_MESSAGE) String phone,
        @Size(max = 50) String jobTitle,
        @NotEmpty List<String> roles,
        Boolean enabled) {
}
