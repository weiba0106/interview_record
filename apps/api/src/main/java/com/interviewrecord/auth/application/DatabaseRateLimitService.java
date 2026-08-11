package com.interviewrecord.auth.application;

import com.interviewrecord.common.error.RateLimitExceededException;
import com.interviewrecord.common.token.SecureTokenService;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class DatabaseRateLimitService implements RateLimitService {
    private final SecureTokenService tokens;
    private final RateLimitBucketWriter writer;
    public DatabaseRateLimitService(SecureTokenService tokens, RateLimitBucketWriter writer) {
        this.tokens = tokens; this.writer = writer;
    }
    @Override
    public void check(String action, String subject, int limit, Duration window, Duration blockDuration) {
        Duration retryAfter = writer.recordAttempt(action, subjectHash(action, subject), limit, window, blockDuration);
        if (retryAfter != null) throw new RateLimitExceededException(retryAfter);
    }
    @Override
    public void reset(String action, String subject) { writer.reset(action, subjectHash(action, subject)); }
    private byte[] subjectHash(String action, String subject) { return tokens.sha256(action + ':' + normalizeSubject(subject)); }
    private String normalizeSubject(String subject) {
        String trimmed = subject.trim();
        if (trimmed.contains("@")) return trimmed.toLowerCase(Locale.ROOT);
        try { return InetAddress.getByName(trimmed).getHostAddress(); } catch (Exception ignored) { return trimmed; }
    }
}
