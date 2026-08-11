package com.interviewrecord.auth.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.auth.infrastructure.SpringSessionRevoker;
import com.interviewrecord.common.error.InvalidRegistrationException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AccountDeletionServiceTest {
    private final JpaUserRepository users = org.mockito.Mockito.mock(JpaUserRepository.class);
    private final PasswordEncoder passwords = org.mockito.Mockito.mock(PasswordEncoder.class);
    private final SpringSessionRevoker sessions = org.mockito.Mockito.mock(SpringSessionRevoker.class);
    private final AccountDeletionService service = new AccountDeletionService(users, passwords, sessions);

    @Test
    void deletionRevokesSessionsBeforeTheUserCascade() {
        User user = org.mockito.Mockito.mock(User.class);
        given(users.requireById(42L)).willReturn(user);
        given(user.passwordHash()).willReturn("hash");
        given(user.email()).willReturn(" Alice@example.com ");
        given(passwords.matches("Password123", "hash")).willReturn(true);
        service.deleteCurrentUser(42L, "Password123");

        verify(sessions).revokeAllForPrincipal("alice@example.com");
        verify(users).delete(user);
        verify(users).flush();
    }

    @Test
    void wrongPasswordDoesNotDeleteTheUserOrRevokeSessions() {
        User user = org.mockito.Mockito.mock(User.class);
        given(users.requireById(42L)).willReturn(user);
        given(user.passwordHash()).willReturn("hash");
        given(passwords.matches("wrong", "hash")).willReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.deleteCurrentUser(42L, "wrong"))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessage("INVALID_PASSWORD");
        verify(users, never()).delete(user);
        verify(sessions, never()).revokeAllForPrincipal(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void revocationFailureLeavesTheAccountUntouched() {
        User user = org.mockito.Mockito.mock(User.class);
        given(users.requireById(42L)).willReturn(user);
        given(user.passwordHash()).willReturn("hash");
        given(user.email()).willReturn("alice@example.com");
        given(passwords.matches("Password123", "hash")).willReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("session store unavailable"))
                .when(sessions).revokeAllForPrincipal("alice@example.com");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.deleteCurrentUser(42L, "Password123"))
                .isInstanceOf(IllegalStateException.class);

        verify(users, never()).delete(user);
        verify(users, never()).flush();
    }
}
