package com.interviewrecord.defaults.domain;

import com.interviewrecord.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "position_statuses")
public class DefaultPositionStatus {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false) private String name;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false, length = 7) private String color;
    @Column(name = "statistics_category", nullable = false) private String statisticsCategory;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected DefaultPositionStatus() {}
    public DefaultPositionStatus(User user, int sortOrder, String name, String color, String statisticsCategory, Instant now) {
        this.user = user; this.sortOrder = sortOrder; this.name = name; this.color = color;
        this.statisticsCategory = statisticsCategory; this.active = true; this.createdAt = now; this.updatedAt = now;
    }
}
