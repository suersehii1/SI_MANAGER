package com.sipm.dashboard;

import com.sipm.assignment.ProjectAssignment;
import com.sipm.metrics.Basis;
import com.sipm.metrics.DateRanges;
import com.sipm.metrics.ManMonthCalculator;
import com.sipm.project.Project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 프로젝트 단위 지표 계산 (specs §3.3). Dashboard와 Plan-vs-Actual summary가 공유한다.
 * 모든 MM 값은 반올림 전(scale 10) 값이며, 응답 직전에 round2 한다.
 */
public final class ProjectMetrics {
    private ProjectMetrics() {
    }

    /** 할당별 PLAN 기준 전체 기간 MM 합. */
    public static BigDecimal totalPlannedMM(List<ProjectAssignment> assignments, LocalDate asOf) {
        BigDecimal sum = ManMonthCalculator.zero();
        for (ProjectAssignment a : assignments) {
            DateRanges.Span span = ManMonthCalculator.effectiveSpan(Basis.PLAN, a.getPlannedStart(),
                    a.getPlannedEnd(), a.getActualStart(), a.getActualEnd(), asOf);
            sum = sum.add(ManMonthCalculator.totalMM(a.getAllocationRate(), span));
        }
        return sum;
    }

    /** PLAN 기준이되 종료를 min(planned_end, asOf)로 자른 MM 합 (planned_start > asOf면 0). */
    public static BigDecimal plannedMMToDate(List<ProjectAssignment> assignments, LocalDate asOf) {
        BigDecimal sum = ManMonthCalculator.zero();
        for (ProjectAssignment a : assignments) {
            DateRanges.Span span = ManMonthCalculator.plannedSpanToDate(a.getPlannedStart(), a.getPlannedEnd(), asOf);
            sum = sum.add(ManMonthCalculator.totalMM(a.getAllocationRate(), span));
        }
        return sum;
    }

    /** ACTUAL 기준(asOf에서 자름) MM 합. 미투입(actual_start null 또는 > asOf)은 0. */
    public static BigDecimal actualMMToDate(List<ProjectAssignment> assignments, LocalDate asOf) {
        BigDecimal sum = ManMonthCalculator.zero();
        for (ProjectAssignment a : assignments) {
            DateRanges.Span span = ManMonthCalculator.effectiveSpan(Basis.ACTUAL, a.getPlannedStart(),
                    a.getPlannedEnd(), a.getActualStart(), a.getActualEnd(), asOf);
            sum = sum.add(ManMonthCalculator.totalMM(a.getAllocationRate(), span));
        }
        return sum;
    }

    /** asOf 시점에 실제 투입 중인지: actual_start <= asOf 이고 (actual_end null 또는 >= asOf). */
    public static boolean isActive(ProjectAssignment a, LocalDate asOf) {
        return a.getActualStart() != null && !a.getActualStart().isAfter(asOf)
                && (a.getActualEnd() == null || !a.getActualEnd().isBefore(asOf));
    }

    /** asOf가 계획 기간 안에 있는지. */
    public static boolean isPlannedActive(ProjectAssignment a, LocalDate asOf) {
        return !a.getPlannedStart().isAfter(asOf) && !a.getPlannedEnd().isBefore(asOf);
    }

    public static long plannedDurationDays(Project p) {
        return DateRanges.days(p.getPlannedStartDate(), p.getPlannedEndDate());
    }

    /** actual_start가 null이거나 asOf 이후면 null, 아니면 days(actual_start, min(coalesce(actual_end, asOf), asOf)). */
    public static Long actualDurationDays(Project p, LocalDate asOf) {
        if (p.getActualStartDate() == null || p.getActualStartDate().isAfter(asOf)) {
            return null;
        }
        LocalDate end = p.getActualEndDate() == null ? asOf : DateRanges.min(p.getActualEndDate(), asOf);
        return DateRanges.days(p.getActualStartDate(), end);
    }
}
