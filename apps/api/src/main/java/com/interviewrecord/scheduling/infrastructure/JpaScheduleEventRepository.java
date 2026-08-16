package com.interviewrecord.scheduling.infrastructure;

import com.interviewrecord.scheduling.domain.ScheduleEvent;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaScheduleEventRepository extends JpaRepository<ScheduleEvent, Long> {
    Optional<ScheduleEvent> findByIdAndUserId(Long id, Long userId);

    List<ScheduleEvent> findAllByUserId(Long userId);

    List<ScheduleEvent> findAllByUserIdAndStatus(Long userId, String status);

    /** 按下次日程排序需要的最早待处理时间：包括已逾期日程。 */
    @Query("""
            SELECT s FROM ScheduleEvent s WHERE s.userId = :userId AND s.status = 'PENDING'
            AND s.positionId IN :positionIds
            AND COALESCE(s.startsAt, s.endsAt) IS NOT NULL
            ORDER BY COALESCE(s.startsAt, s.endsAt) ASC
            """)
    List<ScheduleEvent> findPendingForPositions(@Param("userId") Long userId,
            @Param("positionIds") Collection<Long> positionIds);

    long countByUserIdAndPositionId(Long userId, Long positionId);

    long countByUserIdAndPositionIdIn(Long userId, Collection<Long> positionIds);

    @Query("SELECT s.positionId, COUNT(s) FROM ScheduleEvent s WHERE s.userId = :userId"
            + " AND s.positionId IN :positionIds GROUP BY s.positionId")
    List<Object[]> countByUserIdGroupedByPosition(@Param("userId") Long userId,
            @Param("positionIds") Collection<Long> positionIds);

    long countByUserIdAndInterviewRoundId(Long userId, Long interviewRoundId);

    @Query("""
            SELECT s FROM ScheduleEvent s WHERE s.userId = :userId
            AND COALESCE(s.startsAt, s.endsAt) IS NOT NULL
            AND COALESCE(s.startsAt, s.endsAt) <= :until
            ORDER BY COALESCE(s.startsAt, s.endsAt) ASC
            """)
    List<ScheduleEvent> findAllByUserIdWithReferenceUpTo(@Param("userId") Long userId, @Param("until") Instant until);

    @Query("""
            SELECT s FROM ScheduleEvent s WHERE s.userId = :userId AND s.status = 'PENDING'
            AND s.positionId IN :positionIds
            AND COALESCE(s.startsAt, s.endsAt) IS NOT NULL
            AND COALESCE(s.startsAt, s.endsAt) >= :from
            ORDER BY COALESCE(s.startsAt, s.endsAt) ASC
            """)
    List<ScheduleEvent> findPendingForPositionsFrom(@Param("userId") Long userId,
            @Param("positionIds") Collection<Long> positionIds, @Param("from") Instant from);

    @Query("""
            SELECT s FROM ScheduleEvent s WHERE s.userId = :userId AND s.interviewRoundId = :roundId
            """)
    List<ScheduleEvent> findAllByUserIdAndInterviewRoundId(@Param("userId") Long userId, @Param("roundId") Long roundId);
}
