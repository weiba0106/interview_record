package com.interviewrecord.export.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

/** 一次性导出文件：令牌只存摘要，下载后失效，30 分钟过期。 */
@Entity
@Table(name = "export_files")
public class ExportFile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "token_hash", nullable = false, columnDefinition = "BINARY(32)") private byte[] tokenHash;
    @Column(name = "file_name", nullable = false, length = 120) private String fileName;
    @Column(name = "content_type", nullable = false, length = 64) private String contentType;
    @Lob @Column(nullable = false) private byte[] payload;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "downloaded_at") private Instant downloadedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected ExportFile() {}

    public ExportFile(Long userId, byte[] tokenHash, String fileName, String contentType,
            byte[] payload, Instant expiresAt, Instant createdAt) {
        this.userId = userId; this.tokenHash = tokenHash; this.fileName = fileName;
        this.contentType = contentType; this.payload = payload; this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public Long id() { return id; }
    public Long userId() { return userId; }
    public byte[] tokenHash() { return tokenHash; }
    public String fileName() { return fileName; }
    public String contentType() { return contentType; }
    public byte[] payload() { return payload; }
    public Instant expiresAt() { return expiresAt; }
    public Instant downloadedAt() { return downloadedAt; }
    public Instant createdAt() { return createdAt; }
}
