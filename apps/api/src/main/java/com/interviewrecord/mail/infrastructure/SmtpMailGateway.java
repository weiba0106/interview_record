package com.interviewrecord.mail.infrastructure;

import com.interviewrecord.mail.application.MailGateway;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
    private final String fromAddress;
    public SmtpMailGateway(JavaMailSender sender, @Value("${app.frontend-base-url}") String frontendBaseUrl,
            @Value("${spring.mail.username}") String fromAddress) {
        this.sender = sender; this.frontendBaseUrl = frontendBaseUrl; this.fromAddress = fromAddress;
    }
    @Override
    public void sendVerificationEmail(String email, String rawToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("验证你的面试记录账号");
        message.setText("请在 24 小时内打开以下链接完成邮箱验证：\n"
                + frontendBaseUrl + "/verify-email?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8));
        sender.send(message);
    }
    @Override
    public void sendPasswordResetEmail(String email, String rawToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("重置你的面试记录账号密码");
        message.setText("请在 1 小时内打开以下链接重置密码：\n"
                + frontendBaseUrl + "/reset-password?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8));
        sender.send(message);
    }
    @Override
    public void sendScheduleReminder(String email, String scheduleTitle, Instant scheduledFor) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("面试记录日程提醒：" + scheduleTitle);
        message.setText("日程：" + scheduleTitle + "\n时间（UTC）：" + scheduledFor
                + "\n\n查看详情：" + frontendBaseUrl + "/app/schedules");
        sender.send(message);
    }
}
