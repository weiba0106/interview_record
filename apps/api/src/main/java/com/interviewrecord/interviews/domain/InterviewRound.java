package com.interviewrecord.interviews.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.sql.Types;
import java.time.Instant;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "interview_rounds")
public class InterviewRound {
    public static final Set<String> TYPES = Set.of("PHONE", "VIDEO", "ONSITE", "WRITTEN_TEST", "OTHER");
    public static final Set<String> RESULTS = Set.of("UPCOMING", "AWAITING_RESULT", "PASSED", "FAILED", "CANCELLED");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "position_id", nullable = false) private Long positionId;
    @Column(name = "round_name", nullable = false, length = 80) private String roundName;
    @Column(name = "round_number", nullable = false) private int roundNumber;
    @Column(name = "interview_type", nullable = false, length = 16) private String interviewType;
    @Column(name = "starts_at") private Instant startsAt;
    @Column(name = "ends_at") private Instant endsAt;
    @Column(length = 500) private String location;
    @Column(nullable = false, length = 20) private String result;
    /** 富文本字段（服务端白名单清洗后存储），V9 起为 LONGTEXT。 */
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "process_notes") private String processNotes;
    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "review_summary") private String reviewSummary;
    @Version private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected InterviewRound() {}

    public InterviewRound(Long userId, Long positionId, String roundName, int roundNumber, String interviewType,
            Instant startsAt, Instant endsAt, String location, String result,
            String processNotes, String reviewSummary, Instant now) {
        this.userId = userId; this.positionId = positionId; this.roundName = roundName;
        this.roundNumber = roundNumber; this.interviewType = interviewType;
        this.startsAt = startsAt; this.endsAt = endsAt; this.location = location;
        this.result = result; this.processNotes = processNotes; this.reviewSummary = reviewSummary;
        this.createdAt = now; this.updatedAt = now;
    }

    public Long id() { return id; }
    public Long userId() { return userId; }
    public Long positionId() { return positionId; }
    public String roundName() { return roundName; }
    public int roundNumber() { return roundNumber; }
    public String interviewType() { return interviewType; }
    public Instant startsAt() { return startsAt; }
    public Instant endsAt() { return endsAt; }
    public String location() { return location; }
    public String result() { return result; }
    public String processNotes() { return processNotes; }
    public String reviewSummary() { return reviewSummary; }
    public long version() { return version; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public void update(String roundName, int roundNumber, String interviewType, Instant startsAt, Instant endsAt,
            String location, String result, String processNotes, String reviewSummary, Instant now) {
        this.roundName = roundName; this.roundNumber = roundNumber; this.interviewType = interviewType;
        this.startsAt = startsAt; this.endsAt = endsAt; this.location = location; this.result = result;
        this.processNotes = processNotes; this.reviewSummary = reviewSummary; this.updatedAt = now;
    }

    /** Keeps an explicitly linked schedule authoritative when its time is edited. */
    public void reschedule(Instant startsAt, Instant endsAt, Instant now) {
        this.startsAt = startsAt; this.endsAt = endsAt; this.updatedAt = now;
    }
}
