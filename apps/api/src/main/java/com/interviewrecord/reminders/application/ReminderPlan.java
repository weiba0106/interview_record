package com.interviewrecord.reminders.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

final class ReminderPlan {
    private ReminderPlan() { }

    static List<Integer> defaultOffsets(String eventType, List<Integer> interviewOffsets, List<Integer> deadlineOffsets) {
        return switch (eventType) {
            case "INTERVIEW" -> interviewOffsets;
            case "WRITTEN_TEST", "APPLY_DEADLINE", "OFFER_DEADLINE" -> deadlineOffsets;
            default -> List.of();
        };
    }

    static Instant scheduledAt(Instant eventTime, int offsetMinutes) {
        return eventTime.minus(Duration.ofMinutes(offsetMinutes));
    }
}
