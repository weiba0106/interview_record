package com.interviewrecord.common.error;

/**
 * Business conflict with a stable machine-readable code, e.g. duplicate names
 * or deletion guards. The code is exposed verbatim in the API error body.
 */
public class ConflictException extends RuntimeException {
    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
