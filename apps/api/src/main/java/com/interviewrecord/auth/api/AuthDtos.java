package com.interviewrecord.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}
    public record RegisterRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 80) String displayName,
            @Size(max = 64) String timeZone) {}
    public record RegisterResponse(String email, boolean verificationRequired) {}
}
