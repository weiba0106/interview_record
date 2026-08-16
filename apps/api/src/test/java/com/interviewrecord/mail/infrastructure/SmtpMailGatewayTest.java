package com.interviewrecord.mail.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.interviewrecord.mail.application.ScheduleReminderMail;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpMailGatewayTest {

    private MimeMessage newMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    @Test
    void setsFromHeaderToTheAuthenticatedMailboxOnVerificationEmail() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = newMessage();
        given(sender.createMimeMessage()).willReturn(message);
        SmtpMailGateway gateway = new SmtpMailGateway(sender, "http://localhost:5173", "2263945169@qq.com");

        gateway.sendVerificationEmail("recipient@example.com", "raw-token");

        ArgumentCaptor<MimeMessage> capture = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(capture.capture());
        assertThat(capture.getValue().getFrom()[0].toString()).isEqualTo("2263945169@qq.com");

        String html = capture.getValue().getContent().toString();
        assertThat(html).contains("账号验证", "验证你的邮箱", "完成邮箱验证");
        assertThat(html).contains("http://localhost:5173/verify-email?token=raw-token");
        assertThat(html).contains("如果你没有注册过面试记录账号，请忽略这封邮件。");
    }

    @Test
    void passwordResetEmailIsFormattedHtmlWithTheResetLink() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = newMessage();
        given(sender.createMimeMessage()).willReturn(message);
        SmtpMailGateway gateway = new SmtpMailGateway(sender, "http://localhost:5173", "2263945169@qq.com");

        gateway.sendPasswordResetEmail("recipient@example.com", "reset-token");

        ArgumentCaptor<MimeMessage> capture = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(capture.capture());
        String html = capture.getValue().getContent().toString();
        assertThat(html).contains("账号安全", "重置密码", "设置新密码");
        assertThat(html).contains("http://localhost:5173/reset-password?token=reset-token");
        assertThat(html).contains("如果你没有申请重置密码，请忽略这封邮件。");
    }

    @Test
    void reminderEmailIsStructuredHtmlWithEscapedUserContent() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = newMessage();
        given(sender.createMimeMessage()).willReturn(message);

        SmtpMailGateway gateway = new SmtpMailGateway(sender, "http://localhost:5173", "reminder@example.com");
        gateway.sendScheduleReminder("candidate@example.com", new ScheduleReminderMail(
                "投递截止：阿里巴巴 <script>alert(1)</script>",
                "阿里巴巴 <b>公司</b>",
                "后端开发工程师",
                Instant.parse("2026-08-16T17:28:00Z"),
                "Asia/Shanghai",
                java.time.Duration.ofMinutes(120)));

        ArgumentCaptor<MimeMessage> capture = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(capture.capture());

        String html = capture.getValue().getContent().toString();
        assertThat(html).contains("面试记录 · 日程提醒");
        assertThat(html).contains("投递截止：阿里巴巴");
        assertThat(html).contains("阿里巴巴", "后端开发工程师");
        assertThat(html).contains("2026-08-17 01:28");
        assertThat(html).contains("Asia/Shanghai");
        assertThat(html).contains("http://localhost:5173/app/schedules");
        assertThat(html).contains("查看详情");
        assertThat(html).contains("本提醒提前 2 小时 发送");
        // 用户内容已 HTML 转义，脚本与标签不会原样进入邮件
        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
        assertThat(html).contains("&lt;b&gt;公司&lt;/b&gt;");

        String subject = capture.getValue().getSubject();
        assertThat(subject).contains("投递截止：阿里巴巴");
    }

    @Test
    void missingCompanyAndPositionRenderAsPlaceholders() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        MimeMessage message = newMessage();
        given(sender.createMimeMessage()).willReturn(message);

        SmtpMailGateway gateway = new SmtpMailGateway(sender, "http://localhost:5173", "reminder@example.com");
        gateway.sendScheduleReminder("candidate@example.com", new ScheduleReminderMail(
                "笔试", null, null, Instant.parse("2026-08-16T17:28:00Z"), null,
                java.time.Duration.ofMinutes(1440)));

        ArgumentCaptor<MimeMessage> capture = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(capture.capture());

        String html = capture.getValue().getContent().toString();
        // 空公司/岗位渲染为占位符（经 HTML 实体转义为 &mdash;，邮件客户端显示 —）
        assertThat(html).contains("&mdash;");
        // 无时区时回落到 UTC
        assertThat(html).contains("UTC");
    }
}
