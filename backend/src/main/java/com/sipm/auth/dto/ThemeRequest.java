package com.sipm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 내 화면 스킨 변경 요청. LIGHT = 낮(해), DARK = 밤(달) */
public record ThemeRequest(
        @NotBlank @Pattern(regexp = "^(LIGHT|DARK)$", message = "스킨은 LIGHT 또는 DARK 여야 합니다.") String theme) {
}
