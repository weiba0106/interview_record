package com.interviewrecord.common.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public final class SecureTokenService {
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public SecureTokenService(Clock clock) {
        this.clock = clock;
    }

    public IssuedToken issue(Duration lifetime) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new IssuedToken(raw, sha256(raw), clock.instant().plus(lifetime));
    }

    public byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public boolean hashesMatch(byte[] left, byte[] right) {
        return MessageDigest.isEqual(left, right);
    }
}
