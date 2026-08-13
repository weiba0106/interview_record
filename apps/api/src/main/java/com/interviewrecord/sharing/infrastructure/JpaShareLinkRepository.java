package com.interviewrecord.sharing.infrastructure;

import com.interviewrecord.sharing.domain.ShareLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaShareLinkRepository extends JpaRepository<ShareLink, Long> {
    Optional<ShareLink> findByTokenHash(byte[] tokenHash);
    Optional<ShareLink> findByIdAndUserId(Long id, Long userId);
    List<ShareLink> findAllByUserIdAndPositionIdOrderByCreatedAtDesc(Long userId, Long positionId);
}
