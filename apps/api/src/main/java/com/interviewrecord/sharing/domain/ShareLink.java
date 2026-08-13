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
@Table(name = "share_links")
public class ShareLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "position_id", nullable = false) private Long positionId;
    @Column(name = "token_hash", nullable = false, length = 32) private byte[] tokenHash;
    @Column(name = "position_fields", nullable = false, length = 255) private String positionFieldsValue;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected ShareLink() {}

    public ShareLink(Long userId, Long positionId, byte[] tokenHash, Set<String> positionFields,
            Instant expiresAt, Instant createdAt) {
        this.userId = userId;
        this.positionId = positionId;
        this.tokenHash = tokenHash.clone();
        this.positionFieldsValue = encode(positionFields);
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public Long id() { return id; }
    public Long userId() { return userId; }
    public Long positionId() { return positionId; }
    public byte[] tokenHash() { return tokenHash.clone(); }
    public Set<String> positionFields() { return decode(positionFieldsValue); }
    public Instant expiresAt() { return expiresAt; }
    public Instant revokedAt() { return revokedAt; }
    public Instant createdAt() { return createdAt; }
    public void revoke(Instant now) { if (revokedAt == null) revokedAt = now; }
    public boolean isActiveAt(Instant now) {
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(now));
    }

    private static String encode(Set<String> fields) {
        return String.join(",", new java.util.TreeSet<>(fields));
    }
    private static Set<String> decode(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return new LinkedHashSet<>(Arrays.asList(value.split(",")));
    }
}
