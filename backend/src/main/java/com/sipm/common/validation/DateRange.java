package com.sipm.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 클래스 레벨 날짜 순서 검증 (specs §5.1): 종료일 ≥ 시작일(같은 날 허용).
 * 위반은 종료 필드에 DATE_RANGE로, requireStartWhenEnd이면 "종료만 있고 시작 없음"을 시작 필드에 REQUIRED_WITH_END로 붙인다.
 * 메시지를 "CODE|메시지"로 만들어 두면 GlobalExceptionHandler가 fieldErrors의 code/message로 나눈다.
 *
 * (English) Class-level constraint: end >= start (same day allowed).
 * Violations are attached to the end property with code DATE_RANGE;
 * when requireStartWhenEnd is true and end != null and start == null, the violation is
 * attached to start with code REQUIRED_WITH_END.
 * The violation message is encoded as "CODE|message" and split by the exception handler.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Repeatable(DateRange.List.class)
public @interface DateRange {
    String message() default "DATE_RANGE|종료일은 시작일과 같거나 이후여야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    String start();

    String end();

    boolean requireStartWhenEnd() default false;

    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        DateRange[] value();
    }
}
