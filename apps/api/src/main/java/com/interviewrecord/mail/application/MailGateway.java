package com.interviewrecord.mail.application;

public interface MailGateway {
    void sendVerificationEmail(String email, String rawToken);
    void sendPasswordResetEmail(String email, String rawToken);
}
