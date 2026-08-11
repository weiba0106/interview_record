package com.interviewrecord.common.error;

public final class InvalidRegistrationException extends RuntimeException {
    public InvalidRegistrationException(String code) { super(code); }
}
