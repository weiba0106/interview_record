package com.interviewrecord.mail.application;

import java.time.Instant;

public interface MailGateway {
    void sendVerificationEmail(String email, String rawToken);
    void sendPasswordResetEmail(String email, String rawToken);
    void sendScheduleReminder(String email, String scheduleTitle, Instant scheduledFor);
}
