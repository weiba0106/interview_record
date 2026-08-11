package com.interviewrecord.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaEmailVerificationTokenRepository;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.common.error.InvalidRegistrationException;
import com.interviewrecord.common.error.RateLimitExceededException;
import com.interviewrecord.support.FakeMailGateway;
import com.interviewrecord.support.MySqlIntegrationTestBase;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import({FakeMailGateway.Config.class, EmailVerificationServiceTest.MutableClockConfig.class})
class EmailVerificationServiceTest extends MySqlIntegrationTestBase {
    private static final MutableClock CLOCK = new MutableClock(Instant.parse("2026-08-11T08:00:00Z"));

    @Autowired RegistrationService registrationService;
    @Autowired EmailVerificationService verificationService;
    @Autowired JpaUserRepository users;
    @Autowired JpaEmailVerificationTokenRepository tokens;
    @Autowired FakeMailGateway mail;

    @BeforeEach
    void resetClockAndMail() {
        CLOCK.set(Instant.parse("2026-08-11T08:00:00Z"));
        mail.reset();
    }

    @Test
    void verifiesValidTokenOnlyOnce() {
        RegistrationResult registration = register("verify-once@example.com", "127.0.0.1");
        String rawToken = mail.verificationMessages().getFirst().rawToken();

        verificationService.verify(rawToken);

        User user = users.requireById(registration.userId());
        assertThat(user.isVerified()).isTrue();
        assertThatThrownBy(() -> verificationService.verify(rawToken))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessage("INVALID_OR_EXPIRED_TOKEN");
    }

    @Test
    void rejectsUnknownAndExpiredTokens() {
        register("expired@example.com", "127.0.0.2");
        String rawToken = mail.verificationMessages().getFirst().rawToken();
        CLOCK.set(CLOCK.instant().plusSeconds(24 * 60 * 60 + 1));

        assertThatThrownBy(() -> verificationService.verify(rawToken))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessage("INVALID_OR_EXPIRED_TOKEN");
        assertThatThrownBy(() -> verificationService.verify("not-a-real-token"))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessage("INVALID_OR_EXPIRED_TOKEN");
    }

    @Test
    void resendsForUnverifiedAccountAndInvalidatesPreviousToken() {
        RegistrationResult registration = register("resend@example.com", "127.0.0.3");
        String original = mail.verificationMessages().getFirst().rawToken();
        CLOCK.set(CLOCK.instant().plusSeconds(61));

        verificationService.resend(" resend@example.com ", "127.0.0.3");

        assertThat(mail.verificationMessages()).hasSize(2);
        assertThat(tokens.findByUserId(registration.userId())).hasSize(2);
        assertThatThrownBy(() -> verificationService.verify(original))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessage("INVALID_OR_EXPIRED_TOKEN");
    }

    @Test
    void cooldownRejectsResendWithoutConsumingTheOriginalToken() {
        RegistrationResult registration = register("known-first-resend@example.com", "127.0.0.31");
        String original = mail.verificationMessages().getFirst().rawToken();

        assertThatThrownBy(() -> verificationService.resend("known-first-resend@example.com", "127.0.0.31"))
                .isInstanceOf(RateLimitExceededException.class);

        assertThat(mail.verificationMessages()).hasSize(1);
        assertThat(tokens.findByUserId(registration.userId())).singleElement()
                .matches(token -> token.isUsableAt(CLOCK.instant()));
        verificationService.verify(original);
    }

    @Test
    void resendHasNoExistenceSignalAndDoesNotMailVerifiedUser() {
        register("verified-resend@example.com", "127.0.0.4");
        String rawToken = mail.verificationMessages().getFirst().rawToken();
        verificationService.verify(rawToken);
        CLOCK.set(CLOCK.instant().plusSeconds(61));

        verificationService.resend("verified-resend@example.com", "127.0.0.4");
        verificationService.resend("unknown@example.com", "127.0.0.5");

        assertThat(mail.verificationMessages()).hasSize(1);
    }

    @Test
    void resendRecoversFromRegistrationDeliveryFailure() {
        mail.failVerificationDelivery();
        RegistrationResult registration = register("recovery@example.com", "127.0.0.6");
        mail.reset();
        CLOCK.set(CLOCK.instant().plusSeconds(61));

        verificationService.resend("recovery@example.com", "127.0.0.6");

        assertThat(mail.verificationMessages()).hasSize(1);
        assertThat(tokens.findByUserId(registration.userId())).hasSize(2);
    }

    @Test
    void enforcesCooldownAndPerEmailAndPerIpHourlyCaps() {
        register("email-cap@example.com", "127.0.0.7");
        assertThatThrownBy(() -> verificationService.resend("email-cap@example.com", "127.0.0.7"))
                .isInstanceOf(RateLimitExceededException.class);

        CLOCK.set(CLOCK.instant().plusSeconds(61));
        for (int attempt = 0; attempt < 4; attempt++) {
            verificationService.resend("email-cap@example.com", "127.0.0.7");
            CLOCK.set(CLOCK.instant().plusSeconds(61));
        }
        assertThatThrownBy(() -> verificationService.resend("email-cap@example.com", "127.0.0.7"))
                .isInstanceOf(RateLimitExceededException.class);

        for (int attempt = 0; attempt < 5; attempt++) {
            verificationService.resend("ip-user-" + attempt + "@example.com", "127.0.0.8");
        }
        assertThatThrownBy(() -> verificationService.resend("ip-overflow@example.com", "127.0.0.8"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    private RegistrationResult register(String email, String ip) {
        return registrationService.register(new RegisterCommand(email, "Password123", "测试用户", "Asia/Shanghai", ip));
    }

    @TestConfiguration
    static class MutableClockConfig {
        @Bean @Primary Clock clock() { return CLOCK; }
    }

    static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now;
        MutableClock(Instant now) { this.now = new AtomicReference<>(now); }
        void set(Instant instant) { now.set(instant); }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }
}
