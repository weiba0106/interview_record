package com.interviewrecord.insights.api;

import java.time.LocalDate;
import java.util.List;

/** Public response shapes for the authenticated insights endpoint. */
public final class InsightDtos {
    private InsightDtos() {}

    public record InsightFilter(Long jobTypeId, LocalDate appliedFrom, LocalDate appliedTo) {}

    public record InsightsResponse(
            List<StatusDistributionItem> statusDistribution,
            List<JobTypeBreakdownItem> jobTypeBreakdown,
            List<ApplicationTrendItem> applicationTrend,
            ConversionMetrics conversions) {}

    public record StatusDistributionItem(
            Long statusId, String statusName, String statisticsCategory, long count, double percentage) {}

    public record JobTypeBreakdownItem(
            Long jobTypeId, String jobTypeName, long applicationCount,
            long interviewedPositionCount, long offerCount) {}

    public record ApplicationTrendItem(LocalDate date, long applicationCount, long interviewRoundCount) {}

    public record ConversionMetrics(
            ConversionRate interviewReachRate,
            ConversionRate offerConversionRate,
            ConversionRate interviewPassRate) {}

    /** A null percentage means the metric has no valid denominator. */
    public record ConversionRate(boolean available, Double percentage) {}
}
