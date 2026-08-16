package com.interviewrecord.common.error;

/**
 * Business input that passes bean validation but violates a domain rule,
 * e.g. an unsafe apply URL or an end time before the start time.
 */
public class InvalidInputException extends RuntimeException {
    private final String code;

    public InvalidInputException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
