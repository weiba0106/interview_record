package com.interviewrecord.common.error;

import java.time.Duration;

public final class RateLimitExceededException extends RuntimeException {
    private final Duration retryAfter;
    public RateLimitExceededException(Duration retryAfter) { super("RATE_LIMITED"); this.retryAfter = retryAfter; }
    public Duration retryAfter() { return retryAfter; }
}
