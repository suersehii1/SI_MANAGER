package com.sipm.wbs.dto;

import com.sipm.user.dto.UserSummary;
import com.sipm.wbs.WbsStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/** rolledUp=true면 기간/진척률/상태는 하위 노드에서 집계된 읽기 전용 값. */
public record WbsItemResponse(Long id, Long projectId, Long parentId, String wbsCode, int level, int sortOrder,
                              String name, String description, String deliverable, UserSummary assignee,
                              WbsStatus status, BigDecimal weight,
                              LocalDate plannedStart, LocalDate plannedEnd, LocalDate actualStart, LocalDate actualEnd,
                              BigDecimal progress, Long plannedDays, boolean delayed,
                              boolean hasChildren, int childCount, boolean rolledUp, OffsetDateTime updatedAt) {
}
