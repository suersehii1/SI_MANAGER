package com.sipm.common.util;

import java.util.Locale;

public final class LikePatterns {
    private LikePatterns() {
    }

    /**
     * 부분 일치 검색용 소문자 "%kw%" 패턴. 사용자가 입력한 %, _ 가 와일드카드로 동작하지 않도록 '\'로 이스케이프한다.
     * 검색어가 비어 있으면 null(= 조건 없음).
     */
    public static String contains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String escaped = keyword.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
