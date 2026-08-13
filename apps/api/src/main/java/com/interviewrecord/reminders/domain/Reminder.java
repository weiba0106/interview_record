package com.interviewrecord.reminders.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "reminders")
public class Reminder {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    private static final int MAX_ATTEMPTS = 3;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "schedule_id", nullable = false) private Long scheduleId;
    @Column(name = "idempotency_key", nullable = false, length = 180) private String idempotencyKey;
    @Column(name = "scheduled_at", nullable = false) private Instant scheduledAt;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "last_error_code", length = 80) private String lastErrorCode;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Reminder() { }

    public Reminder(Long userId, Long scheduleId, String idempotencyKey, Instant scheduledAt, Instant now) {
        this.userId = userId; this.scheduleId = scheduleId; this.idempotencyKey = idempotencyKey;
        this.scheduledAt = scheduledAt; this.status = STATUS_PENDING; this.nextAttemptAt = scheduledAt;
        this.createdAt = now; this.updatedAt = now;
    }

    public Long id() { return id; }
    public Long userId() { return userId; }
    public Long scheduleId() { return scheduleId; }
    public String idempotencyKey() { return idempotencyKey; }
    public Instant scheduledAt() { return scheduledAt; }
    public String status() { return status; }
    public int attemptCount() { return attemptCount; }
    public Instant nextAttemptAt() { return nextAttemptAt; }
    public Instant sentAt() { return sentAt; }

    public void markSent(Instant now) {
        status = STATUS_SENT; sentAt = now; nextAttemptAt = now; lastErrorCode = null; updatedAt = now;
    }

    public void markDeliveryFailure(Instant now) {
        attemptCount++;
        lastErrorCode = "MAIL_DELIVERY_FAILED";
        if (attemptCount >= MAX_ATTEMPTS) {
            status = STATUS_FAILED;
            nextAttemptAt = now;
        } else {
            status = STATUS_PENDING;
            nextAttemptAt = now.plus(Duration.ofMinutes(attemptCount));
        }
        updatedAt = now;
    }

    public void cancel(Instant now) {
        if (!STATUS_SENT.equals(status)) {
            status = STATUS_CANCELLED; updatedAt = now;
        }
    }
}
