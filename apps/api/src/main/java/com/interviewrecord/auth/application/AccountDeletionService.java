package com.interviewrecord.auth.application;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import com.interviewrecord.auth.infrastructure.SpringSessionRevoker;
import com.interviewrecord.common.error.InvalidRegistrationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class AccountDeletionService {
    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);
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
        users.delete(user);
        users.flush();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    sessionRevoker.revokeAllForPrincipal(email);
                } catch (RuntimeException exception) {
                    log.atWarn().addKeyValue("error_code", "SESSION_REVOCATION_FAILED")
                            .log("session_revocation_failed");
                }
            }
        });
    }
}
