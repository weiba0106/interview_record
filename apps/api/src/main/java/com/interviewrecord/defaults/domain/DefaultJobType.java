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
@Table(name = "job_types")
public class DefaultJobType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected DefaultJobType() {}
    public DefaultJobType(User user, String name, Instant now) {
        this.user = user; this.name = name; this.active = true; this.createdAt = now; this.updatedAt = now;
    }
}
