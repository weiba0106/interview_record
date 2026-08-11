package com.interviewrecord.auth.application;

import com.interviewrecord.auth.domain.EmailVerificationToken;
import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaEmailVerificationTokenRepository;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.common.error.InvalidRegistrationException;
import com.interviewrecord.common.token.IssuedToken;
import com.interviewrecord.common.token.SecureTokenService;
import com.interviewrecord.mail.application.DeferredMailDelivery;
import jakarta.mail.internet.InternetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class EmailVerificationService {
    private static final Duration TOKEN_LIFETIME = Duration.ofHours(24);
    private final JpaUserRepository users;
    private final JpaEmailVerificationTokenRepository tokens;
    private final RateLimitService rateLimits;
    private final SecureTokenService secureTokens;
    private final DeferredMailDelivery mailDelivery;
    private final Clock clock;

    public EmailVerificationService(JpaUserRepository users, JpaEmailVerificationTokenRepository tokens,
            RateLimitService rateLimits, SecureTokenService secureTokens, DeferredMailDelivery mailDelivery, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.rateLimits = rateLimits;
        this.secureTokens = secureTokens;
        this.mailDelivery = mailDelivery;
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
        Instant now = clock.instant();
        // Keep unknown and verified addresses on the same token/hash and token-query path
        // without creating a token or sending mail for an address that must remain private.
        long tokenOwnerId = user == null ? -1L : user.id();
        tokens.findByUserId(tokenOwnerId).forEach(token -> token.consume(now));
        IssuedToken issued = secureTokens.issue(TOKEN_LIFETIME);
        if (user == null || user.isVerified()) return;
        tokens.save(new EmailVerificationToken(user, issued.sha256(), issued.expiresAt(), now));
        sendAfterCommit(normalizedEmail, issued.rawValue());
    }

    private void sendAfterCommit(String email, String rawToken) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                mailDelivery.sendVerificationEmail(email, rawToken);
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
