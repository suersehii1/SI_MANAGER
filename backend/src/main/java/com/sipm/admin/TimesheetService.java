package com.sipm.admin;

import com.sipm.admin.dto.MonthValue;
import com.sipm.admin.dto.TimesheetResponse;
import com.sipm.assignment.AssignmentRepository;
import com.sipm.assignment.ProjectAssignment;
import com.sipm.common.error.BusinessException;
import com.sipm.metrics.Basis;
import com.sipm.metrics.DateRanges;
import com.sipm.metrics.ManMonthCalculator;
import com.sipm.project.ProjectRepository;
import com.sipm.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 월별 타임시트 (specs §3.4).
 * 셀 = 할당의 basis 유효 기간과 월의 겹침 일수 비례 MM. 사용자 월합/합계/월 총합은 모두 반올림 전 값으로 합산하고,
 * 응답에 담는 각 숫자만 마지막에 소수 2자리로 반올림한다. 과다 할당 판정도 반올림 전 값으로 비교한다.
 */
@Service
public class TimesheetService {
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final AssignmentRepository assignmentRepository;
    private final ProjectRepository projectRepository;

    public TimesheetService(AssignmentRepository assignmentRepository, ProjectRepository projectRepository) {
        this.assignmentRepository = assignmentRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public TimesheetResponse timesheet(YearMonth from, YearMonth to, Basis basis, Long projectId, LocalDate asOf) {
        // 기본값: asOf 연도의 1월 ~ 12월, basis 기본 ACTUAL
        if (from == null) {
            from = YearMonth.of(asOf.getYear(), 1);
        }
        if (to == null) {
            to = YearMonth.of(asOf.getYear(), 12);
        }
        if (basis == null) {
            basis = Basis.ACTUAL;
        }
        MonthRangeValidator.validate(from, to);

        List<ProjectAssignment> assignments;
        if (projectId != null) {
            if (!projectRepository.existsById(projectId)) {
                throw BusinessException.notFound("프로젝트");
            }
            assignments = assignmentRepository.findByProjectIdsFetch(List.of(projectId));
        } else {
            assignments = assignmentRepository.findAllFetch();
        }

        List<YearMonth> months = DateRanges.months(from, to);
        LocalDate rangeStart = from.atDay(1);
        LocalDate rangeEnd = to.atEndOfMonth();

        // 1) 대상 할당 선별: basis 유효 기간이 존재하고 조회 범위와 1일 이상 겹치는 것만. 사용자별로 묶는다.
        Map<Long, List<ProjectAssignment>> byUser = new LinkedHashMap<>();
        Map<Long, DateRanges.Span> spans = new LinkedHashMap<>();
        for (ProjectAssignment a : assignments) {
            DateRanges.Span span = ManMonthCalculator.effectiveSpan(basis, a.getPlannedStart(), a.getPlannedEnd(),
                    a.getActualStart(), a.getActualEnd(), asOf);
            if (span == null || DateRanges.days(DateRanges.max(span.start(), rangeStart),
                    DateRanges.min(span.end(), rangeEnd)) == 0) {
                continue;
            }
            spans.put(a.getId(), span);
            byUser.computeIfAbsent(a.getUser().getId(), k -> new ArrayList<>()).add(a);
        }

        // 2) 사용자 행 구성
        List<TimesheetResponse.Row> rows = new ArrayList<>();
        BigDecimal[] monthTotals = zeros(months.size());
        for (List<ProjectAssignment> userAssignments : byUser.values()) {
            User user = userAssignments.get(0).getUser();
            userAssignments.sort(Comparator.comparing(a -> a.getProject().getCode()));

            BigDecimal[] userMonths = zeros(months.size());
            List<TimesheetResponse.ProjectRow> projectRows = new ArrayList<>();
            for (ProjectAssignment a : userAssignments) {
                DateRanges.Span span = spans.get(a.getId());
                List<MonthValue> cells = new ArrayList<>();
                BigDecimal rowTotal = ManMonthCalculator.zero();
                for (int i = 0; i < months.size(); i++) {
                    BigDecimal mm = ManMonthCalculator.monthMM(a.getAllocationRate(), span, months.get(i));
                    cells.add(new MonthValue(months.get(i), ManMonthCalculator.round2(mm)));
                    rowTotal = rowTotal.add(mm);
                    userMonths[i] = userMonths[i].add(mm);
                }
                projectRows.add(new TimesheetResponse.ProjectRow(a.getId(), a.getProject().getId(),
                        a.getProject().getCode(), a.getProject().getName(), a.getRoleInProject(),
                        a.getAllocationRate(), span.start(), span.end(), a.getPlannedStart(), a.getPlannedEnd(),
                        a.getActualStart(), a.getActualEnd(), cells, ManMonthCalculator.round2(rowTotal)));
            }

            List<MonthValue> userCells = new ArrayList<>();
            List<YearMonth> overAllocated = new ArrayList<>();
            BigDecimal userTotal = ManMonthCalculator.zero();
            for (int i = 0; i < months.size(); i++) {
                userCells.add(new MonthValue(months.get(i), ManMonthCalculator.round2(userMonths[i])));
                userTotal = userTotal.add(userMonths[i]);
                monthTotals[i] = monthTotals[i].add(userMonths[i]);
                // 과다 할당: 반올림 전 사용자 월합 > 1.00
                if (userMonths[i].compareTo(ONE) > 0) {
                    overAllocated.add(months.get(i));
                }
            }
            rows.add(new TimesheetResponse.Row(user.getId(), user.getUsername(), user.getName(), userCells,
                    ManMonthCalculator.round2(userTotal), overAllocated, projectRows));
        }
        // rows 정렬: 사용자 이름 오름차순
        rows.sort(Comparator.comparing(TimesheetResponse.Row::name).thenComparing(TimesheetResponse.Row::userId));

        List<MonthValue> totals = new ArrayList<>();
        BigDecimal grand = ManMonthCalculator.zero();
        for (int i = 0; i < months.size(); i++) {
            totals.add(new MonthValue(months.get(i), ManMonthCalculator.round2(monthTotals[i])));
            grand = grand.add(monthTotals[i]);
        }
        return new TimesheetResponse(from, to, basis, asOf, months, rows, totals, ManMonthCalculator.round2(grand));
    }

    private static BigDecimal[] zeros(int n) {
        BigDecimal[] arr = new BigDecimal[n];
        for (int i = 0; i < n; i++) {
            arr[i] = ManMonthCalculator.zero();
        }
        return arr;
    }
}
