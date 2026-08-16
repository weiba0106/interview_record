package com.interviewrecord.reminders.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.interviewrecord.auth.domain.User;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        // 默认规则：提前 24 小时与 2 小时各一次
        assertThat(capture.getAllValues()).extracting(Reminder::scheduledAt)
                .containsExactly(NOW.plusSeconds(2 * 3600), NOW.plusSeconds(24 * 3600));
    }

    @Test
    void customOffsetsReplacePreferenceDefaults() {
        JpaReminderRepository reminders = org.mockito.Mockito.mock(JpaReminderRepository.class);
        ReminderService service = service(reminders);
        ScheduleEvent event = event("INTERVIEW", NOW.plusSeconds(26 * 3600));
        event.overrideReminders("60,15", NOW);

        service.synchronize(1L, event);

        ArgumentCaptor<Reminder> capture = ArgumentCaptor.forClass(Reminder.class);
        verify(reminders, org.mockito.Mockito.times(2)).save(capture.capture());
        assertThat(capture.getAllValues()).extracting(Reminder::scheduledAt)
                .containsExactly(NOW.plusSeconds(25 * 3600), NOW.plusSeconds(25 * 3600 + 45 * 60));
    }

    @Test
    void disabledRemindersCreateNothing() {
        JpaReminderRepository reminders = org.mockito.Mockito.mock(JpaReminderRepository.class);
        ReminderService service = service(reminders);
        ScheduleEvent event = event("INTERVIEW", NOW.plusSeconds(26 * 3600));
        event.overrideReminders("", NOW);

        service.synchronize(1L, event);

        verify(reminders, org.mockito.Mockito.never()).save(any());
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
                .when(mail).sendScheduleReminder(any(), any());

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

        verify(mail, org.mockito.Mockito.times(1)).sendScheduleReminder(any(), any());
        assertThat(reminder.status()).isEqualTo(Reminder.STATUS_SENT);
    }

    @Test
    void deliveredMailCarriesCompanyPositionAndUserTimezone() {
        JpaReminderRepository reminders = org.mockito.Mockito.mock(JpaReminderRepository.class);
        MailGateway mail = org.mockito.Mockito.mock(MailGateway.class);
        JpaPositionRepository positions = org.mockito.Mockito.mock(JpaPositionRepository.class);
        JpaCompanyRepository companies = org.mockito.Mockito.mock(JpaCompanyRepository.class);
        ScheduleEvent schedule = event("INTERVIEW", NOW.plusSeconds(3600));
        ReflectionTestUtils.setField(schedule, "positionId", 7L);
        ReminderService service = service(reminders, mail, positions, companies, schedule);
        Position position = org.mockito.Mockito.mock(Position.class);
        given(position.title()).willReturn("后端开发工程师");
        given(position.companyId()).willReturn(77L);
        given(positions.findByIdAndUserId(7L, 1L)).willReturn(java.util.Optional.of(position));
        Company company = org.mockito.Mockito.mock(Company.class);
        given(company.name()).willReturn("示例科技");
        given(companies.findByIdAndUserId(77L, 1L)).willReturn(java.util.Optional.of(company));
        Reminder reminder = new Reminder(1L, 9L, "9:1440:2026-08-14T10:00:00Z", NOW, NOW);
        ReflectionTestUtils.setField(reminder, "id", 3L);
        ReflectionTestUtils.setField(reminder, "status", Reminder.STATUS_PROCESSING);
        given(reminders.findById(3L)).willReturn(java.util.Optional.of(reminder));

        service.deliverClaimed(3L);

        ArgumentCaptor<ScheduleReminderMail> capture = ArgumentCaptor.forClass(ScheduleReminderMail.class);
        verify(mail).sendScheduleReminder(eq("verified@example.com"), capture.capture());
        assertThat(capture.getValue().companyName()).isEqualTo("示例科技");
        assertThat(capture.getValue().positionTitle()).isEqualTo("后端开发工程师");
        assertThat(capture.getValue().timeZone()).isEqualTo("UTC");
        assertThat(capture.getValue().title()).isEqualTo("Interview");
    }

    @Test
    void statesByScheduleGroupsDeliveryStatesPerSchedule() {
        JpaReminderRepository reminders = org.mockito.Mockito.mock(JpaReminderRepository.class);
        ReminderService service = service(reminders);
        Reminder sent = new Reminder(1L, 9L, "9:30:2026-08-14T10:00:00Z", NOW.minusSeconds(3600), NOW.minusSeconds(7200));
        sent.markSent(NOW.minusSeconds(3600));
        Reminder failed = new Reminder(1L, 9L, "9:1440:2026-08-14T10:00:00Z", NOW, NOW);
        ReflectionTestUtils.setField(failed, "status", Reminder.STATUS_FAILED);
        given(reminders.findAllByUserIdAndScheduleIdInOrderByScheduledAtDesc(1L, Set.of(9L)))
                .willReturn(List.of(failed, sent));

        Map<Long, List<ReminderState>> states = service.statesBySchedule(1L, Set.of(9L));

        assertThat(states.get(9L)).extracting(ReminderState::status)
                .containsExactly(Reminder.STATUS_FAILED, Reminder.STATUS_SENT);
    }

    private ReminderService service(JpaReminderRepository reminders) {
        return service(reminders, org.mockito.Mockito.mock(MailGateway.class),
                org.mockito.Mockito.mock(JpaPositionRepository.class),
                org.mockito.Mockito.mock(JpaCompanyRepository.class), event("INTERVIEW", NOW.plusSeconds(3600)));
    }

    private ReminderService service(JpaReminderRepository reminders, MailGateway mail) {
        return service(reminders, mail, org.mockito.Mockito.mock(JpaPositionRepository.class),
                org.mockito.Mockito.mock(JpaCompanyRepository.class), event("INTERVIEW", NOW.plusSeconds(3600)));
    }

    private ReminderService service(JpaReminderRepository reminders, MailGateway mail,
            JpaPositionRepository positions, JpaCompanyRepository companies, ScheduleEvent activeSchedule) {
        JpaUserPreferenceRepository preferences = org.mockito.Mockito.mock(JpaUserPreferenceRepository.class);
        User user = new User("verified@example.com", "hash", "Verified", NOW);
        user.verify(NOW);
        UserPreference preference = new UserPreference(user, "UTC", NOW);
        given(preferences.requireByUserId(1L)).willReturn(preference);
        JpaScheduleEventRepository schedules = org.mockito.Mockito.mock(JpaScheduleEventRepository.class);
        given(schedules.findByIdAndUserId(9L, 1L)).willReturn(java.util.Optional.of(activeSchedule));
        return new ReminderService(reminders, preferences, schedules, positions, companies,
                mail, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private ScheduleEvent event(String type, Instant startsAt) {
        ScheduleEvent event = new ScheduleEvent(1L, "Interview", type, startsAt, null,
                null, null, null, null, NOW);
        ReflectionTestUtils.setField(event, "id", 9L);
        return event;
    }
}
