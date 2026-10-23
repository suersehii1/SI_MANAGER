package com.sipm.assignment.dto;

import com.sipm.assignment.ProjectAssignment;
import com.sipm.user.dto.UserSummary;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssignmentResponse(Long id, Long projectId, UserSummary user, String roleInProject,
                                 BigDecimal allocationRate, LocalDate plannedStart, LocalDate plannedEnd,
                                 LocalDate actualStart, LocalDate actualEnd, String state, String note) {

    public static AssignmentResponse from(ProjectAssignment a, LocalDate today) {
        return new AssignmentResponse(a.getId(), a.getProject().getId(), UserSummary.from(a.getUser()),
                a.getRoleInProject(), a.getAllocationRate(), a.getPlannedStart(), a.getPlannedEnd(),
                a.getActualStart(), a.getActualEnd(), state(a.getActualStart(), a.getActualEnd(), today), a.getNote());
    }

    /** 할당 상태(오늘 기준 파생값): 미투입/투입 예정 → PLANNED, 철수일 지남 → RELEASED, 그 외 ACTIVE. */
    public static String state(LocalDate actualStart, LocalDate actualEnd, LocalDate today) {
        if (actualStart == null || actualStart.isAfter(today)) {
            return "PLANNED";
        }
        if (actualEnd != null && actualEnd.isBefore(today)) {
            return "RELEASED";
        }
        return "ACTIVE";
    }
}
