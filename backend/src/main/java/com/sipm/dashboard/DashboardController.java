package com.sipm.dashboard;

import com.sipm.common.util.Times;
import com.sipm.dashboard.dto.DashboardProject;
import com.sipm.dashboard.dto.DashboardSummary;
import com.sipm.project.ProjectStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummary summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return dashboardService.summary(Times.orToday(asOf));
    }

    @GetMapping("/projects")
    public List<DashboardProject> projects(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @RequestParam(required = false) ProjectStatus status) {
        return dashboardService.projects(Times.orToday(asOf), status);
    }
}
