package com.sipm.project.dto;

import com.sipm.project.Project;
import com.sipm.project.ProjectStatus;

import java.time.LocalDate;

public record ProjectSummary(Long id, String code, String name, ProjectStatus status,
                             LocalDate plannedStartDate, LocalDate plannedEndDate) {
    public static ProjectSummary from(Project p) {
        return new ProjectSummary(p.getId(), p.getCode(), p.getName(), p.getStatus(),
                p.getPlannedStartDate(), p.getPlannedEndDate());
    }
}
