package com.sipm.wbs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 이동 요청: 새 부모(null=루트)의 형제 목록 내 0-based 위치. index > 형제 수면 마지막. */
public record WbsMoveRequest(Long parentId, @NotNull @Min(0) Integer index) {
}
