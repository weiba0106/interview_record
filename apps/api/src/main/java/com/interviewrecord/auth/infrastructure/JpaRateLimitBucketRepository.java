package com.interviewrecord.auth.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(JdbcTemplate.class)
public class JpaRateLimitBucketRepository {
    private final JdbcTemplate jdbc;
    public JpaRateLimitBucketRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void insertIfMissing(String action, byte[] subjectHash, Instant now) {
        jdbc.update("INSERT IGNORE INTO rate_limit_buckets (action_name, subject_hash, window_started_at, attempt_count, blocked_until, updated_at) VALUES (?, ?, ?, 0, NULL, ?)", action, subjectHash, Timestamp.from(now), Timestamp.from(now));
    }
    public Optional<Bucket> lock(String action, byte[] subjectHash) {
        return jdbc.query("SELECT window_started_at, attempt_count, blocked_until FROM rate_limit_buckets WHERE action_name = ? AND subject_hash = ? FOR UPDATE",
                result -> result.next() ? Optional.of(new Bucket(result.getTimestamp(1).toInstant(), result.getInt(2),
                        result.getTimestamp(3) == null ? null : result.getTimestamp(3).toInstant())) : Optional.empty(), action, subjectHash);
    }
    public void update(String action, byte[] subjectHash, Instant windowStartedAt, int attemptCount, Instant blockedUntil, Instant now) {
        jdbc.update("UPDATE rate_limit_buckets SET window_started_at = ?, attempt_count = ?, blocked_until = ?, updated_at = ? WHERE action_name = ? AND subject_hash = ?",
                Timestamp.from(windowStartedAt), attemptCount, blockedUntil == null ? null : Timestamp.from(blockedUntil), Timestamp.from(now), action, subjectHash);
    }
    public void delete(String action, byte[] subjectHash) { jdbc.update("DELETE FROM rate_limit_buckets WHERE action_name = ? AND subject_hash = ?", action, subjectHash); }
    public record Bucket(Instant windowStartedAt, int attemptCount, Instant blockedUntil) {}
}
