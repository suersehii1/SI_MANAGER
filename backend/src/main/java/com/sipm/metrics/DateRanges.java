package com.sipm.metrics;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Calendar-day helpers (inclusive on both ends). Pure, no Spring dependencies. */
public final class DateRanges {
    private DateRanges() {
    }

    /** Inclusive date span; start <= end. */
    public record Span(LocalDate start, LocalDate end) {
    }

    /** days(a, b) = b - a + 1 when a <= b, otherwise 0. */
    public static long days(LocalDate a, LocalDate b) {
        if (a == null || b == null || b.isBefore(a)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(a, b) + 1;
    }

    public static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    public static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    /** overlap(s, e, m) = days(max(s, monthStart), min(e, monthEnd)). */
    public static long overlap(LocalDate s, LocalDate e, YearMonth m) {
        if (s == null || e == null) {
            return 0;
        }
        return days(max(s, m.atDay(1)), min(e, m.atEndOfMonth()));
    }

    /** Months from..to inclusive, ascending. Empty if from > to. */
    public static List<YearMonth> months(YearMonth from, YearMonth to) {
        List<YearMonth> list = new ArrayList<>();
        for (YearMonth m = from; !m.isAfter(to); m = m.plusMonths(1)) {
            list.add(m);
        }
        return list;
    }

    /** Number of months in from..to inclusive. */
    public static long monthCount(YearMonth from, YearMonth to) {
        return ChronoUnit.MONTHS.between(from, to) + 1;
    }
}
