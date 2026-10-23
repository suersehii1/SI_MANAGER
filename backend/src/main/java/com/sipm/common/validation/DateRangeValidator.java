package com.sipm.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapperImpl;

import java.lang.reflect.Method;
import java.time.LocalDate;

public class DateRangeValidator implements ConstraintValidator<DateRange, Object> {
    public static final String DATE_RANGE_MSG = "DATE_RANGE|종료일은 시작일과 같거나 이후여야 합니다.";
    public static final String REQUIRED_WITH_END_MSG = "REQUIRED_WITH_END|종료일을 입력하려면 시작일이 필요합니다.";

    private String startField;
    private String endField;
    private boolean requireStartWhenEnd;

    @Override
    public void initialize(DateRange annotation) {
        this.startField = annotation.start();
        this.endField = annotation.end();
        this.requireStartWhenEnd = annotation.requireStartWhenEnd();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDate start = read(value, startField);
        LocalDate end = read(value, endField);

        if (requireStartWhenEnd && end != null && start == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(REQUIRED_WITH_END_MSG)
                    .addPropertyNode(startField)
                    .addConstraintViolation();
            return false;
        }
        if (start != null && end != null && end.isBefore(start)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(DATE_RANGE_MSG)
                    .addPropertyNode(endField)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    /** Reads a property from a record (accessor "field()") or a JavaBean (getter). */
    private static LocalDate read(Object bean, String field) {
        try {
            Method m = bean.getClass().getMethod(field);
            return (LocalDate) m.invoke(bean);
        } catch (NoSuchMethodException e) {
            return (LocalDate) new BeanWrapperImpl(bean).getPropertyValue(field);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read property " + field, e);
        }
    }
}
