package com.interviewrecord.interviews.infrastructure;

import com.interviewrecord.interviews.domain.InterviewRound;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInterviewRoundRepository extends JpaRepository<InterviewRound, Long> {
    Optional<InterviewRound> findByIdAndUserId(Long id, Long userId);

    List<InterviewRound> findAllByUserId(Long userId);

    /** 题库聚合用的批量轮次查询。 */
    List<InterviewRound> findAllByUserIdAndIdIn(Long userId, Collection<Long> ids);

    @org.springframework.data.jpa.repository.Query("""
            SELECT r FROM InterviewRound r WHERE r.userId = :userId AND r.positionId = :positionId
            ORDER BY CASE WHEN r.startsAt IS NULL THEN 1 ELSE 0 END ASC, r.startsAt ASC, r.roundNumber ASC
            """)
    List<InterviewRound> findAllByUserIdAndPositionIdInDisplayOrder(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("positionId") Long positionId);

    boolean existsByUserIdAndPositionIdAndRoundNumber(Long userId, Long positionId, int roundNumber);

    long countByUserIdAndPositionId(Long userId, Long positionId);

    long countByUserIdAndPositionIdIn(Long userId, Collection<Long> positionIds);

    @org.springframework.data.jpa.repository.Query(
            "SELECT r.positionId, COUNT(r) FROM InterviewRound r WHERE r.userId = :userId"
            + " AND r.positionId IN :positionIds GROUP BY r.positionId")
    List<Object[]> countByUserIdGroupedByPosition(@org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("positionIds") Collection<Long> positionIds);

    List<InterviewRound> findAllByUserIdAndPositionId(Long userId, Long positionId);
}
