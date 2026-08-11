package com.interviewrecord.auth.application;

import java.time.Duration;

public interface RateLimitService {
    void check(String action, String subject, int limit, Duration window, Duration blockDuration);
    void reset(String action, String subject);
}
