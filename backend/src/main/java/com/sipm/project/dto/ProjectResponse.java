package com.sipm.project.dto;

import com.sipm.project.Project;
import com.sipm.project.ProjectStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ProjectResponse(Long id, String code, String name, String clientName, String description,
                              ProjectStatus status, LocalDate plannedStartDate, LocalDate plannedEndDate,
                              LocalDate actualStartDate, LocalDate actualEndDate, long assignedHeadcount,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    public static ProjectResponse from(Project p, long headcount) {
        return new ProjectResponse(p.getId(), p.getCode(), p.getName(), p.getClientName(), p.getDescription(),
                p.getStatus(), p.getPlannedStartDate(), p.getPlannedEndDate(), p.getActualStartDate(),
                p.getActualEndDate(), headcount, p.getCreatedAt(), p.getUpdatedAt());
    }
}
