package com.interviewrecord.mail.infrastructure;

import com.interviewrecord.mail.application.MailGateway;
import com.interviewrecord.mail.application.ScheduleReminderMail;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

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
        sendActionEmail(email, "验证你的面试记录账号",
                "账号验证", "验证你的邮箱",
                "请在 24 小时内点击下方按钮完成邮箱验证，链接一次性有效。",
                "完成邮箱验证",
                frontendBaseUrl + "/verify-email?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8),
                "如果你没有注册过面试记录账号，请忽略这封邮件。");
    }

    @Override
    public void sendPasswordResetEmail(String email, String rawToken) {
        sendActionEmail(email, "重置你的面试记录账号密码",
                "账号安全", "重置密码",
                "请在 1 小时内点击下方按钮设置新密码，链接一次性有效。",
                "设置新密码",
                frontendBaseUrl + "/reset-password?token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8),
                "如果你没有申请重置密码，请忽略这封邮件。");
    }

    /** 账号类操作邮件（验证/重置）共用同一套卡片模板。 */
    private void sendActionEmail(String to, String subject, String kicker, String title,
            String body, String buttonLabel, String rawLink, String footer) {
        try {
            MimeMessageHelper helper = new MimeMessageHelper(sender.createMimeMessage(), StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildActionHtml(kicker, title, body, buttonLabel,
                    HtmlUtils.htmlEscape(rawLink), footer), true);
            sender.send(helper.getMimeMessage());
        } catch (jakarta.mail.MessagingException exception) {
            throw new IllegalStateException("Unable to build account email", exception);
        }
    }

    private String buildActionHtml(String kicker, String title, String body, String buttonLabel,
            String link, String footer) {
        return """
                <div style="background:#f6f7f6;padding:24px 12px;font-family:'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;color:#23262a;">
                  <div style="max-width:480px;margin:0 auto;background:#ffffff;border:1px solid #e2e5e2;border-radius:12px;overflow:hidden;">
                    <div style="padding:18px 24px 14px;border-bottom:1px solid #eef0ee;">
                      <span style="font-size:12px;font-weight:700;letter-spacing:.06em;color:#d2403a;">面试记录 · %s</span>
                      <div style="font-size:17px;font-weight:700;margin-top:8px;line-height:1.5;">%s</div>
                      <div style="font-size:14px;color:#6b716c;margin-top:8px;line-height:1.7;">%s</div>
                    </div>
                    <div style="padding:16px 24px 20px;">
                      <a href="%s" style="display:inline-block;background:#d2403a;color:#ffffff;text-decoration:none;padding:11px 24px;border-radius:8px;font-size:14px;font-weight:600;">%s</a>
                    </div>
                    <div style="padding:12px 24px;border-top:1px solid #eef0ee;color:#9aa09b;font-size:12px;">
                      %s
                    </div>
                  </div>
                </div>
                """.formatted(kicker, title, body, link, buttonLabel, footer);
    }

    @Override
    public void sendScheduleReminder(String email, ScheduleReminderMail mail) {
        try {
            MimeMessageHelper helper = new MimeMessageHelper(sender.createMimeMessage(), StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject("面试记录日程提醒：" + safeSubject(mail.title()));
            helper.setText(buildReminderHtml(mail), true);
            sender.send(helper.getMimeMessage());
        } catch (jakarta.mail.MessagingException exception) {
            throw new IllegalStateException("Unable to build schedule reminder email", exception);
        }
    }

    /**
     * 结构化 HTML 邮件：内联样式 + 卡片布局，兼容常见邮件客户端；
     * 所有用户内容经过 HTML 转义，链接使用系统基准地址拼接。
     */
    private String buildReminderHtml(ScheduleReminderMail mail) {
        ZoneId zone = userZone(mail.timeZone());
        String time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone).format(mail.scheduledFor());
        String title = HtmlUtils.htmlEscape(mail.title());
        String company = HtmlUtils.htmlEscape(nullable(mail.companyName()));
        String position = HtmlUtils.htmlEscape(nullable(mail.positionTitle()));
        String link = frontendBaseUrl + "/app/schedules";
        String zoneText = zoneLabel(zone);
        return """
                <div style="background:#f6f7f6;padding:24px 12px;font-family:'Segoe UI','PingFang SC','Microsoft YaHei',sans-serif;color:#23262a;">
                  <div style="max-width:480px;margin:0 auto;background:#ffffff;border:1px solid #e2e5e2;border-radius:12px;overflow:hidden;">
                    <div style="padding:18px 24px 14px;border-bottom:1px solid #eef0ee;">
                      <span style="font-size:12px;font-weight:700;letter-spacing:.06em;color:#d2403a;">面试记录 · 日程提醒</span>
                      <div style="font-size:17px;font-weight:700;margin-top:8px;line-height:1.5;">%s</div>
                    </div>
                    <table style="width:100%%;border-collapse:collapse;font-size:14px;line-height:1.9;margin:6px 0;">
                      <tr><td style="color:#6b716c;width:86px;padding:3px 24px;">公司</td><td style="padding:3px 24px 3px 0;">%s</td></tr>
                      <tr><td style="color:#6b716c;padding:3px 24px;">岗位</td><td style="padding:3px 24px 3px 0;">%s</td></tr>
                      <tr><td style="color:#6b716c;padding:3px 24px;">时间</td><td style="padding:3px 24px 3px 0;">%s（%s）</td></tr>
                    </table>
                    <div style="padding:10px 24px 20px;">
                      <a href="%s" style="display:inline-block;background:#d2403a;color:#ffffff;text-decoration:none;padding:10px 22px;border-radius:8px;font-size:14px;font-weight:600;">查看详情</a>
                    </div>
                    <div style="padding:12px 24px;border-top:1px solid #eef0ee;color:#9aa09b;font-size:12px;">
                      时间按你的时区显示 · 你的数据仅对自己可见
                    </div>
                  </div>
                </div>
                """.formatted(title, company, position, time, zoneText, link);
    }

    /** 主题里的用户内容去掉换行，防止邮件头注入。 */
    private String safeSubject(String title) {
        return title == null ? "" : title.replaceAll("[\\r\\n]+", " ").trim();
    }

    private ZoneId userZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) return ZoneOffset.UTC;
        try {
            return ZoneId.of(timeZone);
        } catch (DateTimeException invalid) {
            return ZoneOffset.UTC;
        }
    }

    /** ZoneOffset.UTC.getId() 是 "Z"，邮件里显示为更友好的 "UTC"。 */
    private String zoneLabel(ZoneId zone) {
        return ZoneOffset.UTC.equals(zone) ? "UTC" : zone.getId();
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
