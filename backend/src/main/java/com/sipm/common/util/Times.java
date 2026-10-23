package com.sipm.common.util;

import java.time.LocalDate;
import java.time.ZoneId;

public final class Times {
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private Times() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDate orToday(LocalDate asOf) {
        return asOf != null ? asOf : today();
    }
}
