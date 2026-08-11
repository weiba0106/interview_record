package com.interviewrecord.common.token;

import java.time.Instant;

public record IssuedToken(String rawValue, byte[] sha256, Instant expiresAt) {
    public IssuedToken {
        sha256 = sha256.clone();
    }

    @Override
    public byte[] sha256() {
        return sha256.clone();
    }
}
