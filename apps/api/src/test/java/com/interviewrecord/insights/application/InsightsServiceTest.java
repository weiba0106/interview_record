package com.interviewrecord.insights.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.interviewrecord.insights.api.InsightDtos.InsightFilter;
import com.interviewrecord.insights.api.InsightDtos.InsightsResponse;
import com.interviewrecord.interviews.domain.InterviewRound;
import com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository;
import com.interviewrecord.tracking.domain.JobType;
import com.interviewrecord.tracking.domain.Position;
import com.interviewrecord.tracking.domain.PositionStatus;
import com.interviewrecord.tracking.infrastructure.JpaManagedJobTypeRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedPositionStatusRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class InsightsServiceTest {
    private final JpaPositionRepository positions = mock(JpaPositionRepository.class);
    private final JpaInterviewRoundRepository rounds = mock(JpaInterviewRoundRepository.class);
    private final JpaManagedPositionStatusRepository statuses = mock(JpaManagedPositionStatusRepository.class);
    private final JpaManagedJobTypeRepository jobTypes = mock(JpaManagedJobTypeRepository.class);
    private final InsightsService service = new InsightsService(positions, rounds, statuses, jobTypes);

    @Test
    void calculatesStatusTypeTrendAndConversionMetricsWithinTheUsersFilter() {
        Position active = position(1L, 10L, 100L, LocalDate.of(2026, 8, 1));
        Position offer = position(2L, 10L, 101L, LocalDate.of(2026, 8, 2));
        Position outsideDateRange = position(3L, 11L, 100L, LocalDate.of(2026, 7, 31));
        Position withoutApplicationDate = position(4L, 10L, 100L, null);
        PositionStatus activeStatus = status(100L, "进行中", "ACTIVE");
        PositionStatus offerStatus = status(101L, "Offer", "SUCCESS");
        JobType autumnJobType = jobType(10L, "秋招");
        JobType internshipJobType = jobType(11L, "日常实习");
        InterviewRound passedFirstRound = round(1L, "PASSED");
        InterviewRound failedFirstRound = round(1L, "FAILED");
        InterviewRound passedSecondRound = round(2L, "PASSED");
        InterviewRound passedThirdRound = round(3L, "PASSED");
        given(positions.findAllByUserId(42L)).willReturn(List.of(active, offer, outsideDateRange, withoutApplicationDate));
        given(statuses.findAllByUserIdOrderBySortOrderAsc(42L)).willReturn(List.of(activeStatus, offerStatus));
        given(jobTypes.findAllByUserIdOrderByIdAsc(42L)).willReturn(List.of(autumnJobType, internshipJobType));
        given(rounds.findAllByUserId(42L)).willReturn(List.of(
                passedFirstRound, failedFirstRound, passedSecondRound, passedThirdRound));

        InsightsResponse response = service.getInsights(42L,
                new InsightFilter(null, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)));

        assertThat(response.statusDistribution()).extracting(item -> item.count())
                .containsExactly(1L, 1L);
        assertThat(response.statusDistribution()).extracting(item -> item.percentage())
                .containsExactly(50.0, 50.0);
        assertThat(response.jobTypeBreakdown()).extracting(item -> item.applicationCount())
                .containsExactly(2L, 0L);
        assertThat(response.jobTypeBreakdown()).extracting(item -> item.interviewedPositionCount())
                .containsExactly(2L, 0L);
        assertThat(response.jobTypeBreakdown()).extracting(item -> item.offerCount())
                .containsExactly(1L, 0L);
        assertThat(response.applicationTrend()).hasSize(2);
        assertThat(response.applicationTrend().getFirst()).satisfies(item -> {
            assertThat(item.date()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(item.applicationCount()).isEqualTo(1L);
            assertThat(item.interviewRoundCount()).isEqualTo(2L);
        });
        assertThat(response.applicationTrend().get(1)).satisfies(item -> {
            assertThat(item.date()).isEqualTo(LocalDate.of(2026, 8, 2));
            assertThat(item.applicationCount()).isEqualTo(1L);
            assertThat(item.interviewRoundCount()).isEqualTo(1L);
        });
        assertThat(response.conversions().interviewReachRate().available()).isTrue();
        assertThat(response.conversions().interviewReachRate().percentage()).isEqualTo(100.0);
        assertThat(response.conversions().offerConversionRate().percentage()).isEqualTo(50.0);
        assertThat(response.conversions().interviewPassRate().percentage()).isEqualTo(200.0 / 3.0);
        verify(positions).findAllByUserId(42L);
        verify(rounds).findAllByUserId(42L);
        verify(statuses).findAllByUserIdOrderBySortOrderAsc(42L);
        verify(jobTypes).findAllByUserIdOrderByIdAsc(42L);
    }

    @Test
    void filtersEveryMetricByRecruitmentType() {
        Position autumn = position(1L, 10L, 100L, LocalDate.of(2026, 8, 1));
        Position internship = position(2L, 11L, 101L, LocalDate.of(2026, 8, 2));
        PositionStatus activeStatus = status(100L, "进行中", "ACTIVE");
        PositionStatus offerStatus = status(101L, "Offer", "SUCCESS");
        JobType autumnJobType = jobType(10L, "秋招");
        JobType internshipJobType = jobType(11L, "实习");
        InterviewRound passedRound = round(1L, "PASSED");
        InterviewRound failedRound = round(2L, "FAILED");
        given(positions.findAllByUserId(42L)).willReturn(List.of(autumn, internship));
        given(statuses.findAllByUserIdOrderBySortOrderAsc(42L)).willReturn(List.of(activeStatus, offerStatus));
        given(jobTypes.findAllByUserIdOrderByIdAsc(42L)).willReturn(List.of(autumnJobType, internshipJobType));
        given(rounds.findAllByUserId(42L)).willReturn(List.of(passedRound, failedRound));

        InsightsResponse response = service.getInsights(42L, new InsightFilter(10L, null, null));

        assertThat(response.statusDistribution()).extracting(item -> item.count()).containsExactly(1L, 0L);
        assertThat(response.jobTypeBreakdown()).extracting(item -> item.applicationCount()).containsExactly(1L, 0L);
        assertThat(response.conversions().offerConversionRate().percentage()).isEqualTo(0.0);
        assertThat(response.conversions().interviewPassRate().percentage()).isEqualTo(100.0);
    }

    @Test
    void marksAConversionUnavailableWhenItsDenominatorIsZero() {
        Position position = position(1L, 10L, 100L, null);
        PositionStatus activeStatus = status(100L, "进行中", "ACTIVE");
        JobType autumnJobType = jobType(10L, "秋招");
        InterviewRound upcomingRound = round(1L, "UPCOMING");
        given(positions.findAllByUserId(42L)).willReturn(List.of(position));
        given(statuses.findAllByUserIdOrderBySortOrderAsc(42L)).willReturn(List.of(activeStatus));
        given(jobTypes.findAllByUserIdOrderByIdAsc(42L)).willReturn(List.of(autumnJobType));
        given(rounds.findAllByUserId(42L)).willReturn(List.of(upcomingRound));

        InsightsResponse response = service.getInsights(42L, new InsightFilter(null, null, null));

        assertThat(response.conversions().interviewReachRate().available()).isFalse();
        assertThat(response.conversions().interviewReachRate().percentage()).isNull();
        assertThat(response.conversions().offerConversionRate().available()).isFalse();
        assertThat(response.conversions().interviewPassRate().available()).isFalse();
    }

    private Position position(Long id, Long jobTypeId, Long statusId, LocalDate appliedAt) {
        Position position = mock(Position.class);
        given(position.id()).willReturn(id);
        given(position.jobTypeId()).willReturn(jobTypeId);
        given(position.statusId()).willReturn(statusId);
        given(position.appliedAt()).willReturn(appliedAt);
        return position;
    }

    private PositionStatus status(Long id, String name, String category) {
        PositionStatus status = mock(PositionStatus.class);
        given(status.id()).willReturn(id);
        given(status.name()).willReturn(name);
        given(status.statisticsCategory()).willReturn(category);
        return status;
    }

    private JobType jobType(Long id, String name) {
        JobType jobType = mock(JobType.class);
        given(jobType.id()).willReturn(id);
        given(jobType.name()).willReturn(name);
        return jobType;
    }

    private InterviewRound round(Long positionId, String result) {
        InterviewRound round = mock(InterviewRound.class);
        given(round.positionId()).willReturn(positionId);
        given(round.result()).willReturn(result);
        return round;
    }
}
