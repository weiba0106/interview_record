package com.interviewrecord.mail.infrastructure;

import tools.jackson.databind.ObjectMapper;
import com.interviewrecord.mail.application.MailGateway;
import com.interviewrecord.mail.application.ScheduleReminderMail;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** E2E-only transport. It never sends a real message or writes a token to application logs. */
@Component
@Profile("e2e")
public class CapturingMailGateway implements MailGateway {
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String frontendBaseUrl;
    private final Path mailboxPath;

    public CapturingMailGateway(ObjectMapper objectMapper, Clock clock,
            @Value("${app.frontend-base-url}") String frontendBaseUrl,
            @Value("${app.e2e-mailbox-path}") String mailboxPath) {
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.frontendBaseUrl = frontendBaseUrl;
        this.mailboxPath = Path.of(mailboxPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.mailboxPath.getParent());
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to create E2E mailbox directory", exception);
        }
    }

    @Override
    public void sendVerificationEmail(String email, String rawToken) {
        append(email, "VERIFY_EMAIL", "/verify-email?token=" + encode(rawToken));
    }

    @Override
    public void sendPasswordResetEmail(String email, String rawToken) {
        append(email, "RESET_PASSWORD", "/reset-password?token=" + encode(rawToken));
    }

    @Override
    public void sendScheduleReminder(String email, ScheduleReminderMail mail) {
        append(email, "SCHEDULE_REMINDER", "/app/schedules");
    }

    private String encode(String rawToken) {
        return URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }

    private synchronized void append(String recipient, String type, String pathAndQuery) {
        CapturedMail message = new CapturedMail(recipient, type, frontendBaseUrl + pathAndQuery, clock.instant());
        try {
            Files.writeString(mailboxPath, objectMapper.writeValueAsString(message) + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to append an E2E mailbox message", exception);
        }
    }

    private record CapturedMail(String recipient, String type, String url, Instant createdAt) {
    }
}
