package com.sipm.common.error;

public record FieldErrorItem(String field, String code, String message, Object rejectedValue) {
}
