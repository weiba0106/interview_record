package com.interviewrecord.reminders.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReminderPlanTest {
    private static final Instant EVENT_AT = Instant.parse("2026-08-14T10:00:00Z");

    @Test
    void interviewDefaultsScheduleTwentyFourHourAndThirtyMinuteReminders() {
        assertThat(ReminderPlan.defaultOffsets("INTERVIEW", List.of(1440, 30), List.of(1440)))
                .containsExactly(1440, 30);
        assertThat(ReminderPlan.scheduledAt(EVENT_AT, 1440)).isEqualTo(Instant.parse("2026-08-13T10:00:00Z"));
        assertThat(ReminderPlan.scheduledAt(EVENT_AT, 30)).isEqualTo(Instant.parse("2026-08-14T09:30:00Z"));
    }

    @Test
    void deadlineDefaultsScheduleOnlyTwentyFourHourReminder() {
        assertThat(ReminderPlan.defaultOffsets("APPLY_DEADLINE", List.of(1440, 30), List.of(1440)))
                .containsExactly(1440);
        assertThat(ReminderPlan.defaultOffsets("WRITTEN_TEST", List.of(1440, 30), List.of(1440)))
                .containsExactly(1440);
        assertThat(ReminderPlan.defaultOffsets("OFFER_DEADLINE", List.of(1440, 30), List.of(1440)))
                .containsExactly(1440);
    }
}
