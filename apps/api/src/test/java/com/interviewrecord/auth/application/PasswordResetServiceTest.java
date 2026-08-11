package com.interviewrecord.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaPasswordResetTokenRepository;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.common.error.InvalidRegistrationException;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

@Import({FakeMailGateway.Config.class, PasswordResetServiceTest.MutableClockConfig.class})
class PasswordResetServiceTest extends MySqlIntegrationTestBase {
    private static final MutableClock CLOCK = new MutableClock(Instant.parse("2026-08-11T08:00:00Z"));

    @Autowired RegistrationService registration;
    @Autowired EmailVerificationService verification;
    @Autowired PasswordResetService resets;
    @Autowired JpaUserRepository users;
    @Autowired JpaPasswordResetTokenRepository tokens;
    @Autowired PasswordEncoder passwords;
    @Autowired FakeMailGateway mail;
    @SuppressWarnings("rawtypes") @Autowired FindByIndexNameSessionRepository sessions;

    @BeforeEach
    void resetClockAndMail() {
        CLOCK.set(Instant.parse("2026-08-11T08:00:00Z"));
        mail.reset();
    }

    @Test
    void resetConsumesTokenChangesPasswordAndRevokesEverySession() {
        User user = verifiedUser("reset@example.com");
        createSessionFor(user.email());
        createSessionFor(user.email());
        resets.request(user.email(), "127.0.0.1");
        String rawToken = mail.passwordResetMessages().getFirst().rawToken();

        resets.reset(rawToken, "NewPassword123");

        assertThat(passwords.matches("NewPassword123", users.requireById(user.id()).passwordHash())).isTrue();
        assertThat(passwords.matches("Password123", users.requireById(user.id()).passwordHash())).isFalse();
        assertThat(tokens.findByUserId(user.id())).allMatch(token -> token.consumedAt() != null);
        assertThat(sessions.findByPrincipalName(user.email())).isEmpty();
        assertThatThrownBy(() -> resets.reset(rawToken, "AnotherPassword123"))
                .isInstanceOf(InvalidRegistrationException.class).hasMessage("INVALID_OR_EXPIRED_TOKEN");
    }

    @Test
    void resetRejectsExpiredConsumedAndUnknownTokens() {
        User user = verifiedUser("expired-reset@example.com");
        resets.request(user.email(), "127.0.0.2");
        String rawToken = mail.passwordResetMessages().getFirst().rawToken();
        CLOCK.set(CLOCK.instant().plusSeconds(60 * 60 + 1));

        assertThatThrownBy(() -> resets.reset(rawToken, "NewPassword123"))
                .isInstanceOf(InvalidRegistrationException.class).hasMessage("INVALID_OR_EXPIRED_TOKEN");
        assertThatThrownBy(() -> resets.reset("unknown", "NewPassword123"))
                .isInstanceOf(InvalidRegistrationException.class).hasMessage("INVALID_OR_EXPIRED_TOKEN");
    }

    @Test
    void forgotPasswordHasNoAccountExistenceSignal() {
        resets.request("unknown@example.com", "127.0.0.3");

        assertThat(mail.passwordResetMessages()).isEmpty();
    }

    @Test
    void resetEnforcesPasswordPolicyWithoutConsumingTheToken() {
        User user = verifiedUser("policy-reset@example.com");
        resets.request(user.email(), "127.0.0.4");
        String rawToken = mail.passwordResetMessages().getFirst().rawToken();

        assertThatThrownBy(() -> resets.reset(rawToken, "PasswordOnly"))
                .isInstanceOf(InvalidRegistrationException.class).hasMessage("INVALID_PASSWORD");
        assertThat(passwords.matches("Password123", users.requireById(user.id()).passwordHash())).isTrue();
        resets.reset(rawToken, "NewPassword123");
    }

    @Test
    void forgotPasswordIsLimitedToFivePerEmailAndIpPerHour() {
        User user = verifiedUser("email-cap-reset@example.com");
        for (int attempt = 0; attempt < 5; attempt++) {
            resets.request(user.email(), "127.0.0.5");
        }
        assertThatThrownBy(() -> resets.request(user.email(), "127.0.0.6"))
                .isInstanceOf(com.interviewrecord.common.error.RateLimitExceededException.class);

        for (int attempt = 0; attempt < 5; attempt++) {
            resets.request("ip-cap-" + attempt + "@example.com", "127.0.0.7");
        }
        assertThatThrownBy(() -> resets.request("ip-cap-overflow@example.com", "127.0.0.7"))
                .isInstanceOf(com.interviewrecord.common.error.RateLimitExceededException.class);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void createSessionFor(String email) {
        Session session = (Session) sessions.createSession();
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, email);
        sessions.save(session);
    }

    private User verifiedUser(String email) {
        RegistrationResult result = registration.register(new RegisterCommand(email, "Password123", "测试用户", "Asia/Shanghai", "127.0.0.9"));
        verification.verify(mail.verificationMessages().getLast().rawToken());
        return users.requireById(result.userId());
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
