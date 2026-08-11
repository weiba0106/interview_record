package com.interviewrecord.preference.domain;

import com.interviewrecord.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_preferences")
public class UserPreference {
    @Id @Column(name = "user_id") private Long userId;
    @OneToOne(optional = false) @MapsId @JoinColumn(name = "user_id") private User user;
    @Column(name = "time_zone", nullable = false) private String timeZone;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Theme theme;
    @Column(name = "interview_reminder_offsets", nullable = false, columnDefinition = "json") private String interviewReminderOffsets;
    @Column(name = "deadline_reminder_offsets", nullable = false, columnDefinition = "json") private String deadlineReminderOffsets;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected UserPreference() {}
    public UserPreference(User user, String timeZone, Instant now) {
        this.user = user; this.timeZone = timeZone; this.theme = Theme.GRAPHITE_CORAL;
        this.interviewReminderOffsets = "[1440,30]"; this.deadlineReminderOffsets = "[1440]";
        this.createdAt = now; this.updatedAt = now;
    }
    public String timeZone() { return timeZone; }
    public Theme theme() { return theme; }
}
