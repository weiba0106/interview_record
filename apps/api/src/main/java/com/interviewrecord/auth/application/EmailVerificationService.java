package com.interviewrecord.auth.application;

import com.interviewrecord.auth.domain.EmailVerificationToken;
import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaEmailVerificationTokenRepository;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.common.error.InvalidRegistrationException;
import com.interviewrecord.common.token.IssuedToken;
import com.interviewrecord.common.token.SecureTokenService;
import com.interviewrecord.mail.application.MailGateway;
import jakarta.mail.internet.InternetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class EmailVerificationService {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(24);
    private final JpaUserRepository users;
    private final JpaEmailVerificationTokenRepository tokens;
    private final RateLimitService rateLimits;
    private final SecureTokenService secureTokens;
    private final MailGateway mail;
    private final Clock clock;

    public EmailVerificationService(JpaUserRepository users, JpaEmailVerificationTokenRepository tokens,
            RateLimitService rateLimits, SecureTokenService secureTokens, MailGateway mail, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.rateLimits = rateLimits;
        this.secureTokens = secureTokens;
        this.mail = mail;
        this.clock = clock;
    }

    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw invalidToken();
        Instant now = clock.instant();
        EmailVerificationToken token = tokens.findByTokenHashForUpdate(secureTokens.sha256(rawToken))
                .filter(found -> found.isUsableAt(now))
                .orElseThrow(this::invalidToken);
        token.consume(now);
        token.user().verify(now);
    }

    @Transactional
    public void resend(String email, String clientIp) {
        String normalizedEmail = normalizeEmail(email);
        rateLimits.check("resend-verification-cooldown", normalizedEmail, 1, Duration.ofMinutes(1), Duration.ofMinutes(1));
        rateLimits.check("resend-verification-email", normalizedEmail, 5, Duration.ofHours(1), Duration.ofHours(1));
        rateLimits.check("resend-verification-ip", requireClientIp(clientIp), 5, Duration.ofHours(1), Duration.ofHours(1));

        User user = users.findByEmail(normalizedEmail).orElse(null);
        if (user == null || user.isVerified()) return;

        Instant now = clock.instant();
        tokens.findByUserId(user.id()).forEach(token -> token.consume(now));
        IssuedToken issued = secureTokens.issue(TOKEN_LIFETIME);
        tokens.save(new EmailVerificationToken(user, issued.sha256(), issued.expiresAt(), now));
        sendAfterCommit(normalizedEmail, issued.rawValue());
    }

    private void sendAfterCommit(String email, String rawToken) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { mail.sendVerificationEmail(email, rawToken); }
                catch (MailException exception) {
                    log.atWarn().addKeyValue("error_code", "VERIFICATION_DELIVERY_FAILED").log("verification_delivery_failed");
                }
            }
        });
    }

    private InvalidRegistrationException invalidToken() {
        return new InvalidRegistrationException("INVALID_OR_EXPIRED_TOKEN");
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) throw new InvalidRegistrationException("INVALID_EMAIL");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254) throw new InvalidRegistrationException("INVALID_EMAIL");
        try { new InternetAddress(normalized, true).validate(); }
        catch (jakarta.mail.internet.AddressException exception) { throw new InvalidRegistrationException("INVALID_EMAIL"); }
        return normalized;
    }

    private String requireClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) throw new InvalidRegistrationException("INVALID_CLIENT_IP");
        return clientIp;
    }
}
