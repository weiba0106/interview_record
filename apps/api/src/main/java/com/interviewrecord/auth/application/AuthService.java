package com.interviewrecord.auth.application;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.common.security.AuthenticatedUser;
import com.interviewrecord.common.token.SecureTokenService;
import com.interviewrecord.preference.domain.UserPreference;
import com.interviewrecord.preference.infrastructure.JpaUserPreferenceRepository;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class AuthService {
    private static final int LOGIN_LIMIT = 10;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);
    private final AuthenticationManager authenticationManager;
    private final JpaUserRepository users;
    private final JpaUserPreferenceRepository preferences;
    private final RateLimitService rateLimits;
    private final SecureTokenService tokens;

    public AuthService(AuthenticationManager authenticationManager, JpaUserRepository users,
            JpaUserPreferenceRepository preferences, RateLimitService rateLimits, SecureTokenService tokens) {
        this.authenticationManager = authenticationManager;
        this.users = users;
        this.preferences = preferences;
        this.rateLimits = rateLimits;
        this.tokens = tokens;
    }

    public AuthenticatedUser login(String rawEmail, String password, String clientIp) {
        String email = normalizeEmail(rawEmail);
        String subject = loginSubject(email, clientIp);
        rateLimits.check("login-email-ip", subject, LOGIN_LIMIT, LOGIN_WINDOW, LOGIN_WINDOW);
        try {
            authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        } catch (BadCredentialsException exception) {
            throw new InvalidCredentialsException();
        }

        User user = users.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (!user.isVerified()) {
            throw new EmailNotVerifiedException();
        }
        UserPreference preference = preferences.requireByUserId(user.id());
        rateLimits.reset("login-email-ip", subject);
        return new AuthenticatedUser(user.id(), user.email(), user.displayName(), true,
                preference.timeZone(), preference.theme());
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String loginSubject(String email, String clientIp) {
        return hashSubjectPart(email) + ":" + hashSubjectPart(canonicalIp(clientIp));
    }

    private String hashSubjectPart(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokens.sha256(value));
    }

    private String canonicalIp(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        try {
            return InetAddress.getByName(trimmed).getHostAddress();
        } catch (Exception ignored) {
            return trimmed;
        }
    }

    public static final class InvalidCredentialsException extends RuntimeException {
    }

    public static final class EmailNotVerifiedException extends RuntimeException {
    }
}
