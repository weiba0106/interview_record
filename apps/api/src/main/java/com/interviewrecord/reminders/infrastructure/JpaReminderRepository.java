package com.interviewrecord.reminders.infrastructure;

import com.interviewrecord.reminders.domain.Reminder;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaReminderRepository extends JpaRepository<Reminder, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("update Reminder r set r.status = 'CANCELLED', r.updatedAt = :now "
            + "where r.userId = :userId and r.scheduleId = :scheduleId and r.status in ('PENDING', 'PROCESSING')")
    int cancelUnsentByUserIdAndScheduleId(@Param("userId") Long userId, @Param("scheduleId") Long scheduleId,
            @Param("now") Instant now);

    @Query("select r.id from Reminder r where r.status = 'PENDING' and r.nextAttemptAt <= :now "
            + "order by r.nextAttemptAt asc")
    List<Long> findReadyIds(@Param("now") Instant now, org.springframework.data.domain.Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Reminder r set r.status = 'PROCESSING', r.updatedAt = :now "
            + "where r.id = :id and r.status = 'PENDING' and r.nextAttemptAt <= :now")
    int claim(@Param("id") Long id, @Param("now") Instant now);
}
