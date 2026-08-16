package com.interviewrecord.reminders.application;

import com.interviewrecord.preference.domain.UserPreference;
import com.interviewrecord.scheduling.domain.ScheduleEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** 提醒时间规则：日程覆盖优先，否则回落用户偏好默认值。 */
public final class ReminderPlan {
    public static final int MAX_CUSTOM_OFFSETS = 5;
    public static final int MAX_OFFSET_MINUTES = 10_080;

    private ReminderPlan() { }

    public static List<Integer> defaultOffsets(String eventType, List<Integer> interviewOffsets, List<Integer> deadlineOffsets) {
        return switch (eventType) {
            case "INTERVIEW" -> interviewOffsets;
            case "WRITTEN_TEST", "APPLY_DEADLINE", "OFFER_DEADLINE" -> deadlineOffsets;
            default -> List.of();
        };
    }

    /** 日程自身覆盖优先；未覆盖时回落到用户偏好默认规则。 */
    public static List<Integer> effectiveOffsets(ScheduleEvent event, UserPreference preference) {
        String override = event.reminderOffsets();
        if (override != null) return parse(override);
        return defaultOffsets(event.eventType(),
                preference.interviewReminderOffsets(), preference.deadlineReminderOffsets());
    }

    /** '' 表示关闭；'1440,30' 解析为去重后的倒序分钟列表。 */
    public static List<Integer> parse(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(Integer::valueOf)
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /** null=跟随默认；空列表=关闭；否则倒序去重后的逗号字符串。 */
    public static String canonical(List<Integer> offsets) {
        if (offsets == null) return null;
        return offsets.stream().distinct().sorted(Comparator.reverseOrder())
                .map(String::valueOf).collect(Collectors.joining(","));
    }

    public static Instant scheduledAt(Instant eventTime, int offsetMinutes) {
        return eventTime.minus(Duration.ofMinutes(offsetMinutes));
    }
}
