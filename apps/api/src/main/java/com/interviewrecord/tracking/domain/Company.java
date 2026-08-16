package com.interviewrecord.tracking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "companies")
public class Company {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 2048) private String website;
    @Column(length = 2000) private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Company() {}

    public Company(Long userId, String name, String website, String notes, Instant now) {
        this.userId = userId; this.name = name; this.website = website; this.notes = notes;
        this.createdAt = now; this.updatedAt = now;
    }

    public Long id() { return id; }
    public Long userId() { return userId; }
    public String name() { return name; }
    public String website() { return website; }
    public String notes() { return notes; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public void update(String name, String website, String notes, Instant now) {
        this.name = name; this.website = website; this.notes = notes; this.updatedAt = now;
    }
}
