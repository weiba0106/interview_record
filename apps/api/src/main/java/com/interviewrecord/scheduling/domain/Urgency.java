package com.interviewrecord.scheduling.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Urgency follows PRD 7.6.3. A manual override wins over the automatic rule
 * while the event is pending; clearing the override restores automation.
 * Completed or cancelled events are always HANDLED.
 */
public enum Urgency {
    URGENT, APPROACHING, NORMAL, HANDLED;

    private static final Duration URGENT_WINDOW = Duration.ofHours(24);
    private static final Duration APPROACHING_WINDOW = Duration.ofHours(72);

    public static Urgency of(ScheduleEvent event, Instant now) {
        if (!event.pending()) {
            return HANDLED;
        }
        if (event.manualUrgency() != null) {
            return Urgency.valueOf(event.manualUrgency());
        }
        return automatic(event.referenceTime(), now);
    }

    public static Urgency automatic(Instant referenceTime, Instant now) {
        if (referenceTime == null) {
            return NORMAL;
        }
        Duration remaining = Duration.between(now, referenceTime);
        if (remaining.isNegative() || remaining.compareTo(URGENT_WINDOW) < 0) {
            return URGENT;
        }
        if (remaining.compareTo(APPROACHING_WINDOW) < 0) {
            return APPROACHING;
        }
        return NORMAL;
    }

    public boolean overdue() {
        return this == URGENT;
    }
}
