package com.interviewrecord.scheduling.api;

import com.interviewrecord.reminders.domain.ReminderState;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class ScheduleDtos {
    private ScheduleDtos() {}

    public record ScheduleRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank String eventType,
            Instant startsAt,
            Instant endsAt,
            String positionId,
            String interviewRoundId,
            @Size(max = 500) String location,
            @Size(max = 2000) String notes,
            /** 提醒覆盖：null/缺省=跟随默认规则；空数组=关闭；否则为事件前分钟数。 */
            List<@Min(0) @Max(10080) Integer> reminderOffsets,
            Long version) {}

    public record StatusUpdateRequest(@NotBlank String status) {}

    public record UrgencyOverrideRequest(String urgency) {}

    /** null=恢复默认规则；空数组=关闭提醒；非空=自定义提醒时间。 */
    public record ReminderUpdateRequest(List<@Min(0) @Max(10080) Integer> offsets) {}

    public record ScheduleResponse(String id, String title, String eventType, Instant startsAt, Instant endsAt,
            String positionId, String positionTitle, String interviewRoundId, String location, String notes,
            String status, String urgency, boolean overdue, String manualUrgency, Instant referenceTime,
            long version, Instant updatedAt, List<Integer> reminderOffsets, List<ReminderState> reminders) {}

    public record ScheduleListResponse(List<ScheduleResponse> items) {}
}
