package com.sipm.metrics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * MM(Man-Month) 계산 (specs §3). Spring 의존성 없는 순수 클래스.
 * 부분 월 비례 배분: 해당 월에 투입된 달력 일수 ÷ 해당 월 전체 달력 일수 × 투입률.
 *   예) 1/15~1/31 전일 투입(1.00) → 17/31 = 0.548387… → 표시 0.55
 * 합계/누계는 반올림 전 값으로 더하고, 응답 직전에 각 값만 소수 2자리로 반올림한다(표시값 합과 0.01 차이 가능).
 * ACTUAL 기준은 asOf(기준일) 이후를 계산하지 않는다.
 *
 * (English) Man-month calculations. Pure class without Spring dependencies.
 * <ul>
 *   <li>MM(a, m, s, e) = allocation x overlap(s, e, m) / D(m), BigDecimal scale 10 HALF_UP.</li>
 *   <li>Sums are computed from unrounded values; {@link #round2} is applied to each response value.</li>
 * </ul>
 */
public final class ManMonthCalculator {
    public static final int SCALE = 10;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private ManMonthCalculator() {
    }

    /** Effective period for a basis (section 3.2); null when the assignment contributes nothing. */
    public static DateRanges.Span effectiveSpan(Basis basis, LocalDate plannedStart, LocalDate plannedEnd,
                                                LocalDate actualStart, LocalDate actualEnd, LocalDate asOf) {
        if (basis == Basis.PLAN) {
            if (plannedStart == null || plannedEnd == null || plannedEnd.isBefore(plannedStart)) {
                return null;
            }
            return new DateRanges.Span(plannedStart, plannedEnd);
        }
        if (actualStart == null || actualStart.isAfter(asOf)) {
            return null;
        }
        LocalDate end = actualEnd == null ? asOf : DateRanges.min(actualEnd, asOf);
        if (end.isBefore(actualStart)) {
            return null;
        }
        return new DateRanges.Span(actualStart, end);
    }

    /** PLAN span cut at asOf (plannedMMToDate); null when plannedStart > asOf. */
    public static DateRanges.Span plannedSpanToDate(LocalDate plannedStart, LocalDate plannedEnd, LocalDate asOf) {
        if (plannedStart == null || plannedEnd == null || plannedStart.isAfter(asOf)) {
            return null;
        }
        LocalDate end = DateRanges.min(plannedEnd, asOf);
        if (end.isBefore(plannedStart)) {
            return null;
        }
        return new DateRanges.Span(plannedStart, end);
    }

    /** MM for one month (unrounded, scale 10). */
    public static BigDecimal monthMM(BigDecimal allocation, DateRanges.Span span, YearMonth month) {
        if (span == null) {
            return zero();
        }
        long overlap = DateRanges.overlap(span.start(), span.end(), month);
        if (overlap == 0) {
            return zero();
        }
        return allocation.multiply(BigDecimal.valueOf(overlap))
                .divide(BigDecimal.valueOf(month.lengthOfMonth()), SCALE, ROUNDING);
    }

    /** Sum of monthly MM over the whole span (unrounded). */
    public static BigDecimal totalMM(BigDecimal allocation, DateRanges.Span span) {
        BigDecimal total = zero();
        if (span == null) {
            return total;
        }
        for (YearMonth m : DateRanges.months(YearMonth.from(span.start()), YearMonth.from(span.end()))) {
            total = total.add(monthMM(allocation, span, m));
        }
        return total;
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }

    /** Final presentation rounding: scale 2 HALF_UP (null-safe). */
    public static BigDecimal round2(BigDecimal v) {
        return v == null ? null : v.setScale(2, ROUNDING);
    }
}
