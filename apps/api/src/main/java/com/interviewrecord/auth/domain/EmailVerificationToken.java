package com.interviewrecord.auth.domain;

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
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "token_hash", nullable = false, columnDefinition = "BINARY(32)") private byte[] tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_at") private Instant consumedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected EmailVerificationToken() {}
    public EmailVerificationToken(User user, byte[] tokenHash, Instant expiresAt, Instant createdAt) {
        this.user = user; this.tokenHash = tokenHash.clone(); this.expiresAt = expiresAt; this.createdAt = createdAt;
    }
    public User user() { return user; }
    public boolean isUsableAt(Instant now) { return consumedAt == null && expiresAt.isAfter(now); }
    public void consume(Instant now) { if (consumedAt == null) consumedAt = now; }
}
