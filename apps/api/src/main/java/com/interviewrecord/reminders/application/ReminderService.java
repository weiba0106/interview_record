package com.interviewrecord.reminders.application;

import com.interviewrecord.mail.application.MailGateway;
import com.interviewrecord.mail.application.ScheduleReminderMail;
import com.interviewrecord.preference.domain.UserPreference;
import com.interviewrecord.preference.infrastructure.JpaUserPreferenceRepository;
import com.interviewrecord.reminders.domain.Reminder;
import com.interviewrecord.reminders.domain.ReminderState;
import com.interviewrecord.reminders.infrastructure.JpaReminderRepository;
import com.interviewrecord.scheduling.domain.ScheduleEvent;
import com.interviewrecord.scheduling.infrastructure.JpaScheduleEventRepository;
import com.interviewrecord.tracking.domain.Company;
import com.interviewrecord.tracking.domain.Position;
import com.interviewrecord.tracking.infrastructure.JpaCompanyRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists reminder intent in the schedule transaction; SMTP delivery is executed separately by the worker. */
@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class ReminderService {
    private static final int MAX_STATES_PER_SCHEDULE = 10;

    private final JpaReminderRepository reminders;
    private final JpaUserPreferenceRepository preferences;
    private final JpaScheduleEventRepository schedules;
    private final JpaPositionRepository positions;
    private final JpaCompanyRepository companies;
    private final MailGateway mail;
    private final Clock clock;

    public ReminderService(JpaReminderRepository reminders, JpaUserPreferenceRepository preferences,
            JpaScheduleEventRepository schedules, JpaPositionRepository positions,
            JpaCompanyRepository companies, MailGateway mail, Clock clock) {
        this.reminders = reminders; this.preferences = preferences; this.schedules = schedules;
        this.positions = positions; this.companies = companies; this.mail = mail; this.clock = clock;
    }

    @Transactional
    public void synchronize(Long userId, ScheduleEvent event) {
        Instant now = clock.instant();
        reminders.cancelUnsentByUserIdAndScheduleId(userId, event.id(), now);
        if (!event.pending() || event.referenceTime() == null) return;
        UserPreference preference = preferences.requireByUserId(userId);
        if (!preference.user().isVerified()) return;
        for (Integer offset : ReminderPlan.effectiveOffsets(event, preference)) {
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
            mail.sendScheduleReminder(preference.user().email(), buildMail(reminder, schedule, preference));
            reminder.markSent(now);
        } catch (RuntimeException exception) {
            reminder.markDeliveryFailure(now);
        }
    }

    /** 供日程响应展示的发送状态（含最终失败），按 scheduleId 分组。 */
    @Transactional(readOnly = true)
    public Map<Long, List<ReminderState>> statesBySchedule(Long userId, Set<Long> scheduleIds) {
        if (scheduleIds.isEmpty()) return Map.of();
        Map<Long, List<ReminderState>> states = new HashMap<>();
        for (Reminder reminder : reminders.findAllByUserIdAndScheduleIdInOrderByScheduledAtDesc(userId, scheduleIds)) {
            states.computeIfAbsent(reminder.scheduleId(), key -> new ArrayList<>())
                    .add(new ReminderState(reminder.scheduledAt(), reminder.status(), reminder.sentAt()));
        }
        states.replaceAll((key, list) -> list.size() > MAX_STATES_PER_SCHEDULE
                ? new ArrayList<>(list.subList(0, MAX_STATES_PER_SCHEDULE))
                : list);
        return states;
    }

    private ScheduleReminderMail buildMail(Reminder reminder, ScheduleEvent schedule, UserPreference preference) {
        Long userId = reminder.userId();
        String companyName = null;
        String positionTitle = null;
        if (schedule.positionId() != null) {
            Position position = positions.findByIdAndUserId(schedule.positionId(), userId).orElse(null);
            if (position != null) {
                positionTitle = position.title();
                Company company = companies.findByIdAndUserId(position.companyId(), userId).orElse(null);
                if (company != null) companyName = company.name();
            }
        }
        java.time.Duration leadTime = schedule.referenceTime() == null
                ? java.time.Duration.ZERO
                : java.time.Duration.between(reminder.scheduledAt(), schedule.referenceTime());
        return new ScheduleReminderMail(schedule.title(), companyName, positionTitle,
                schedule.referenceTime(), preference.timeZone(), leadTime);
    }

    private String idempotencyKey(Long scheduleId, int offset, Instant eventTime) {
        return scheduleId + ":" + offset + ":" + eventTime;
    }
}
