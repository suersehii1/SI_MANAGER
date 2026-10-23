package com.sipm.dashboard.dto;

import com.sipm.project.ProjectStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardProject(Long id, String code, String name, String clientName, ProjectStatus status,
                               LocalDate plannedStartDate, LocalDate plannedEndDate,
                               LocalDate actualStartDate, LocalDate actualEndDate,
                               long plannedDurationDays, Long actualDurationDays,
                               long assignedHeadcount, long activeHeadcount, long plannedActiveHeadcount,
                               BigDecimal totalPlannedMM, BigDecimal plannedMMToDate, BigDecimal actualMMToDate) {
}
