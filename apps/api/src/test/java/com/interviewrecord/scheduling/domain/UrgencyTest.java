package com.interviewrecord.scheduling.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UrgencyTest {
    private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

    @Test
    void overduePendingEventIsUrgent() {
        ScheduleEvent event = eventStartingAt(NOW.minus(Duration.ofHours(2)));
        assertThat(event.urgency(NOW)).isEqualTo(Urgency.URGENT);
    }

    @Test
    void eventWithinTwentyFourHoursIsUrgent() {
        ScheduleEvent event = eventStartingAt(NOW.plus(Duration.ofHours(23)));
        assertThat(event.urgency(NOW)).isEqualTo(Urgency.URGENT);
    }

    @Test
    void eventExactlyAtTwentyFourHoursIsApproaching() {
        ScheduleEvent event = eventStartingAt(NOW.plus(Duration.ofHours(24)));
        assertThat(event.urgency(NOW)).isEqualTo(Urgency.APPROACHING);
    }

    @Test
    void eventBetweenTwentyFourAndSeventyTwoHoursIsApproaching() {
        ScheduleEvent event = eventStartingAt(NOW.plus(Duration.ofHours(48)));
        assertThat(event.urgency(NOW)).isEqualTo(Urgency.APPROACHING);
    }

    @Test
    void eventBeyondSeventyTwoHoursIsNormal() {
        ScheduleEvent event = eventStartingAt(NOW.plus(Duration.ofHours(100)));
        assertThat(event.urgency(NOW)).isEqualTo(Urgency.NORMAL);
    }

    @Test
    void deadlineOnlyEventUsesEndsAtAsReference() {
        ScheduleEvent event = new ScheduleEvent(1L, "投递截止", "APPLY_DEADLINE", null,
                NOW.plus(Duration.ofHours(10)), null, null, null, null, NOW);
        assertThat(event.referenceTime()).isEqualTo(NOW.plus(Duration.ofHours(10)));
        assertThat(event.urgency(NOW)).isEqualTo(Urgency.URGENT);
    }

    @Test
    void manualOverrideWinsOverAutomaticUrgency() {
        ScheduleEvent event = eventStartingAt(NOW.plus(Duration.ofHours(100)));
        event.overrideUrgency("URGENT", NOW);
        assertThat(event.urgency(NOW)).isEqualTo(Urgency.URGENT);
    }

    @Test
    void clearingManualOverrideRestoresAutomaticUrgency() {
        ScheduleEvent event = eventStartingAt(NOW.plus(Duration.ofHours(100)));
        event.overrideUrgency("URGENT", NOW);
        event.overrideUrgency(null, NOW);
        assertThat(event.urgency(NOW)).isEqualTo(Urgency.NORMAL);
    }

    @Test
    void completedOrCancelledEventsAreHandledRegardlessOfTime() {
        ScheduleEvent completed = eventStartingAt(NOW.plus(Duration.ofHours(1)));
        completed.changeStatus(ScheduleEvent.STATUS_COMPLETED, NOW);
        assertThat(completed.urgency(NOW)).isEqualTo(Urgency.HANDLED);

        ScheduleEvent cancelled = eventStartingAt(NOW.minus(Duration.ofHours(5)));
        cancelled.changeStatus(ScheduleEvent.STATUS_CANCELLED, NOW);
        assertThat(cancelled.urgency(NOW)).isEqualTo(Urgency.HANDLED);
    }

    private ScheduleEvent eventStartingAt(Instant startsAt) {
        return new ScheduleEvent(1L, "面试", "INTERVIEW", startsAt, startsAt.plus(Duration.ofHours(1)),
                null, null, null, null, NOW);
    }
}
