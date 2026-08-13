package com.interviewrecord.sharing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "share_rounds")
public class ShareRound {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "share_id", nullable = false) private Long shareId;
    @Column(name = "round_id", nullable = false) private Long roundId;
    @Column(name = "visible_fields", nullable = false, length = 255) private String visibleFieldsValue;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected ShareRound() {}

    public ShareRound(Long shareId, Long roundId, Set<String> visibleFields, Instant createdAt) {
        this.shareId = shareId;
        this.roundId = roundId;
        this.visibleFieldsValue = String.join(",", new java.util.TreeSet<>(visibleFields));
        this.createdAt = createdAt;
    }

    public Long id() { return id; }
    public Long shareId() { return shareId; }
    public Long roundId() { return roundId; }
    public Set<String> visibleFields() {
        if (visibleFieldsValue == null || visibleFieldsValue.isBlank()) return Set.of();
        return new LinkedHashSet<>(Arrays.asList(visibleFieldsValue.split(",")));
    }
    public Instant createdAt() { return createdAt; }
}
