package com.sipm.admin;

import com.sipm.common.error.BusinessException;
import com.sipm.metrics.DateRanges;

import java.time.YearMonth;

/** 월 범위 쿼리 검증 (specs §5.4): from > to → to/DATE_RANGE, 60개월 초과 → to/RANGE_TOO_LARGE. */
final class MonthRangeValidator {
    static final int MAX_MONTHS = 60;

    private MonthRangeValidator() {
    }

    static void validate(YearMonth from, YearMonth to) {
        if (from.isAfter(to)) {
            throw BusinessException.validation("to", "DATE_RANGE", "종료 월은 시작 월과 같거나 이후여야 합니다.", to.toString());
        }
        if (DateRanges.monthCount(from, to) > MAX_MONTHS) {
            throw BusinessException.validation("to", "RANGE_TOO_LARGE", "조회 범위는 최대 60개월입니다.", to.toString());
        }
    }
}
