package com.sipm.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

/** specs §3.6(a) 할당별 월 MM 표 전체 셀 + §3.1~3.2 경계 규칙 검증. */
class ManMonthCalculatorTest {
    private static final LocalDate AS_OF = LocalDate.of(2026, 3, 15);

    /** A1(kim, P1, 1.00), A2(lee, P1, 0.50), A3(kim, P2, 0.50) — 시드와 동일 */
    private static DateRanges.Span span(String assignment, Basis basis) {
        return switch (assignment) {
            case "A1" -> ManMonthCalculator.effectiveSpan(basis, d("2026-01-01"), d("2026-03-31"),
                    d("2026-01-15"), d("2026-03-31"), AS_OF);
            case "A2" -> ManMonthCalculator.effectiveSpan(basis, d("2026-02-01"), d("2026-03-31"),
                    d("2026-02-01"), null, AS_OF);
            default -> ManMonthCalculator.effectiveSpan(basis, d("2026-03-16"), d("2026-04-30"), null, null, AS_OF);
        };
    }

    private static BigDecimal alloc(String assignment) {
        return assignment.equals("A1") ? new BigDecimal("1.00") : new BigDecimal("0.50");
    }

    @ParameterizedTest(name = "{0} {1}: {2} {3} {4} {5}")
    @CsvSource({
            "A1, PLAN,   1.00, 1.00, 1.00, 0.00",
            "A2, PLAN,   0.00, 0.50, 0.50, 0.00",
            "A3, PLAN,   0.00, 0.00, 0.26, 0.50",
            "A1, ACTUAL, 0.55, 1.00, 0.48, 0.00",
            "A2, ACTUAL, 0.00, 0.50, 0.24, 0.00",
            "A3, ACTUAL, 0.00, 0.00, 0.00, 0.00"
    })
    void monthlyMmTable(String a, Basis basis, String jan, String feb, String mar, String apr) {
        DateRanges.Span s = span(a, basis);
        String[] expected = {jan, feb, mar, apr};
        for (int i = 0; i < 4; i++) {
            BigDecimal mm = ManMonthCalculator.monthMM(alloc(a), s, YearMonth.of(2026, i + 1));
            assertThat(ManMonthCalculator.round2(mm)).as(a + " " + basis + " month " + (i + 1))
                    .isEqualByComparingTo(expected[i]);
        }
    }

    @Test
    void exactFractionsBeforeRounding() {
        // 17/31 = 0.5483870968 (scale 10, HALF_UP)
        BigDecimal jan = ManMonthCalculator.monthMM(BigDecimal.ONE, span("A1", Basis.ACTUAL), YearMonth.of(2026, 1));
        assertThat(jan).isEqualByComparingTo("0.5483870968");
        // 0.5 × 16/31 = 0.2580645161
        BigDecimal mar = ManMonthCalculator.monthMM(new BigDecimal("0.50"), span("A3", Basis.PLAN), YearMonth.of(2026, 3));
        assertThat(mar).isEqualByComparingTo("0.2580645161");
    }

    @Test
    void actualSpanIsCutAtAsOfAndFutureStartContributesNothing() {
        DateRanges.Span a1 = span("A1", Basis.ACTUAL);
        assertThat(a1.end()).isEqualTo(AS_OF); // actual_end 03-31이 입력돼 있어도 asOf에서 자름
        assertThat(span("A3", Basis.ACTUAL)).isNull();
        DateRanges.Span future = ManMonthCalculator.effectiveSpan(Basis.ACTUAL, d("2026-01-01"), d("2026-12-31"),
                d("2026-04-01"), null, AS_OF);
        assertThat(future).isNull();
    }

    @Test
    void totalsAndPlannedToDate() {
        // A1 PLAN 전 기간 = 3.00, A3 PLAN = 0.758065
        assertThat(ManMonthCalculator.round2(ManMonthCalculator.totalMM(BigDecimal.ONE, span("A1", Basis.PLAN))))
                .isEqualByComparingTo("3.00");
        assertThat(ManMonthCalculator.round2(
                ManMonthCalculator.totalMM(new BigDecimal("0.50"), span("A3", Basis.PLAN))))
                .isEqualByComparingTo("0.76");
        // plannedMMToDate: A1 01-01~03-15 = 2.483871, A3는 시작이 asOf 이후라 null
        DateRanges.Span a1ToDate = ManMonthCalculator.plannedSpanToDate(d("2026-01-01"), d("2026-03-31"), AS_OF);
        assertThat(ManMonthCalculator.round2(ManMonthCalculator.totalMM(BigDecimal.ONE, a1ToDate)))
                .isEqualByComparingTo("2.48");
        assertThat(ManMonthCalculator.plannedSpanToDate(d("2026-03-16"), d("2026-04-30"), AS_OF)).isNull();
    }

    @Test
    void calendarDayRules() {
        assertThat(DateRanges.days(d("2026-01-01"), d("2026-03-31"))).isEqualTo(90);
        assertThat(DateRanges.days(d("2026-03-01"), d("2026-03-01"))).isEqualTo(1);
        assertThat(DateRanges.days(d("2026-03-02"), d("2026-03-01"))).isEqualTo(0);
        assertThat(YearMonth.of(2026, 2).lengthOfMonth()).isEqualTo(28);
        assertThat(DateRanges.overlap(d("2026-01-15"), d("2026-03-15"), YearMonth.of(2026, 2))).isEqualTo(28);
    }

    private static LocalDate d(String s) {
        return LocalDate.parse(s);
    }
}
