package com.interviewrecord.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Set;

@Entity
@Table(name = "schedule_events")
public class ScheduleEvent {
    public static final Set<String> EVENT_TYPES = Set.of(
            "INTERVIEW", "WRITTEN_TEST", "HR_COMMUNICATION", "APPLY_DEADLINE", "OFFER_DEADLINE", "CUSTOM");
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 120) private String title;
    @Column(name = "event_type", nullable = false, length = 20) private String eventType;
    @Column(name = "starts_at") private Instant startsAt;
    @Column(name = "ends_at") private Instant endsAt;
    @Column(name = "position_id") private Long positionId;
    @Column(name = "interview_round_id") private Long interviewRoundId;
    @Column(length = 500) private String location;
    @Column(length = 2000) private String notes;
    @Column(nullable = false, length = 16) private String status;
    @Column(name = "manual_urgency", length = 16) private String manualUrgency;
    @Version private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected ScheduleEvent() {}

    public ScheduleEvent(Long userId, String title, String eventType, Instant startsAt, Instant endsAt,
            Long positionId, Long interviewRoundId, String location, String notes, Instant now) {
        this.userId = userId; this.title = title; this.eventType = eventType;
        this.startsAt = startsAt; this.endsAt = endsAt; this.positionId = positionId;
        this.interviewRoundId = interviewRoundId; this.location = location; this.notes = notes;
        this.status = STATUS_PENDING; this.createdAt = now; this.updatedAt = now;
    }

    public Long id() { return id; }
    public Long userId() { return userId; }
    public String title() { return title; }
    public String eventType() { return eventType; }
    public Instant startsAt() { return startsAt; }
    public Instant endsAt() { return endsAt; }
    public Long positionId() { return positionId; }
    public Long interviewRoundId() { return interviewRoundId; }
    public String location() { return location; }
    public String notes() { return notes; }
    public String status() { return status; }
    public String manualUrgency() { return manualUrgency; }
    public long version() { return version; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    /** Reference instant used for urgency: start time, falling back to the deadline. */
    public Instant referenceTime() {
        return startsAt != null ? startsAt : endsAt;
    }

    public boolean pending() {
        return STATUS_PENDING.equals(status);
    }

    public Urgency urgency(Instant now) {
        return Urgency.of(this, now);
    }

    public void update(String title, String eventType, Instant startsAt, Instant endsAt, Long positionId,
            Long interviewRoundId, String location, String notes, Instant now) {
        this.title = title; this.eventType = eventType; this.startsAt = startsAt; this.endsAt = endsAt;
        this.positionId = positionId; this.interviewRoundId = interviewRoundId;
        this.location = location; this.notes = notes; this.updatedAt = now;
    }

    public void changeStatus(String status, Instant now) {
        this.status = status; this.updatedAt = now;
    }

    /** Pass null to clear the manual override and restore automatic urgency. */
    public void overrideUrgency(String manualUrgency, Instant now) {
        this.manualUrgency = manualUrgency; this.updatedAt = now;
    }

    public void reschedule(Instant startsAt, Instant endsAt, Instant now) {
        this.startsAt = startsAt; this.endsAt = endsAt; this.updatedAt = now;
    }
}
