package com.sipm.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_SORT(HttpStatus.BAD_REQUEST, "허용되지 않은 정렬 조건입니다."),
    ASSIGNMENT_OUT_OF_PROJECT_PERIOD(HttpStatus.BAD_REQUEST, "할당 계획 기간이 프로젝트 계획 기간을 벗어났습니다."),
    USER_DISABLED(HttpStatus.BAD_REQUEST, "비활성화된 사용자는 지정할 수 없습니다."),
    SELF_MODIFICATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신을 비활성화하거나 관리자 권한을 제거할 수 없습니다."),
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "빈 파일은 업로드할 수 없습니다."),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "파일 크기는 2MB를 초과할 수 없습니다."),
    FILE_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다."),
    INVALID_FILENAME(HttpStatus.BAD_REQUEST, "파일명이 올바르지 않습니다."),
    WBS_OUT_OF_PROJECT_PERIOD(HttpStatus.BAD_REQUEST, "WBS 계획 기간이 프로젝트 계획 기간을 벗어났습니다."),
    WBS_ASSIGNEE_NOT_MEMBER(HttpStatus.BAD_REQUEST, "담당자는 해당 프로젝트에 할당된 사용자여야 합니다."),
    WBS_INVALID_PARENT(HttpStatus.BAD_REQUEST, "상위 항목이 올바르지 않습니다."),
    WBS_INVALID_MOVE(HttpStatus.BAD_REQUEST, "자기 자신 또는 하위 항목으로 이동할 수 없습니다."),
    WBS_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "WBS 깊이는 최대 5단계까지 허용됩니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_DISABLED(HttpStatus.UNAUTHORIZED, "비활성화된 계정입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    DUPLICATE_PROJECT_CODE(HttpStatus.CONFLICT, "이미 사용 중인 프로젝트 코드입니다."),
    DUPLICATE_ASSIGNMENT(HttpStatus.CONFLICT, "같은 사람이 이 프로젝트에 투입 기간이 겹치게 할당되어 있습니다."),
    PROJECT_PERIOD_CONFLICT(HttpStatus.CONFLICT, "기존 할당이 새 프로젝트 계획 기간을 벗어납니다."),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "데이터 제약 조건을 위반했습니다."),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기는 2MB를 초과할 수 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() { return status; }
    public String defaultMessage() { return defaultMessage; }

    /** API-level code string. PAYLOAD_TOO_LARGE is reported as FILE_TOO_LARGE (413). */
    public String apiCode() {
        return this == PAYLOAD_TOO_LARGE ? "FILE_TOO_LARGE" : name();
    }
}
