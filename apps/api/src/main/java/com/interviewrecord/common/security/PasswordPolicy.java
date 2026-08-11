package com.interviewrecord.common.security;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public final class PasswordPolicy {
    public void validate(String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || !password.codePoints().anyMatch(Character::isLetter)
                || !password.codePoints().anyMatch(Character::isDigit)) {
            throw new IllegalArgumentException("INVALID_PASSWORD");
        }
    }
}
