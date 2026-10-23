package com.sipm.admin;

import com.sipm.admin.dto.PlanActualResponse;
import com.sipm.assignment.AssignmentRepository;
import com.sipm.assignment.ProjectAssignment;
import com.sipm.dashboard.ProjectMetrics;
import com.sipm.metrics.Basis;
import com.sipm.metrics.DateRanges;
import com.sipm.metrics.ManMonthCalculator;
import com.sipm.project.Project;
import com.sipm.project.ProjectService;
import com.sipm.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 계획 vs 실제 시계열 (specs §3.5).
 * - plannedMM(m): PLAN 기준 월 MM 합 / actualMM(m): ACTUAL 기준 월 MM 합 (monthStart > asOf인 미래 월은 null)
 * - 누계는 from 월에서 0부터 시작, 반올림 전 값으로 누적한다.
 * - summary는 from/to 범위와 무관하게 전 기간 기준.
 */
@Service
public class PlanActualService {
    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final AssignmentRepository assignmentRepository;

    public PlanActualService(ProjectService projectService, ProjectRepository projectRepository,
                             AssignmentRepository assignmentRepository) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public PlanActualResponse planVsActual(Long projectId, YearMonth from, YearMonth to, LocalDate asOf) {
        Project single = projectId != null ? projectService.get(projectId) : null;
        List<Project> projects = single != null ? List.of(single) : projectRepository.findAll();
        List<Long> ids = new ArrayList<>();
        for (Project p : projects) {
            ids.add(p.getId());
        }
        List<ProjectAssignment> assignments = ids.isEmpty() ? List.of() : assignmentRepository.findByProjectIdsFetch(ids);

        // 기본 범위: 프로젝트/할당의 가장 이른 시작 ~ 가장 늦은 종료(ACTUAL 유효 종료 포함)
        if (from == null) {
            LocalDate min = defaultStart(projects, assignments);
            from = min == null ? null : YearMonth.from(min);
        }
        if (to == null) {
            LocalDate max = defaultEnd(projects, assignments, asOf);
            to = max == null ? null : YearMonth.from(max);
        }

        List<PlanActualResponse.Point> series = new ArrayList<>();
        if (from != null && to != null) {
            MonthRangeValidator.validate(from, to);
            series = buildSeries(assignments, from, to, asOf);
        }

        // summary (전 기간)
        BigDecimal totalPlanned = ProjectMetrics.totalPlannedMM(assignments, asOf);
        BigDecimal plannedToDate = ProjectMetrics.plannedMMToDate(assignments, asOf);
        BigDecimal actualToDate = ProjectMetrics.actualMMToDate(assignments, asOf);
        Long plannedDays = single != null ? ProjectMetrics.plannedDurationDays(single) : null;
        Long actualDays = single != null ? ProjectMetrics.actualDurationDays(single, asOf) : null;
        PlanActualResponse.Summary summary = new PlanActualResponse.Summary(
                ManMonthCalculator.round2(totalPlanned),
                ManMonthCalculator.round2(plannedToDate),
                ManMonthCalculator.round2(actualToDate),
                ManMonthCalculator.round2(actualToDate.subtract(plannedToDate)),
                plannedDays, actualDays);

        return new PlanActualResponse(
                single != null ? single.getId() : null,
                single != null ? single.getCode() : null,
                single != null ? single.getName() : null,
                from, to, asOf, series, summary);
    }

    private List<PlanActualResponse.Point> buildSeries(List<ProjectAssignment> assignments, YearMonth from,
                                                       YearMonth to, LocalDate asOf) {
        List<PlanActualResponse.Point> series = new ArrayList<>();
        BigDecimal cumPlanned = ManMonthCalculator.zero();
        BigDecimal cumActual = ManMonthCalculator.zero();
        for (YearMonth m : DateRanges.months(from, to)) {
            BigDecimal planned = ManMonthCalculator.zero();
            BigDecimal actual = ManMonthCalculator.zero();
            for (ProjectAssignment a : assignments) {
                DateRanges.Span planSpan = ManMonthCalculator.effectiveSpan(Basis.PLAN, a.getPlannedStart(),
                        a.getPlannedEnd(), a.getActualStart(), a.getActualEnd(), asOf);
                DateRanges.Span actualSpan = ManMonthCalculator.effectiveSpan(Basis.ACTUAL, a.getPlannedStart(),
                        a.getPlannedEnd(), a.getActualStart(), a.getActualEnd(), asOf);
                planned = planned.add(ManMonthCalculator.monthMM(a.getAllocationRate(), planSpan, m));
                actual = actual.add(ManMonthCalculator.monthMM(a.getAllocationRate(), actualSpan, m));
            }
            cumPlanned = cumPlanned.add(planned);

            // 미래 월(월 시작일 > asOf)은 실제값이 없다 → actual 관련 값 모두 null
            boolean future = m.atDay(1).isAfter(asOf);
            if (future) {
                series.add(new PlanActualResponse.Point(m, ManMonthCalculator.round2(planned), null,
                        ManMonthCalculator.round2(cumPlanned), null, null, null));
            } else {
                cumActual = cumActual.add(actual);
                series.add(new PlanActualResponse.Point(m,
                        ManMonthCalculator.round2(planned),
                        ManMonthCalculator.round2(actual),
                        ManMonthCalculator.round2(cumPlanned),
                        ManMonthCalculator.round2(cumActual),
                        ManMonthCalculator.round2(actual.subtract(planned)),
                        ManMonthCalculator.round2(cumActual.subtract(cumPlanned))));
            }
        }
        return series;
    }

    private static LocalDate defaultStart(List<Project> projects, List<ProjectAssignment> assignments) {
        LocalDate min = null;
        for (Project p : projects) {
            min = earlier(min, p.getPlannedStartDate());
            min = earlier(min, p.getActualStartDate());
        }
        for (ProjectAssignment a : assignments) {
            min = earlier(min, a.getPlannedStart());
            min = earlier(min, a.getActualStart());
        }
        return min;
    }

    private static LocalDate defaultEnd(List<Project> projects, List<ProjectAssignment> assignments, LocalDate asOf) {
        LocalDate max = null;
        for (Project p : projects) {
            max = later(max, p.getPlannedEndDate());
        }
        for (ProjectAssignment a : assignments) {
            max = later(max, a.getPlannedEnd());
            DateRanges.Span actual = ManMonthCalculator.effectiveSpan(Basis.ACTUAL, a.getPlannedStart(),
                    a.getPlannedEnd(), a.getActualStart(), a.getActualEnd(), asOf);
            if (actual != null) {
                max = later(max, actual.end());
            }
        }
        return max;
    }

    private static LocalDate earlier(LocalDate current, LocalDate candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate.isBefore(current) ? candidate : current;
    }

    private static LocalDate later(LocalDate current, LocalDate candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null || candidate.isAfter(current) ? candidate : current;
    }
}
