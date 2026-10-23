package com.sipm.wbs.dto;

import com.sipm.project.dto.ProjectSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** items는 DFS 전위 순서(부모 → 자식들 sort_order 순). */
public record WbsTreeResponse(ProjectSummary project, LocalDate asOf, boolean canEdit, BigDecimal overallProgress,
                              int itemCount, List<WbsItemResponse> items) {
}
