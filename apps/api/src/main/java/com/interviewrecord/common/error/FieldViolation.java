package com.interviewrecord.common.error;

import org.springframework.validation.FieldError;

public record FieldViolation(String field, String message) {

    static FieldViolation from(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage());
    }
}
