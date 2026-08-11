package com.interviewrecord.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 254) private String email;
    @Column(name = "password_hash", nullable = false) private String passwordHash;
    @Column(name = "display_name", nullable = false, length = 80) private String displayName;
    @Column(name = "email_verified_at") private Instant emailVerifiedAt;
    @Version private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected User() {}
    public User(String email, String passwordHash, String displayName, Instant now) {
        this.email = email; this.passwordHash = passwordHash; this.displayName = displayName;
        this.createdAt = now; this.updatedAt = now;
    }
    public Long id() { return id; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public String displayName() { return displayName; }
    public boolean isVerified() { return emailVerifiedAt != null; }
    public void changePassword(String passwordHash, Instant now) {
        this.passwordHash = passwordHash;
        this.updatedAt = now;
    }
    public void verify(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
            updatedAt = now;
        }
    }
}
