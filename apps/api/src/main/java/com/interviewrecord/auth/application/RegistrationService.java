package com.interviewrecord.auth.application;

import com.interviewrecord.auth.domain.EmailVerificationToken;
import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaEmailVerificationTokenRepository;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.common.security.PasswordPolicy;
import com.interviewrecord.common.token.IssuedToken;
import com.interviewrecord.common.token.SecureTokenService;
import com.interviewrecord.defaults.application.UserDefaultsService;
import com.interviewrecord.mail.application.MailGateway;
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
public class RegistrationService {
    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    private final JpaUserRepository users;
    private final JpaEmailVerificationTokenRepository verificationTokens;
    private final UserDefaultsService defaults;
    private final RateLimitService rateLimits;
    private final PasswordPolicy passwords;
    private final PasswordEncoder passwordEncoder;
    private final SecureTokenService secureTokens;
    private final MailGateway mail;
    private final Clock clock;
    public RegistrationService(JpaUserRepository users, JpaEmailVerificationTokenRepository verificationTokens,
            UserDefaultsService defaults, RateLimitService rateLimits, PasswordPolicy passwords, PasswordEncoder passwordEncoder,
            SecureTokenService secureTokens, MailGateway mail, Clock clock) {
        this.users = users; this.verificationTokens = verificationTokens; this.defaults = defaults; this.rateLimits = rateLimits;
        this.passwords = passwords; this.passwordEncoder = passwordEncoder; this.secureTokens = secureTokens; this.mail = mail; this.clock = clock;
    }
    @Transactional
    public RegistrationResult register(RegisterCommand command) {
        String email = normalizeEmail(command.email());
        rateLimits.check("register-email", email, 5, Duration.ofHours(1), Duration.ofHours(1));
        rateLimits.check("register-ip", command.clientIp(), 5, Duration.ofHours(1), Duration.ofHours(1));
        if (users.findByEmail(email).isPresent()) throw new IllegalArgumentException("EMAIL_ALREADY_REGISTERED");
        passwords.validate(command.password());
        Instant now = clock.instant();
        User user = users.saveAndFlush(new User(email, passwordEncoder.encode(command.password()), normalizeDisplayName(command.displayName()), now));
        defaults.createFor(user, command.timeZone(), now);
        IssuedToken issued = secureTokens.issue(Duration.ofHours(24));
        verificationTokens.save(new EmailVerificationToken(user, issued.sha256(), issued.expiresAt(), now));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { mail.sendVerificationEmail(email, issued.rawValue()); }
                catch (MailException exception) { log.warn("verification_delivery_failed"); }
            }
        });
        return new RegistrationResult(user.id(), email);
    }
    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) throw new IllegalArgumentException("INVALID_EMAIL");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !normalized.contains("@")) throw new IllegalArgumentException("INVALID_EMAIL");
        return normalized;
    }
    private String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty() || displayName.trim().length() > 80) throw new IllegalArgumentException("INVALID_DISPLAY_NAME");
        return displayName.trim();
    }
}
