package com.interviewrecord.insights.application;

import com.interviewrecord.insights.api.InsightDtos.ApplicationTrendItem;
import com.interviewrecord.insights.api.InsightDtos.ConversionMetrics;
import com.interviewrecord.insights.api.InsightDtos.ConversionRate;
import com.interviewrecord.insights.api.InsightDtos.InsightFilter;
import com.interviewrecord.insights.api.InsightDtos.InsightsResponse;
import com.interviewrecord.insights.api.InsightDtos.JobTypeBreakdownItem;
import com.interviewrecord.insights.api.InsightDtos.StatusDistributionItem;
import com.interviewrecord.interviews.domain.InterviewRound;
import com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository;
import com.interviewrecord.tracking.domain.JobType;
import com.interviewrecord.tracking.domain.Position;
import com.interviewrecord.tracking.domain.PositionStatus;
import com.interviewrecord.tracking.infrastructure.JpaManagedJobTypeRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedPositionStatusRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

/**
 * Calculates a user's statistics from user-scoped repositories only.  The service deliberately
 * receives the authenticated user id from its caller rather than accepting it from the browser.
 */
@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class InsightsService {
    private final JpaPositionRepository positions;
    private final JpaInterviewRoundRepository rounds;
    private final JpaManagedPositionStatusRepository statuses;
    private final JpaManagedJobTypeRepository jobTypes;

    public InsightsService(JpaPositionRepository positions, JpaInterviewRoundRepository rounds,
            JpaManagedPositionStatusRepository statuses, JpaManagedJobTypeRepository jobTypes) {
        this.positions = positions;
        this.rounds = rounds;
        this.statuses = statuses;
        this.jobTypes = jobTypes;
    }

    public InsightsResponse getInsights(long userId, InsightFilter filter) {
        InsightFilter effectiveFilter = filter == null ? new InsightFilter(null, null, null) : filter;
        List<Position> filteredPositions = positions.findAllByUserId(userId).stream()
                .filter(position -> matches(position, effectiveFilter))
                .toList();
        List<InterviewRound> userRounds = rounds.findAllByUserId(userId);
        Set<Long> selectedPositionIds = filteredPositions.stream()
                .map(Position::id)
                .collect(Collectors.toSet());
        List<InterviewRound> filteredRounds = userRounds.stream()
                .filter(round -> selectedPositionIds.contains(round.positionId()))
                .toList();

        List<PositionStatus> userStatuses = statuses.findAllByUserIdOrderBySortOrderAsc(userId);
        List<JobType> userJobTypes = jobTypes.findAllByUserIdOrderByIdAsc(userId);
        Map<Long, PositionStatus> statusesById = userStatuses.stream()
                .collect(Collectors.toMap(PositionStatus::id, status -> status));

        return new InsightsResponse(
                statusDistribution(userStatuses, filteredPositions),
                jobTypeBreakdown(userJobTypes, filteredPositions, filteredRounds, statusesById),
                applicationTrend(filteredPositions, filteredRounds),
                conversions(filteredPositions, filteredRounds, statusesById));
    }

    private boolean matches(Position position, InsightFilter filter) {
        if (filter.jobTypeId() != null && !filter.jobTypeId().equals(position.jobTypeId())) {
            return false;
        }
        LocalDate appliedAt = position.appliedAt();
        if (filter.appliedFrom() != null && (appliedAt == null || appliedAt.isBefore(filter.appliedFrom()))) {
            return false;
        }
        return filter.appliedTo() == null || (appliedAt != null && !appliedAt.isAfter(filter.appliedTo()));
    }

    private List<StatusDistributionItem> statusDistribution(List<PositionStatus> userStatuses,
            List<Position> filteredPositions) {
        long total = filteredPositions.size();
        return userStatuses.stream().map(status -> {
            long count = filteredPositions.stream().filter(position -> status.id().equals(position.statusId())).count();
            double percentage = total == 0 ? 0.0 : count * 100.0 / total;
            return new StatusDistributionItem(status.id(), status.name(), status.statisticsCategory(), count, percentage);
        }).toList();
    }

    private List<JobTypeBreakdownItem> jobTypeBreakdown(List<JobType> userJobTypes,
            List<Position> filteredPositions, List<InterviewRound> filteredRounds,
            Map<Long, PositionStatus> statusesById) {
        Set<Long> interviewedPositionIds = filteredRounds.stream().map(InterviewRound::positionId)
                .collect(Collectors.toSet());
        return userJobTypes.stream().map(jobType -> {
            List<Position> positionsForType = filteredPositions.stream()
                    .filter(position -> jobType.id().equals(position.jobTypeId())).toList();
            long applicationCount = positionsForType.stream().filter(position -> position.appliedAt() != null).count();
            long interviewedPositionCount = positionsForType.stream()
                    .filter(position -> interviewedPositionIds.contains(position.id())).count();
            long offerCount = positionsForType.stream()
                    .filter(position -> isSuccessful(position, statusesById)).count();
            return new JobTypeBreakdownItem(jobType.id(), jobType.name(), applicationCount,
                    interviewedPositionCount, offerCount);
        }).toList();
    }

    private List<ApplicationTrendItem> applicationTrend(List<Position> filteredPositions,
            List<InterviewRound> filteredRounds) {
        Map<LocalDate, List<Position>> positionsByDate = filteredPositions.stream()
                .filter(position -> position.appliedAt() != null)
                .collect(Collectors.groupingBy(Position::appliedAt));
        Map<Long, Long> roundCountsByPosition = filteredRounds.stream()
                .collect(Collectors.groupingBy(InterviewRound::positionId, Collectors.counting()));
        return positionsByDate.entrySet().stream().map(entry -> {
            long roundCount = entry.getValue().stream()
                    .mapToLong(position -> roundCountsByPosition.getOrDefault(position.id(), 0L)).sum();
            return new ApplicationTrendItem(entry.getKey(), entry.getValue().size(), roundCount);
        }).sorted(Comparator.comparing(ApplicationTrendItem::date)).toList();
    }

    private ConversionMetrics conversions(List<Position> filteredPositions,
            List<InterviewRound> filteredRounds, Map<Long, PositionStatus> statusesById) {
        List<Position> appliedPositions = filteredPositions.stream()
                .filter(position -> position.appliedAt() != null).toList();
        Set<Long> interviewedPositionIds = filteredRounds.stream().map(InterviewRound::positionId)
                .collect(Collectors.toSet());
        long interviewedApplications = appliedPositions.stream()
                .filter(position -> interviewedPositionIds.contains(position.id())).count();
        long offers = appliedPositions.stream().filter(position -> isSuccessful(position, statusesById)).count();
        long decidedRounds = filteredRounds.stream()
                .filter(round -> "PASSED".equals(round.result()) || "FAILED".equals(round.result())).count();
        long passedRounds = filteredRounds.stream().filter(round -> "PASSED".equals(round.result())).count();
        return new ConversionMetrics(
                percentage(interviewedApplications, appliedPositions.size()),
                percentage(offers, appliedPositions.size()),
                percentage(passedRounds, decidedRounds));
    }

    private boolean isSuccessful(Position position, Map<Long, PositionStatus> statusesById) {
        PositionStatus status = statusesById.get(position.statusId());
        return status != null && "SUCCESS".equals(status.statisticsCategory());
    }

    private ConversionRate percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return new ConversionRate(false, null);
        }
        return new ConversionRate(true, numerator * 100.0 / denominator);
    }
}
