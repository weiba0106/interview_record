package com.interviewrecord.mail.application;

import java.time.Duration;
import java.time.Instant;

/** 日程提醒邮件所需内容；时间按用户时区格式化，公司/岗位允许为空。 */
public record ScheduleReminderMail(
        String title,
        String companyName,
        String positionTitle,
        Instant scheduledFor,
        String timeZone,
        Duration leadTime) {
}
