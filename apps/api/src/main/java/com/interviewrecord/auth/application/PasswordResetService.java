package com.interviewrecord.auth.application;

import com.interviewrecord.auth.domain.PasswordResetToken;
import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaPasswordResetTokenRepository;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.auth.infrastructure.SpringSessionRevoker;
import com.interviewrecord.common.error.InvalidRegistrationException;
import com.interviewrecord.common.security.PasswordPolicy;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class PasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);
    private static final Duration RATE_WINDOW = Duration.ofHours(1);
    private static final int RATE_LIMIT = 5;

    private final JpaUserRepository users;
    private final JpaPasswordResetTokenRepository tokens;
    private final RateLimitService rateLimits;
    private final SecureTokenService secureTokens;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;
    private final SpringSessionRevoker sessionRevoker;
    private final MailGateway mail;
    private final Clock clock;

    public PasswordResetService(JpaUserRepository users, JpaPasswordResetTokenRepository tokens,
            RateLimitService rateLimits, SecureTokenService secureTokens, PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder, SpringSessionRevoker sessionRevoker, MailGateway mail, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.rateLimits = rateLimits;
        this.secureTokens = secureTokens;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
        this.sessionRevoker = sessionRevoker;
        this.mail = mail;
        this.clock = clock;
    }

    @Transactional
    public void request(String email, String clientIp) {
        String normalizedEmail = normalizeEmail(email);
        rateLimits.check("forgot-password-email", normalizedEmail, RATE_LIMIT, RATE_WINDOW, RATE_WINDOW);
        rateLimits.check("forgot-password-ip", requireClientIp(clientIp), RATE_LIMIT, RATE_WINDOW, RATE_WINDOW);

        // Lock the stable user row before invalidating and issuing tokens. Token rows alone
        // cannot serialize two first-time reset requests because there may be no token yet.
        User user = users.findByEmailForUpdate(normalizedEmail).orElse(null);
        if (user == null || !user.isVerified()) return;

        Instant now = clock.instant();
        tokens.findByUserId(user.id()).forEach(token -> token.consume(now));
        IssuedToken issued = secureTokens.issue(TOKEN_LIFETIME);
        tokens.save(new PasswordResetToken(user, issued.sha256(), issued.expiresAt(), now));
        sendAfterCommit(normalizedEmail, issued.rawValue());
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) throw invalidToken();
        Instant now = clock.instant();
        PasswordResetToken token = tokens.findByTokenHashForUpdate(secureTokens.sha256(rawToken))
                .filter(found -> found.isUsableAt(now))
                .orElseThrow(this::invalidToken);
        try {
            passwordPolicy.validate(newPassword);
        } catch (IllegalArgumentException exception) {
            throw new InvalidRegistrationException("INVALID_PASSWORD");
        }

        User user = token.user();
        user.changePassword(passwordEncoder.encode(newPassword), now);
        token.consume(now);
        tokens.findByUserId(user.id()).forEach(found -> found.consume(now));
        sessionRevoker.revokeAllForPrincipal(user.email());
    }

    private void sendAfterCommit(String email, String rawToken) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try {
                    mail.sendPasswordResetEmail(email, rawToken);
                } catch (MailException exception) {
                    log.atWarn().addKeyValue("error_code", "PASSWORD_RESET_DELIVERY_FAILED")
                            .log("password_reset_delivery_failed");
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
        try {
            new InternetAddress(normalized, true).validate();
        } catch (jakarta.mail.internet.AddressException exception) {
            throw new InvalidRegistrationException("INVALID_EMAIL");
        }
        return normalized;
    }

    private String requireClientIp(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) throw new InvalidRegistrationException("INVALID_CLIENT_IP");
        return clientIp;
    }
}
