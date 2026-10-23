package com.sipm.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record PlanActualResponse(Long projectId, String projectCode, String projectName, YearMonth from, YearMonth to,
                                 LocalDate asOf, List<Point> series, Summary summary) {

    public record Point(YearMonth month, BigDecimal plannedMM, BigDecimal actualMM, BigDecimal cumulativePlannedMM,
                        BigDecimal cumulativeActualMM, BigDecimal varianceMM, BigDecimal cumulativeVarianceMM) {
    }

    public record Summary(BigDecimal totalPlannedMM, BigDecimal plannedMMToDate, BigDecimal actualMMToDate,
                          BigDecimal scheduleVarianceMM, Long plannedDurationDays, Long actualDurationDays) {
    }
}
