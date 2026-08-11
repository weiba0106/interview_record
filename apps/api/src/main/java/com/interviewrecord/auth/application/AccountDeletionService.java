package com.interviewrecord.auth.application;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.auth.infrastructure.SpringSessionRevoker;
import com.interviewrecord.common.error.InvalidRegistrationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class AccountDeletionService {
    private final JpaUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SpringSessionRevoker sessionRevoker;

    public AccountDeletionService(JpaUserRepository users, PasswordEncoder passwordEncoder, SpringSessionRevoker sessionRevoker) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessionRevoker = sessionRevoker;
    }

    @Transactional
    public void deleteCurrentUser(long userId, String password) {
        User user = users.requireById(userId);
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new InvalidRegistrationException("INVALID_PASSWORD");
        }
        String email = user.email().trim().toLowerCase(java.util.Locale.ROOT);
        // Revocation must succeed before the irreversible account cascade. If the
        // session store is unavailable, leave the account intact rather than
        // deleting it while other authenticated sessions remain usable.
        sessionRevoker.revokeAllForPrincipal(email);
        users.delete(user);
        users.flush();
    }
}
