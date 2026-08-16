package com.interviewrecord.tracking.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.interviewrecord.common.html.RichTextSanitizer;
import com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository;
import com.interviewrecord.scheduling.application.ScheduleService;
import com.interviewrecord.scheduling.domain.ScheduleEvent;
import com.interviewrecord.scheduling.infrastructure.JpaScheduleEventRepository;
import com.interviewrecord.tracking.api.TrackingDtos.PositionListResponse;
import com.interviewrecord.tracking.domain.Position;
import com.interviewrecord.tracking.infrastructure.JpaCompanyRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedJobTypeRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedPositionStatusRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class PositionServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");

    private Position position(long id, Instant updatedAt) {
        Position position = new Position(1L, 1L, 1L, 1L, "岗位-" + id, null, null,
                null, null, null, NOW);
        ReflectionTestUtils.setField(position, "id", id);
        ReflectionTestUtils.setField(position, "updatedAt", updatedAt);
        return position;
    }

    private ScheduleEvent pendingEvent(long positionId, Instant time) {
        ScheduleEvent event = new ScheduleEvent(1L, "日程", "INTERVIEW", time, null,
                positionId, null, null, null, NOW);
        ReflectionTestUtils.setField(event, "id", positionId);
        return event;
    }

    private PositionService service(JpaPositionRepository positions, JpaScheduleEventRepository schedules) {
        JpaCompanyRepository companies = org.mockito.Mockito.mock(JpaCompanyRepository.class);
        JpaManagedJobTypeRepository jobTypes = org.mockito.Mockito.mock(JpaManagedJobTypeRepository.class);
        JpaManagedPositionStatusRepository statuses = org.mockito.Mockito.mock(JpaManagedPositionStatusRepository.class);
        JpaInterviewRoundRepository rounds = org.mockito.Mockito.mock(JpaInterviewRoundRepository.class);
        ScheduleService scheduleService = org.mockito.Mockito.mock(ScheduleService.class);
        given(companies.findAllByUserIdOrderByUpdatedAtDesc(1L)).willReturn(List.of());
        given(jobTypes.findAllByUserIdOrderByIdAsc(1L)).willReturn(List.of());
        given(statuses.findAllByUserIdOrderBySortOrderAsc(1L)).willReturn(List.of());
        given(rounds.countByUserIdGroupedByPosition(eq(1L), any())).willReturn(List.of());
        given(schedules.countByUserIdGroupedByPosition(eq(1L), any())).willReturn(List.of());
        given(schedules.findPendingForPositionsFrom(eq(1L), any(), any())).willReturn(List.of());
        return new PositionService(positions, companies, jobTypes, statuses, rounds, schedules,
                scheduleService, new RichTextSanitizer(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void nextScheduleSortOrdersByEarliestPendingScheduleWithNullsLastAndPaginates() {
        JpaPositionRepository positions = org.mockito.Mockito.mock(JpaPositionRepository.class);
        JpaScheduleEventRepository schedules = org.mockito.Mockito.mock(JpaScheduleEventRepository.class);
        Position soon = position(1L, NOW.minusSeconds(3600));
        Position noSchedule = position(2L, NOW.minusSeconds(1800));
        Position later = position(3L, NOW.minusSeconds(900));
        given(positions.search(eq(1L), eq(null), eq(null), eq(null), eq(false),
                eq(null), eq(null), eq(null), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(soon, noSchedule, later)));
        given(schedules.findPendingForPositions(eq(1L), any()))
                .willReturn(List.of(
                        pendingEvent(3L, NOW.plusSeconds(7200)),
                        pendingEvent(1L, NOW.plusSeconds(3600))));

        PositionListResponse page = service(positions, schedules).search(1L, null, null, null,
                false, null, null, null, 0, 2, "nextSchedule", "asc");

        assertThat(page.totalItems()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
        assertThat(page.items()).extracting(item -> item.title())
                .containsExactly("岗位-1", "岗位-3");

        PositionListResponse second = service(positions, schedules).search(1L, null, null, null,
                false, null, null, null, 1, 2, "nextSchedule", "asc");
        assertThat(second.items()).extracting(item -> item.title()).containsExactly("岗位-2");
    }
}
