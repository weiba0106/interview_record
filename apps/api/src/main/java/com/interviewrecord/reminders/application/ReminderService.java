package com.interviewrecord.reminders.application;

import com.interviewrecord.mail.application.MailGateway;
import com.interviewrecord.preference.domain.UserPreference;
import com.interviewrecord.preference.infrastructure.JpaUserPreferenceRepository;
import com.interviewrecord.reminders.domain.Reminder;
import com.interviewrecord.reminders.infrastructure.JpaReminderRepository;
import com.interviewrecord.scheduling.domain.ScheduleEvent;
import com.interviewrecord.scheduling.infrastructure.JpaScheduleEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists reminder intent in the schedule transaction; SMTP delivery is executed separately by the worker. */
@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class ReminderService {
    private final JpaReminderRepository reminders;
    private final JpaUserPreferenceRepository preferences;
    private final JpaScheduleEventRepository schedules;
    private final MailGateway mail;
    private final Clock clock;

    public ReminderService(JpaReminderRepository reminders, JpaUserPreferenceRepository preferences,
            JpaScheduleEventRepository schedules, MailGateway mail, Clock clock) {
        this.reminders = reminders; this.preferences = preferences; this.schedules = schedules;
        this.mail = mail; this.clock = clock;
    }

    @Transactional
    public void synchronize(Long userId, ScheduleEvent event) {
        Instant now = clock.instant();
        reminders.cancelUnsentByUserIdAndScheduleId(userId, event.id(), now);
        if (!event.pending() || event.referenceTime() == null) return;
        UserPreference preference = preferences.requireByUserId(userId);
        if (!preference.user().isVerified()) return;
        for (Integer offset : ReminderPlan.defaultOffsets(event.eventType(),
                preference.interviewReminderOffsets(), preference.deadlineReminderOffsets())) {
            Instant scheduledAt = ReminderPlan.scheduledAt(event.referenceTime(), offset);
            String key = idempotencyKey(event.id(), offset, event.referenceTime());
            if (!reminders.existsByIdempotencyKey(key)) {
                reminders.save(new Reminder(userId, event.id(), key, scheduledAt, now));
            }
        }
    }

    @Transactional
    public void cancel(Long userId, Long scheduleId) {
        reminders.cancelUnsentByUserIdAndScheduleId(userId, scheduleId, clock.instant());
    }

    @Transactional(readOnly = true)
    public List<Long> readyReminderIds(int limit) {
        return reminders.findReadyIds(clock.instant(), PageRequest.of(0, limit));
    }

    /** Atomic database claim prevents two scheduler nodes from delivering the same persisted reminder. */
    @Transactional
    public boolean claim(Long reminderId) {
        return reminders.claim(reminderId, clock.instant()) == 1;
    }

    /** Called only after a successful claim, outside the claim transaction. */
    @Transactional
    public void deliverClaimed(Long reminderId) {
        Instant now = clock.instant();
        Reminder reminder = reminders.findById(reminderId).orElse(null);
        if (reminder == null || !Reminder.STATUS_PROCESSING.equals(reminder.status())) return;
        ScheduleEvent schedule = schedules.findByIdAndUserId(reminder.scheduleId(), reminder.userId()).orElse(null);
        if (schedule == null || !schedule.pending()) {
            reminder.cancel(now);
            return;
        }
        UserPreference preference = preferences.requireByUserId(reminder.userId());
        if (!preference.user().isVerified()) {
            reminder.cancel(now);
            return;
        }
        try {
            mail.sendScheduleReminder(preference.user().email(), schedule.title(), schedule.referenceTime());
            reminder.markSent(now);
        } catch (RuntimeException exception) {
            reminder.markDeliveryFailure(now);
        }
    }

    private String idempotencyKey(Long scheduleId, int offset, Instant eventTime) {
        return scheduleId + ":" + offset + ":" + eventTime;
    }
}
