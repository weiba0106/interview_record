package com.interviewrecord.reminders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.interviewrecord.auth.domain.User;
import com.interviewrecord.mail.application.MailGateway;
import com.interviewrecord.preference.domain.UserPreference;
import com.interviewrecord.preference.infrastructure.JpaUserPreferenceRepository;
import com.interviewrecord.reminders.domain.Reminder;
import com.interviewrecord.reminders.infrastructure.JpaReminderRepository;
import com.interviewrecord.scheduling.domain.ScheduleEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ReminderServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T10:00:00Z");

    @Test
    void synchronizingCancelledScheduleCancelsEveryUnsentReminder() {
        JpaReminderRepository reminders = org.mockito.Mockito.mock(JpaReminderRepository.class);
        ReminderService service = service(reminders);
        ScheduleEvent event = event("INTERVIEW", NOW.plusSeconds(3600));
        event.changeStatus(ScheduleEvent.STATUS_CANCELLED, NOW);

        service.synchronize(1L, event);

        verify(reminders).cancelUnsentByUserIdAndScheduleId(1L, 9L, NOW);
        verify(reminders, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void synchronizingChangedScheduleReplacesUnsentDefaultsWithNewTimes() {
        JpaReminderRepository reminders = org.mockito.Mockito.mock(JpaReminderRepository.class);
        ReminderService service = service(reminders);
        ScheduleEvent event = event("INTERVIEW", NOW.plusSeconds(26 * 3600));

        service.synchronize(1L, event);

        ArgumentCaptor<Reminder> capture = ArgumentCaptor.forClass(Reminder.class);
        verify(reminders, org.mockito.Mockito.times(2)).save(capture.capture());
        assertThat(capture.getAllValues()).extracting(Reminder::scheduledAt)
                .containsExactly(NOW.plusSeconds(2 * 3600), NOW.plusSeconds(25 * 3600 + 30 * 60));
    }

    @Test
    void failedDeliveryRetriesOnlyThreeTimesThenBecomesFailed() {
        JpaReminderRepository reminders = org.mockito.Mockito.mock(JpaReminderRepository.class);
        MailGateway mail = org.mockito.Mockito.mock(MailGateway.class);
        ReminderService service = service(reminders, mail);
        Reminder reminder = new Reminder(1L, 9L, "9:1440:2026-08-14T10:00:00Z", NOW, NOW);
        ReflectionTestUtils.setField(reminder, "id", 3L);
        given(reminders.findById(3L)).willReturn(java.util.Optional.of(reminder));
        org.mockito.Mockito.doThrow(new RuntimeException("smtp unavailable"))
                .when(mail).sendScheduleReminder(any(), any(), any());

        ReflectionTestUtils.setField(reminder, "status", Reminder.STATUS_PROCESSING);
        service.deliverClaimed(3L);
        ReflectionTestUtils.setField(reminder, "status", Reminder.STATUS_PROCESSING);
        service.deliverClaimed(3L);
        ReflectionTestUtils.setField(reminder, "status", Reminder.STATUS_PROCESSING);
        service.deliverClaimed(3L);

        assertThat(reminder.status()).isEqualTo(Reminder.STATUS_FAILED);
        assertThat(reminder.attemptCount()).isEqualTo(3);
    }

    @Test
    void claimAllowsOnlyOneWorkerToSendTheSameReminder() {
        JpaReminderRepository reminders = org.mockito.Mockito.mock(JpaReminderRepository.class);
        MailGateway mail = org.mockito.Mockito.mock(MailGateway.class);
        ReminderService service = service(reminders, mail);
        given(reminders.claim(3L, NOW)).willReturn(1, 0);
        Reminder reminder = new Reminder(1L, 9L, "9:30:2026-08-14T10:00:00Z", NOW, NOW);
        ReflectionTestUtils.setField(reminder, "id", 3L);
        ReflectionTestUtils.setField(reminder, "status", Reminder.STATUS_PROCESSING);
        given(reminders.findById(3L)).willReturn(java.util.Optional.of(reminder));

        assertThat(service.claim(3L)).isTrue();
        assertThat(service.claim(3L)).isFalse();
        service.deliverClaimed(3L);

        verify(mail, org.mockito.Mockito.times(1)).sendScheduleReminder(any(), any(), any());
        assertThat(reminder.status()).isEqualTo(Reminder.STATUS_SENT);
    }

    private ReminderService service(JpaReminderRepository reminders) {
        return service(reminders, org.mockito.Mockito.mock(MailGateway.class));
    }

    private ReminderService service(JpaReminderRepository reminders, MailGateway mail) {
        JpaUserPreferenceRepository preferences = org.mockito.Mockito.mock(JpaUserPreferenceRepository.class);
        User user = new User("verified@example.com", "hash", "Verified", NOW);
        user.verify(NOW);
        UserPreference preference = new UserPreference(user, "UTC", NOW);
        given(preferences.requireByUserId(1L)).willReturn(preference);
        return new ReminderService(reminders, preferences,
                org.mockito.Mockito.mock(com.interviewrecord.scheduling.infrastructure.JpaScheduleEventRepository.class),
                mail, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ScheduleEvent event(String type, Instant startsAt) {
        ScheduleEvent event = new ScheduleEvent(1L, "Interview", type, startsAt, null,
                null, null, null, null, NOW);
        ReflectionTestUtils.setField(event, "id", 9L);
        return event;
    }
}
