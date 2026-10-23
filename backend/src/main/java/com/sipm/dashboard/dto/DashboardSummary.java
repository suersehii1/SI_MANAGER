package com.sipm.dashboard.dto;

import java.time.LocalDate;
import java.util.Map;

public record DashboardSummary(LocalDate asOf, long totalProjects, Map<String, Long> projectsByStatus,
                               long activePersonnel, long totalAssignments, long openIssues) {
}
