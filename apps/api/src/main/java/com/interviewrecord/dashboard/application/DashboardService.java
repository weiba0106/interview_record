package com.interviewrecord.dashboard.application;

import com.interviewrecord.dashboard.api.DashboardDtos.DashboardResponse;
import com.interviewrecord.dashboard.api.DashboardDtos.Metrics;
import com.interviewrecord.scheduling.application.ScheduleService;
import com.interviewrecord.tracking.application.PositionService;
import com.interviewrecord.tracking.domain.PositionStatus;
import com.interviewrecord.tracking.infrastructure.JpaManagedPositionStatusRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class DashboardService {
    private static final int POSITION_ROWS = 10;
    private static final int SCHEDULE_ROWS = 5;

    private final PositionService positionService;
    private final ScheduleService scheduleService;
    private final JpaPositionRepository positions;
    private final JpaManagedPositionStatusRepository statuses;
    private final Clock clock;

    public DashboardService(PositionService positionService, ScheduleService scheduleService,
            JpaPositionRepository positions, JpaManagedPositionStatusRepository statuses, Clock clock) {
        this.positionService = positionService; this.scheduleService = scheduleService;
        this.positions = positions; this.statuses = statuses; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardResponse overview(Long userId) {
        Instant now = clock.instant();
        List<PositionStatus> allStatuses = statuses.findAllByUserIdOrderBySortOrderAsc(userId);
        List<Long> activeStatusIds = allStatuses.stream()
                .filter(status -> "ACTIVE".equals(status.statisticsCategory()))
                .map(PositionStatus::id).toList();
        List<Long> successStatusIds = allStatuses.stream()
                .filter(status -> "SUCCESS".equals(status.statisticsCategory()))
                .map(PositionStatus::id).toList();
        long total = positions.countByUserIdAndArchived(userId, false);
        long active = activeStatusIds.isEmpty() ? 0
                : positions.countByUserIdAndStatusIdInAndArchived(userId, activeStatusIds, false);
        long offers = successStatusIds.isEmpty() ? 0
                : positions.countByUserIdAndStatusIdInAndArchived(userId, successStatusIds, false);
        long upcoming = scheduleService.countPendingWithin(userId, now, now.plus(Duration.ofDays(7)));
        Metrics metrics = new Metrics(total, active, upcoming, offers);
        var positionRows = positionService.search(userId, null, null, null, false, null,
                0, POSITION_ROWS, "updatedAt", "desc").items();
        return new DashboardResponse(metrics, positionRows,
                scheduleService.upcomingForDashboard(userId, SCHEDULE_ROWS));
    }
}
