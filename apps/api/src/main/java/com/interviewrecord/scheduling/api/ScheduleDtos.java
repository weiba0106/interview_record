package com.interviewrecord.scheduling.api;

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
            Long version) {}

    public record StatusUpdateRequest(@NotBlank String status) {}

    public record UrgencyOverrideRequest(String urgency) {}

    public record ScheduleResponse(String id, String title, String eventType, Instant startsAt, Instant endsAt,
            String positionId, String positionTitle, String interviewRoundId, String location, String notes,
            String status, String urgency, boolean overdue, String manualUrgency, Instant referenceTime,
            long version, Instant updatedAt) {}

    public record ScheduleListResponse(List<ScheduleResponse> items) {}
}
