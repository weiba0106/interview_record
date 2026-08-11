package com.interviewrecord.mail.infrastructure;

import com.interviewrecord.mail.application.MailGateway;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@Profile("!e2e")
public class SmtpMailGateway implements MailGateway {
    private final JavaMailSender sender;
    private final String frontendBaseUrl;
    public SmtpMailGateway(JavaMailSender sender, @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.sender = sender; this.frontendBaseUrl = frontendBaseUrl;
    }
    @Override
    public void sendVerificationEmail(String email, String rawToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("验证你的面试记录账号");
        message.setText("请在 24 小时内打开以下链接完成邮箱验证：\n"
                + frontendBaseUrl + "/verify-email?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8));
        sender.send(message);
    }
    @Override
    public void sendPasswordResetEmail(String email, String rawToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("重置你的面试记录账号密码");
        message.setText("请在 1 小时内打开以下链接重置密码：\n"
                + frontendBaseUrl + "/reset-password?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8));
        sender.send(message);
    }
}
