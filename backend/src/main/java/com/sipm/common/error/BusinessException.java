package com.sipm.common.error;

import java.util.List;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final List<FieldErrorItem> fieldErrors;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage(), List.of());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public BusinessException(ErrorCode errorCode, String message, List<FieldErrorItem> fieldErrors) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors == null ? List.of() : fieldErrors;
    }

    public ErrorCode getErrorCode() { return errorCode; }
    public List<FieldErrorItem> getFieldErrors() { return fieldErrors; }

    public static BusinessException notFound(String resource) {
        return new BusinessException(ErrorCode.NOT_FOUND, resource + "을(를) 찾을 수 없습니다.");
    }

    public static BusinessException validation(String field, String code, String message, Object rejected) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(),
                List.of(new FieldErrorItem(field, code, message, rejected)));
    }
}
