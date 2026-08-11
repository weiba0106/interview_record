package com.interviewrecord.support;

import com.interviewrecord.mail.application.MailGateway;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.mail.MailSendException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

public final class FakeMailGateway implements MailGateway {
    private final List<VerificationMessage> verificationMessages = new CopyOnWriteArrayList<>();
    private final List<PasswordResetMessage> passwordResetMessages = new CopyOnWriteArrayList<>();
    private volatile boolean failVerificationDelivery;

    @Override
    public void sendVerificationEmail(String email, String rawToken) {
        if (failVerificationDelivery) throw new MailSendException("simulated delivery failure");
        verificationMessages.add(new VerificationMessage(email, rawToken));
    }

    public List<VerificationMessage> verificationMessages() {
        return List.copyOf(verificationMessages);
    }
    @Override
    public void sendPasswordResetEmail(String email, String rawToken) {
        if (failVerificationDelivery) throw new MailSendException("simulated delivery failure");
        passwordResetMessages.add(new PasswordResetMessage(email, rawToken));
    }
    public List<PasswordResetMessage> passwordResetMessages() { return List.copyOf(passwordResetMessages); }

    public void failVerificationDelivery() { failVerificationDelivery = true; }
    public void reset() { verificationMessages.clear(); passwordResetMessages.clear(); failVerificationDelivery = false; }

    public record VerificationMessage(String email, String rawToken) {}
    public record PasswordResetMessage(String email, String rawToken) {}

    @TestConfiguration
    public static class Config {
        @Bean
        @Primary
        FakeMailGateway fakeMailGateway() {
            return new FakeMailGateway();
        }
    }
}
