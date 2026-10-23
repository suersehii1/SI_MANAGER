package com.sipm.admin.dto;

import com.sipm.metrics.Basis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record TimesheetResponse(YearMonth from, YearMonth to, Basis basis, LocalDate asOf, List<YearMonth> months,
                                List<Row> rows, List<MonthValue> monthTotals, BigDecimal grandTotal) {

    /** 사용자 행 (children = 할당별 프로젝트 행). */
    public record Row(Long userId, String username, String name, List<MonthValue> cells, BigDecimal total,
                      List<YearMonth> overAllocatedMonths, List<ProjectRow> projects) {
    }

    public record ProjectRow(Long assignmentId, Long projectId, String projectCode, String projectName,
                             String roleInProject, BigDecimal allocationRate, LocalDate start, LocalDate end,
                             LocalDate plannedStart, LocalDate plannedEnd, LocalDate actualStart, LocalDate actualEnd,
                             List<MonthValue> cells, BigDecimal total) {
    }
}
