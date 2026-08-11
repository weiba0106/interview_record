package com.interviewrecord.auth.infrastructure;

import com.interviewrecord.auth.domain.EmailVerificationToken;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    List<EmailVerificationToken> findByUserId(long userId);
}
