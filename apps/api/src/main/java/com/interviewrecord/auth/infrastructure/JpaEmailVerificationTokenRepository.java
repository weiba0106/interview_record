package com.interviewrecord.auth.infrastructure;

import com.interviewrecord.auth.domain.EmailVerificationToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

public interface JpaEmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    List<EmailVerificationToken> findByUserId(long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from EmailVerificationToken token where token.tokenHash = :tokenHash")
    Optional<EmailVerificationToken> findByTokenHashForUpdate(@Param("tokenHash") byte[] tokenHash);
}
