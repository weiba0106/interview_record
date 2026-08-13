package com.interviewrecord.reminders.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class ReminderDeliveryJob {
    private final ReminderService reminders;

    public ReminderDeliveryJob(ReminderService reminders) { this.reminders = reminders; }

    @Scheduled(fixedDelayString = "${app.reminders.poll-delay:PT1M}")
    public void deliverDueReminders() {
        for (Long id : reminders.readyReminderIds(50)) {
            if (reminders.claim(id)) reminders.deliverClaimed(id);
        }
    }
}
