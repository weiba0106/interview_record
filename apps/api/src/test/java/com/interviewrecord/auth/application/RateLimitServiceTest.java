package com.interviewrecord.auth.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interviewrecord.auth.infrastructure.JpaRateLimitBucketRepository;
import com.interviewrecord.common.error.RateLimitExceededException;
import com.interviewrecord.common.token.SecureTokenService;
import com.interviewrecord.support.MySqlIntegrationTestBase;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RateLimitServiceTest extends MySqlIntegrationTestBase {

    @Autowired RateLimitService rateLimitService;
    @Autowired JpaRateLimitBucketRepository buckets;
    @Autowired SecureTokenService secureTokens;

    @Test
    void blocksTheSixthAttemptInOneHourWindow() {
        for (int attempt = 0; attempt < 5; attempt++) {
            rateLimitService.check("register-email", "user@example.com", 5, Duration.ofHours(1), Duration.ofHours(1));
        }

        assertThatThrownBy(() -> rateLimitService.check(
                "register-email", "user@example.com", 5, Duration.ofHours(1), Duration.ofHours(1)))
                .isInstanceOf(RateLimitExceededException.class);

        JpaRateLimitBucketRepository.Bucket bucket = buckets.find(
                "register-email", secureTokens.sha256("register-email:user@example.com")).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(bucket.attemptCount()).isEqualTo(6);
        org.assertj.core.api.Assertions.assertThat(bucket.blockedUntil()).isNotNull();
    }

    @Test
    void successfulLoginResetsOnlyItsOwnFailureBucket() {
        for (int attempt = 0; attempt < 10; attempt++) {
            rateLimitService.check("login-email", "user@example.com", 10, Duration.ofMinutes(15), Duration.ofMinutes(15));
        }

        assertThatThrownBy(() -> rateLimitService.check(
                "login-email", "user@example.com", 10, Duration.ofMinutes(15), Duration.ofMinutes(15)))
                .isInstanceOf(RateLimitExceededException.class);

        rateLimitService.reset("login-email", "user@example.com");

        org.assertj.core.api.Assertions.assertThatCode(() -> rateLimitService.check(
                "login-email", "user@example.com", 10, Duration.ofMinutes(15), Duration.ofMinutes(15)))
                .doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThat(buckets.find(
                "login-email", secureTokens.sha256("login-email:user@example.com")))
                .isPresent();
    }
}
