package com.sipm.common.error;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldErrorItem> fieldErrors) {

    public static ApiError of(ErrorCode code, String message, String path, List<FieldErrorItem> fieldErrors) {
        return new ApiError(
                OffsetDateTime.now(ZoneId.of("Asia/Seoul")),
                code.status().value(),
                code.status().getReasonPhrase(),
                code.apiCode(),
                message != null ? message : code.defaultMessage(),
                path,
                fieldErrors != null ? fieldErrors : List.of());
    }
}
