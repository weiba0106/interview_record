package com.interviewrecord.common.error;

public final class EmailAlreadyRegisteredException extends RuntimeException {
    public EmailAlreadyRegisteredException() { super("EMAIL_ALREADY_REGISTERED"); }
}
