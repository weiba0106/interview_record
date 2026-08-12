package com.interviewrecord.mail.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpMailGatewayTest {

    @Test
    void setsFromHeaderToTheAuthenticatedMailbox() {
        JavaMailSender sender = org.mockito.Mockito.mock(JavaMailSender.class);
        SmtpMailGateway gateway = new SmtpMailGateway(sender, "http://localhost:5173", "2263945169@qq.com");

        gateway.sendVerificationEmail("recipient@example.com", "raw-token");

        ArgumentCaptor<SimpleMailMessage> messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(messages.capture());
        assertThat(messages.getValue().getFrom()).isEqualTo("2263945169@qq.com");
    }
}
