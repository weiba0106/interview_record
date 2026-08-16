package com.interviewrecord.dashboard.api;

import com.interviewrecord.scheduling.api.ScheduleDtos.ScheduleResponse;
import com.interviewrecord.tracking.api.TrackingDtos.PositionResponse;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record Metrics(long totalPositions, long activePositions,
            long upcomingScheduleCount, long offerCount) {}

    public record DashboardResponse(Metrics metrics, List<PositionResponse> positions,
            List<ScheduleResponse> schedules) {}
}
