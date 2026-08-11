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
    public record VerifyEmailRequest(@NotBlank @Size(max = 256) String token) {}
    public record ResendVerificationRequest(@NotBlank @Email @Size(max = 254) String email) {}
    public record LoginRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {}
    public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 254) String email) {}
    public record ResetPasswordRequest(
            @NotBlank @Size(max = 256) String token,
            @NotBlank @Size(max = 72) String newPassword) {}
    public record DeleteAccountRequest(@NotBlank @Size(max = 72) String password) {}
}
