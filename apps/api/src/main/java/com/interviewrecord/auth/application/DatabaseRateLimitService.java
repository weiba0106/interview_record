package com.interviewrecord.auth.application;

import com.interviewrecord.auth.infrastructure.JpaRateLimitBucketRepository;
import com.interviewrecord.common.token.SecureTokenService;
import java.net.InetAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class DatabaseRateLimitService implements RateLimitService {
    private final JpaRateLimitBucketRepository buckets;
    private final SecureTokenService tokens;
    private final Clock clock;
    public DatabaseRateLimitService(JpaRateLimitBucketRepository buckets, SecureTokenService tokens, Clock clock) {
        this.buckets = buckets; this.tokens = tokens; this.clock = clock;
    }
    @Override @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void check(String action, String subject, int limit, Duration window, Duration blockDuration) {
        Instant now = clock.instant(); byte[] hash = tokens.sha256(action + ':' + normalizeSubject(subject));
        buckets.insertIfMissing(action, hash, now);
        JpaRateLimitBucketRepository.Bucket bucket = buckets.lock(action, hash).orElseThrow();
        if (bucket.blockedUntil() != null && bucket.blockedUntil().isAfter(now)) throw new IllegalStateException("RATE_LIMITED");
        boolean expired = !bucket.windowStartedAt().plus(window).isAfter(now);
        int attempts = expired ? 1 : bucket.attemptCount() + 1;
        Instant blockedUntil = attempts > limit ? now.plus(blockDuration) : null;
        buckets.update(action, hash, expired ? now : bucket.windowStartedAt(), attempts, blockedUntil, now);
        if (attempts > limit) throw new IllegalStateException("RATE_LIMITED");
    }
    @Override @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reset(String action, String subject) { buckets.delete(action, tokens.sha256(action + ':' + normalizeSubject(subject))); }
    private String normalizeSubject(String subject) {
        String trimmed = subject.trim();
        if (trimmed.contains("@")) return trimmed.toLowerCase(Locale.ROOT);
        try { return InetAddress.getByName(trimmed).getHostAddress(); } catch (Exception ignored) { return trimmed; }
    }
}
