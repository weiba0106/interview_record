package com.interviewrecord.reminders.domain;

import java.time.Instant;

/** 单条提醒的发送状态，供日程响应展示（尤其用于在应用内提示最终发送失败）。 */
public record ReminderState(Instant scheduledAt, String status, Instant sentAt) {
}
