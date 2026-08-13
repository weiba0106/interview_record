package com.interviewrecord.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.interviewrecord.interviews.domain.InterviewRound;
import com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository;
import com.interviewrecord.scheduling.api.ScheduleDtos.ScheduleRequest;
import com.interviewrecord.scheduling.domain.ScheduleEvent;
import com.interviewrecord.scheduling.infrastructure.JpaScheduleEventRepository;
import com.interviewrecord.common.error.ConflictException;
import com.interviewrecord.tracking.domain.Position;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ScheduleServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-13T00:00:00Z");
    private static final Instant NEW_START = Instant.parse("2026-08-15T02:00:00Z");

    @Test
    void editingLinkedScheduleResynchronizesTheRoundAndItsOtherSchedules() {
        JpaScheduleEventRepository schedules = org.mockito.Mockito.mock(JpaScheduleEventRepository.class);
        JpaPositionRepository positions = org.mockito.Mockito.mock(JpaPositionRepository.class);
        JpaInterviewRoundRepository rounds = org.mockito.Mockito.mock(JpaInterviewRoundRepository.class);
        ScheduleService service = new ScheduleService(schedules, positions, rounds, fixedClock());

        Position position = position(5L);
        InterviewRound round = round(9L, 5L, NOW.plusSeconds(3600));
        ScheduleEvent edited = schedule(11L, 5L, 9L, NOW.plusSeconds(3600));
        ScheduleEvent sibling = schedule(12L, 5L, 9L, NOW.plusSeconds(3600));
        given(schedules.findByIdAndUserId(11L, 1L)).willReturn(Optional.of(edited));
        given(positions.findByIdAndUserId(5L, 1L)).willReturn(Optional.of(position));
        given(rounds.findByIdAndUserId(9L, 1L)).willReturn(Optional.of(round));
        given(schedules.findAllByUserIdAndInterviewRoundId(1L, 9L)).willReturn(List.of(edited, sibling));

        service.update(1L, 11L, new ScheduleRequest("技术一面", "INTERVIEW", NEW_START,
                NEW_START.plusSeconds(3600), "5", "9", "会议链接", null, 0L));

        assertThat(round.startsAt()).isEqualTo(NEW_START);
        assertThat(round.endsAt()).isEqualTo(NEW_START.plusSeconds(3600));
        assertThat(sibling.startsAt()).isEqualTo(NEW_START);
        assertThat(sibling.endsAt()).isEqualTo(NEW_START.plusSeconds(3600));
    }

    @Test
    void linkedRoundCreationReusesItsExistingScheduleInsteadOfCreatingAnotherOne() {
        JpaScheduleEventRepository schedules = org.mockito.Mockito.mock(JpaScheduleEventRepository.class);
        ScheduleService service = new ScheduleService(schedules,
                org.mockito.Mockito.mock(JpaPositionRepository.class),
                org.mockito.Mockito.mock(JpaInterviewRoundRepository.class), fixedClock());
        ScheduleEvent existing = schedule(11L, 5L, 9L, NOW.plusSeconds(3600));
        given(schedules.findAllByUserIdAndInterviewRoundId(1L, 9L)).willReturn(List.of(existing));

        ScheduleEvent result = service.createLinked(1L, "技术一面", "INTERVIEW", NEW_START,
                NEW_START.plusSeconds(3600), 5L, 9L, "会议链接", null);

        assertThat(result).isSameAs(existing);
        assertThat(existing.startsAt()).isEqualTo(NEW_START);
    }

    @Test
    void staleScheduleVersionIsRejectedBeforeAnyLinkedResourceChanges() {
        JpaScheduleEventRepository schedules = org.mockito.Mockito.mock(JpaScheduleEventRepository.class);
        ScheduleService service = new ScheduleService(schedules,
                org.mockito.Mockito.mock(JpaPositionRepository.class),
                org.mockito.Mockito.mock(JpaInterviewRoundRepository.class), fixedClock());
        ScheduleEvent existing = schedule(11L, null, null, NOW.plusSeconds(3600));
        given(schedules.findByIdAndUserId(11L, 1L)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(1L, 11L, new ScheduleRequest("改期", "CUSTOM", NEW_START,
                null, null, null, null, null, 99L)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("日程已被更新，请刷新后重试");
        assertThat(existing.startsAt()).isEqualTo(NOW.plusSeconds(3600));
    }

    private Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    private Position position(Long id) {
        Position position = new Position(1L, 1L, 1L, 1L, "后端开发", null, null, null,
                null, null, NOW);
        ReflectionTestUtils.setField(position, "id", id);
        return position;
    }

    private InterviewRound round(Long id, Long positionId, Instant startsAt) {
        InterviewRound round = new InterviewRound(1L, positionId, "一面", 1, "VIDEO", startsAt,
                startsAt.plusSeconds(3600), null, "UPCOMING", null, null, NOW);
        ReflectionTestUtils.setField(round, "id", id);
        return round;
    }

    private ScheduleEvent schedule(Long id, Long positionId, Long roundId, Instant startsAt) {
        ScheduleEvent event = new ScheduleEvent(1L, "技术一面", "INTERVIEW", startsAt,
                startsAt.plusSeconds(3600), positionId, roundId, null, null, NOW);
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }
}
