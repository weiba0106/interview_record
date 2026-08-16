package com.interviewrecord.tracking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;

/**
 * Management view of the position_statuses table seeded by UserDefaultsService.
 */
@Entity
@Table(name = "position_statuses")
public class PositionStatus {
    public static final Set<String> STATISTICS_CATEGORIES = Set.of("ACTIVE", "SUCCESS", "REJECTED", "WITHDRAWN");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 40) private String name;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false, length = 7) private String color;
    @Column(name = "statistics_category", nullable = false, length = 16) private String statisticsCategory;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected PositionStatus() {}

    public PositionStatus(Long userId, String name, int sortOrder, String color, String statisticsCategory, Instant now) {
        this.userId = userId; this.name = name; this.sortOrder = sortOrder; this.color = color;
        this.statisticsCategory = statisticsCategory; this.active = true;
        this.createdAt = now; this.updatedAt = now;
    }

    public Long id() { return id; }
    public Long userId() { return userId; }
    public String name() { return name; }
    public int sortOrder() { return sortOrder; }
    public String color() { return color; }
    public String statisticsCategory() { return statisticsCategory; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public void update(String name, String color, String statisticsCategory, boolean active, Instant now) {
        this.name = name; this.color = color; this.statisticsCategory = statisticsCategory;
        this.active = active; this.updatedAt = now;
    }

    public void assignOrder(int sortOrder, Instant now) {
        this.sortOrder = sortOrder; this.updatedAt = now;
    }
}
