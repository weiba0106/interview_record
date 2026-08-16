package com.interviewrecord.tracking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Management view of the job_types table seeded by UserDefaultsService.
 */
@Entity
@Table(name = "job_types")
public class JobType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 40) private String name;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected JobType() {}

    public JobType(Long userId, String name, Instant now) {
        this.userId = userId; this.name = name; this.active = true;
        this.createdAt = now; this.updatedAt = now;
    }

    public Long id() { return id; }
    public Long userId() { return userId; }
    public String name() { return name; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public void rename(String name, Instant now) {
        this.name = name; this.updatedAt = now;
    }

    public void setActive(boolean active, Instant now) {
        this.active = active; this.updatedAt = now;
    }
}
