package com.sipm.common.error;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * 전역 예외 → 공통 에러 JSON(ApiError) 매핑 (specs §4.1).
 * 주의: AccessDeniedException(@PreAuthorize/소유자 검사)과 AuthenticationException은 별도 핸들러로
 * 403/401을 돌려준다. 마지막의 Exception 포괄 핸들러가 이를 500으로 바꾸지 않도록 하기 위함.
 * 날짜 순서 위반(@DateRange)은 메시지를 "CODE|메시지" 형태로 만들어 두고 여기서 code/message로 분리한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ApiError> build(ErrorCode code, String message, HttpServletRequest req,
                                           List<FieldErrorItem> fieldErrors) {
        return ResponseEntity.status(code.status())
                .body(ApiError.of(code, message, ErrorResponseWriter.originalPath(req), fieldErrors));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest req) {
        return build(ex.getErrorCode(), ex.getMessage(), req, ex.getFieldErrors());
    }

    @ExceptionHandler(BindException.class) // includes MethodArgumentNotValidException
    public ResponseEntity<ApiError> handleBind(BindException ex, HttpServletRequest req) {
        List<FieldErrorItem> items = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            items.add(toItem(fe.getField(), fe.getCode(), fe.getDefaultMessage(), fe.getRejectedValue(),
                    fe.isBindingFailure()));
        }
        for (ObjectError oe : ex.getBindingResult().getGlobalErrors()) {
            items.add(toItem(oe.getObjectName(), oe.getCode(), oe.getDefaultMessage(), null, false));
        }
        return build(ErrorCode.VALIDATION_ERROR, null, req, items);
    }

    private FieldErrorItem toItem(String field, String code, String message, Object rejected, boolean bindingFailure) {
        String c = code;
        String m = message;
        if (message != null) {
            int idx = message.indexOf('|');
            if (idx > 0) {
                c = message.substring(0, idx);
                m = message.substring(idx + 1);
            }
        }
        if (bindingFailure) {
            c = "TYPE_MISMATCH";
            m = "값의 형식이 올바르지 않습니다.";
        }
        return new FieldErrorItem(field, c, m, rejected);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(ErrorCode.VALIDATION_ERROR, null, req,
                List.of(new FieldErrorItem(ex.getName(), "TYPE_MISMATCH", "값의 형식이 올바르지 않습니다.", ex.getValue())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex,
                                                       HttpServletRequest req) {
        return build(ErrorCode.VALIDATION_ERROR, null, req,
                List.of(new FieldErrorItem(ex.getParameterName(), "REQUIRED", "필수 파라미터입니다.", null)));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest req) {
        return build(ErrorCode.FILE_EMPTY, null, req, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(ErrorCode.VALIDATION_ERROR, "요청 본문 형식이 올바르지 않습니다.", req, List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return build(ErrorCode.PAYLOAD_TOO_LARGE, null, req, List.of());
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiError> handleMultipart(MultipartException ex, HttpServletRequest req) {
        Throwable t = ex;
        while (t != null) {
            String msg = t.getMessage();
            if (t.getClass().getSimpleName().contains("SizeLimitExceeded")
                    || (msg != null && msg.toLowerCase().contains("exceed"))) {
                return build(ErrorCode.PAYLOAD_TOO_LARGE, null, req, List.of());
            }
            t = t.getCause();
        }
        return build(ErrorCode.FILE_EMPTY, "멀티파트 요청이 올바르지 않습니다.", req, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return build(ErrorCode.VALIDATION_ERROR, "지원하지 않는 Content-Type입니다.", req, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return build(ErrorCode.METHOD_NOT_ALLOWED, null, req, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(ErrorCode.NOT_FOUND, "요청한 경로를 찾을 수 없습니다.", req, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(ErrorCode.FORBIDDEN, null, req, List.of());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return build(ErrorCode.BAD_CREDENTIALS, null, req, List.of());
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex, HttpServletRequest req) {
        return build(ErrorCode.ACCOUNT_DISABLED, null, req, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        return build(ErrorCode.UNAUTHORIZED, null, req, List.of());
    }

    /** 같은 순간에 두 사람이 저장해서 DB의 버전 검사(@Version)에 걸린 경우 → 409 */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        log.info("Optimistic lock conflict on {}: {}", req.getRequestURI(), ex.getMessage());
        return build(ErrorCode.CONCURRENT_MODIFICATION, null, req, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.DATA_INTEGRITY_VIOLATION, null, req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {}", req.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, null, req, List.of());
    }
}
