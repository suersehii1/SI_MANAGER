package com.sipm.project.dto;

import com.sipm.project.Project;
import com.sipm.project.ProjectStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** version: 낙관적 잠금 버전 — 수정 화면이 이 값을 수정 요청에 다시 보낸다 */
public record ProjectResponse(Long id, String code, String name, String clientName, String description,
                              ProjectStatus status, LocalDate plannedStartDate, LocalDate plannedEndDate,
                              LocalDate actualStartDate, LocalDate actualEndDate, long assignedHeadcount,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt, Long version) {
    public static ProjectResponse from(Project p, long headcount) {
        return new ProjectResponse(p.getId(), p.getCode(), p.getName(), p.getClientName(), p.getDescription(),
                p.getStatus(), p.getPlannedStartDate(), p.getPlannedEndDate(), p.getActualStartDate(),
                p.getActualEndDate(), headcount, p.getCreatedAt(), p.getUpdatedAt(), p.getVersion());
    }
}
