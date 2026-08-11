package com.interviewrecord.auth.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interviewrecord.support.MySqlIntegrationTestBase;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RateLimitServiceTest extends MySqlIntegrationTestBase {

    @Autowired RateLimitService rateLimitService;

    @Test
    void blocksTheSixthAttemptInOneHourWindow() {
        for (int attempt = 0; attempt < 5; attempt++) {
            rateLimitService.check("register-email", "user@example.com", 5, Duration.ofHours(1), Duration.ofHours(1));
        }

        assertThatThrownBy(() -> rateLimitService.check(
                "register-email", "user@example.com", 5, Duration.ofHours(1), Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("RATE_LIMITED");
    }
}
