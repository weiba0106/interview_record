package com.interviewrecord.auth.application;

import com.interviewrecord.auth.infrastructure.JpaRateLimitBucketRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class RateLimitBucketWriter {
    private final JpaRateLimitBucketRepository buckets;
    private final Clock clock;
    public RateLimitBucketWriter(JpaRateLimitBucketRepository buckets, Clock clock) { this.buckets = buckets; this.clock = clock; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Duration recordAttempt(String action, byte[] subjectHash, int limit, Duration window, Duration blockDuration) {
        Instant now = clock.instant();
        buckets.insertIfMissing(action, subjectHash, now);
        JpaRateLimitBucketRepository.Bucket bucket = buckets.lock(action, subjectHash).orElseThrow();
        if (bucket.blockedUntil() != null && bucket.blockedUntil().isAfter(now)) return Duration.between(now, bucket.blockedUntil());
        boolean expired = !bucket.windowStartedAt().plus(window).isAfter(now);
        int attempts = expired ? 1 : bucket.attemptCount() + 1;
        Instant blockedUntil = attempts > limit ? now.plus(blockDuration) : null;
        buckets.update(action, subjectHash, expired ? now : bucket.windowStartedAt(), attempts, blockedUntil, now);
        return blockedUntil == null ? null : blockDuration;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reset(String action, byte[] subjectHash) { buckets.delete(action, subjectHash); }
}
