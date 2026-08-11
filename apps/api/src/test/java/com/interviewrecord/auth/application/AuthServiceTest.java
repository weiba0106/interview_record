package com.interviewrecord.auth.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.preference.domain.Theme;
import com.interviewrecord.preference.domain.UserPreference;
import com.interviewrecord.preference.infrastructure.JpaUserPreferenceRepository;
import com.interviewrecord.common.token.SecureTokenService;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class AuthServiceTest {
    @Test
    void loginLimiterSubjectDoesNotPersistRawEmailOrIp() {
        AuthenticationManager authenticationManager = authentication -> authentication;
        JpaUserRepository users = org.mockito.Mockito.mock(JpaUserRepository.class);
        JpaUserPreferenceRepository preferences = org.mockito.Mockito.mock(JpaUserPreferenceRepository.class);
        RateLimitService rateLimits = org.mockito.Mockito.mock(RateLimitService.class);
        User user = org.mockito.Mockito.mock(User.class);
        UserPreference preference = org.mockito.Mockito.mock(UserPreference.class);
        given(users.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(user.id()).willReturn(42L);
        given(user.email()).willReturn("user@example.com");
        given(user.displayName()).willReturn("小林");
        given(user.isVerified()).willReturn(true);
        given(preferences.requireByUserId(42L)).willReturn(preference);
        given(preference.timeZone()).willReturn("Asia/Shanghai");
        given(preference.theme()).willReturn(Theme.GRAPHITE_CORAL);

        new AuthService(authenticationManager, users, preferences, rateLimits, new SecureTokenService(Clock.systemUTC()))
                .login(" USER@example.com ", "Password123", "2001:db8::1");

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(rateLimits).check(eq("login-email-ip"), subject.capture(), eq(10), any(), any());
        org.assertj.core.api.Assertions.assertThat(subject.getValue())
                .doesNotContain("user@example.com")
                .doesNotContain("2001:db8::1")
                .contains(":");
    }
}
