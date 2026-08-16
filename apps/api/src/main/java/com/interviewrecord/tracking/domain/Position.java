package com.interviewrecord.tracking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "positions")
public class Position {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "company_id", nullable = false) private Long companyId;
    @Column(name = "job_type_id", nullable = false) private Long jobTypeId;
    @Column(name = "status_id", nullable = false) private Long statusId;
    @Column(nullable = false, length = 100) private String title;
    @Column(name = "apply_url", length = 2048) private String applyUrl;
    @Column(name = "applied_at") private LocalDate appliedAt;
    @Column(name = "deadline_at") private Instant deadlineAt;
    @Column(name = "work_location", length = 100) private String workLocation;
    @Column(length = 2000) private String description;
    @Column(nullable = false) private boolean archived;
    @Version private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Position() {}

    public Position(Long userId, Long companyId, Long jobTypeId, Long statusId, String title,
            String applyUrl, LocalDate appliedAt, Instant deadlineAt, String workLocation,
            String description, Instant now) {
        this.userId = userId; this.companyId = companyId; this.jobTypeId = jobTypeId;
        this.statusId = statusId; this.title = title; this.applyUrl = applyUrl;
        this.appliedAt = appliedAt; this.deadlineAt = deadlineAt; this.workLocation = workLocation;
        this.description = description; this.archived = false; this.createdAt = now; this.updatedAt = now;
    }

    public Long id() { return id; }
    public Long userId() { return userId; }
    public Long companyId() { return companyId; }
    public Long jobTypeId() { return jobTypeId; }
    public Long statusId() { return statusId; }
    public String title() { return title; }
    public String applyUrl() { return applyUrl; }
    public LocalDate appliedAt() { return appliedAt; }
    public Instant deadlineAt() { return deadlineAt; }
    public String workLocation() { return workLocation; }
    public String description() { return description; }
    public boolean archived() { return archived; }
    public long version() { return version; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public void update(Long companyId, Long jobTypeId, Long statusId, String title, String applyUrl,
            LocalDate appliedAt, Instant deadlineAt, String workLocation, String description, Instant now) {
        this.companyId = companyId; this.jobTypeId = jobTypeId; this.statusId = statusId;
        this.title = title; this.applyUrl = applyUrl; this.appliedAt = appliedAt;
        this.deadlineAt = deadlineAt; this.workLocation = workLocation; this.description = description;
        this.updatedAt = now;
    }

    public void changeStatus(Long statusId, Instant now) {
        this.statusId = statusId; this.updatedAt = now;
    }

    public void setArchived(boolean archived, Instant now) {
        this.archived = archived; this.updatedAt = now;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
