package com.sipm.dashboard;

import com.sipm.assignment.AssignmentRepository;
import com.sipm.assignment.ProjectAssignment;
import com.sipm.dashboard.dto.DashboardProject;
import com.sipm.dashboard.dto.DashboardSummary;
import com.sipm.issue.IssueRepository;
import com.sipm.issue.IssueStatus;
import com.sipm.metrics.ManMonthCalculator;
import com.sipm.project.Project;
import com.sipm.project.ProjectRepository;
import com.sipm.project.ProjectStatus;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 대시보드 집계 (specs §3.3, §4.6). */
@Service
public class DashboardService {
    private final ProjectRepository projectRepository;
    private final AssignmentRepository assignmentRepository;
    private final IssueRepository issueRepository;

    public DashboardService(ProjectRepository projectRepository, AssignmentRepository assignmentRepository,
                            IssueRepository issueRepository) {
        this.projectRepository = projectRepository;
        this.assignmentRepository = assignmentRepository;
        this.issueRepository = issueRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(LocalDate asOf) {
        // 4개 상태 키는 0이어도 항상 포함
        Map<String, Long> byStatus = new LinkedHashMap<>();
        long total = 0;
        for (ProjectStatus s : ProjectStatus.values()) {
            long c = projectRepository.countByStatus(s);
            byStatus.put(s.name(), c);
            total += c;
        }

        // activePersonnel: asOf 기준 투입 중인 할당의 distinct user 수 (여러 프로젝트 동시 투입도 1명)
        List<ProjectAssignment> all = assignmentRepository.findAllFetch();
        Set<Long> activeUsers = new HashSet<>();
        for (ProjectAssignment a : all) {
            if (ProjectMetrics.isActive(a, asOf)) {
                activeUsers.add(a.getUser().getId());
            }
        }
        long openIssues = issueRepository.countByStatusIn(List.of(IssueStatus.OPEN, IssueStatus.IN_PROGRESS));
        return new DashboardSummary(asOf, total, byStatus, activeUsers.size(), all.size(), openIssues);
    }

    @Transactional(readOnly = true)
    public List<DashboardProject> projects(LocalDate asOf, ProjectStatus status) {
        List<Project> projects = projectRepository.findAll(ProjectRepository.status(status),
                Sort.by("plannedStartDate").ascending().and(Sort.by("code").ascending()));

        // 전체 할당을 한 번에 읽어 프로젝트별로 묶는다
        Map<Long, List<ProjectAssignment>> byProject = new HashMap<>();
        for (ProjectAssignment a : assignmentRepository.findAllFetch()) {
            byProject.computeIfAbsent(a.getProject().getId(), k -> new ArrayList<>()).add(a);
        }

        List<DashboardProject> result = new ArrayList<>();
        for (Project p : projects) {
            List<ProjectAssignment> list = byProject.getOrDefault(p.getId(), List.of());
            long active = 0;
            long plannedActive = 0;
            for (ProjectAssignment a : list) {
                if (ProjectMetrics.isActive(a, asOf)) {
                    active++;
                }
                if (ProjectMetrics.isPlannedActive(a, asOf)) {
                    plannedActive++;
                }
            }
            result.add(new DashboardProject(p.getId(), p.getCode(), p.getName(), p.getClientName(), p.getStatus(),
                    p.getPlannedStartDate(), p.getPlannedEndDate(), p.getActualStartDate(), p.getActualEndDate(),
                    ProjectMetrics.plannedDurationDays(p), ProjectMetrics.actualDurationDays(p, asOf),
                    list.size(), active, plannedActive,
                    ManMonthCalculator.round2(ProjectMetrics.totalPlannedMM(list, asOf)),
                    ManMonthCalculator.round2(ProjectMetrics.plannedMMToDate(list, asOf)),
                    ManMonthCalculator.round2(ProjectMetrics.actualMMToDate(list, asOf))));
        }
        return result;
    }
}
