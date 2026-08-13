package com.interviewrecord.scheduling.application;

import com.interviewrecord.common.error.InvalidInputException;
import com.interviewrecord.common.error.ConflictException;
import com.interviewrecord.common.error.NotFoundException;
import com.interviewrecord.common.util.ResourceIds;
import com.interviewrecord.interviews.domain.InterviewRound;
import com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository;
import com.interviewrecord.scheduling.api.ScheduleDtos.ScheduleRequest;
import com.interviewrecord.scheduling.api.ScheduleDtos.ScheduleResponse;
import com.interviewrecord.scheduling.domain.ScheduleEvent;
import com.interviewrecord.scheduling.domain.Urgency;
import com.interviewrecord.scheduling.infrastructure.JpaScheduleEventRepository;
import com.interviewrecord.tracking.domain.Position;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class ScheduleService {
    private static final Set<String> STATUSES = Set.of(
            ScheduleEvent.STATUS_PENDING, ScheduleEvent.STATUS_COMPLETED, ScheduleEvent.STATUS_CANCELLED);
    private static final Set<String> MANUAL_URGENCIES = Set.of("URGENT", "APPROACHING", "NORMAL");
    private static final int LIST_LIMIT = 200;

    private final JpaScheduleEventRepository schedules;
    private final JpaPositionRepository positions;
    private final JpaInterviewRoundRepository rounds;
    private final Clock clock;

    public ScheduleService(JpaScheduleEventRepository schedules, JpaPositionRepository positions,
            JpaInterviewRoundRepository rounds, Clock clock) {
        this.schedules = schedules; this.positions = positions; this.rounds = rounds; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(Long userId, String status) {
        List<ScheduleEvent> events;
        if (status != null) {
            requireValidStatus(status);
            events = schedules.findAllByUserIdAndStatus(userId, status);
        } else {
            events = schedules.findAllByUserId(userId);
        }
        Instant now = clock.instant();
        Comparator<ScheduleEvent> pendingFirst = Comparator
                .comparing((ScheduleEvent event) -> event.pending() ? 0 : 1)
                .thenComparing(event -> event.referenceTime() == null ? Instant.MAX : event.referenceTime());
        return events.stream().sorted(pendingFirst).limit(LIST_LIMIT)
                .map(event -> toResponse(event, now, positionTitles(userId, events)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleResponse get(Long userId, Long scheduleId) {
        ScheduleEvent event = requireOwned(userId, scheduleId);
        return toResponse(event, clock.instant(), positionTitles(userId, List.of(event)));
    }

    @Transactional
    public ScheduleResponse create(Long userId, ScheduleRequest request) {
        ScheduleEvent event = buildFromRequest(userId, request);
        ScheduleEvent saved = schedules.save(event);
        return toResponse(saved, clock.instant(), positionTitles(userId, List.of(saved)));
    }

    /** Internal creation path reused by position deadlines and interview rounds. */
    @Transactional
    public ScheduleEvent createLinked(Long userId, String title, String eventType, Instant startsAt,
            Instant endsAt, Long positionId, Long roundId, String location, String notes) {
        requireEventType(eventType);
        requireTimes(startsAt, endsAt);
        if (roundId != null) {
            List<ScheduleEvent> linked = schedules.findAllByUserIdAndInterviewRoundId(userId, roundId);
            if (!linked.isEmpty()) {
                ScheduleEvent existing = linked.getFirst();
                existing.reschedule(startsAt, endsAt, clock.instant());
                return existing;
            }
        }
        return schedules.save(new ScheduleEvent(userId, title, eventType, startsAt, endsAt,
                positionId, roundId, location, notes, clock.instant()));
    }

    @Transactional
    public ScheduleResponse update(Long userId, Long scheduleId, ScheduleRequest request) {
        ScheduleEvent event = requireOwned(userId, scheduleId);
        if (request.version() == null || request.version() != event.version()) {
            throw new ConflictException("CONCURRENT_UPDATE", "日程已被更新，请刷新后重试");
        }
        requireEventType(request.eventType());
        requireTimes(request.startsAt(), request.endsAt());
        ResolvedLinks links = resolveLinks(userId, request.positionId(), request.interviewRoundId());
        event.update(request.title().trim(), request.eventType(), request.startsAt(), request.endsAt(),
                links.positionId(), links.roundId(), blankToNull(request.location()),
                blankToNull(request.notes()), clock.instant());
        if (event.interviewRoundId() != null) {
            syncRoundFromSchedule(userId, event);
        }
        return toResponse(event, clock.instant(), positionTitles(userId, List.of(event)));
    }

    @Transactional
    public ScheduleResponse changeStatus(Long userId, Long scheduleId, String status) {
        requireValidStatus(status);
        ScheduleEvent event = requireOwned(userId, scheduleId);
        event.changeStatus(status, clock.instant());
        return toResponse(event, clock.instant(), positionTitles(userId, List.of(event)));
    }

    /** Pass a null urgency to clear the manual override and restore automation. */
    @Transactional
    public ScheduleResponse overrideUrgency(Long userId, Long scheduleId, String urgency) {
        if (urgency != null && !MANUAL_URGENCIES.contains(urgency)) {
            throw new InvalidInputException("INVALID_URGENCY", "紧急程度只能是 URGENT、APPROACHING 或 NORMAL");
        }
        ScheduleEvent event = requireOwned(userId, scheduleId);
        if (!event.pending()) {
            throw new InvalidInputException("SCHEDULE_NOT_PENDING", "只能为待处理日程设置紧急程度");
        }
        event.overrideUrgency(urgency, clock.instant());
        return toResponse(event, clock.instant(), positionTitles(userId, List.of(event)));
    }

    @Transactional
    public void delete(Long userId, Long scheduleId) {
        schedules.delete(requireOwned(userId, scheduleId));
    }

    /** Keeps schedules attached to a round consistent when the round time changes. */
    @Transactional
    public void syncFromRound(Long userId, InterviewRound round) {
        Instant now = clock.instant();
        schedules.findAllByUserIdAndInterviewRoundId(userId, round.id())
                .forEach(event -> event.reschedule(round.startsAt(), round.endsAt(), now));
    }

    private void syncRoundFromSchedule(Long userId, ScheduleEvent event) {
        InterviewRound round = rounds.findByIdAndUserId(event.interviewRoundId(), userId)
                .orElseThrow(NotFoundException::new);
        Instant now = clock.instant();
        round.reschedule(event.startsAt(), event.endsAt(), now);
        schedules.findAllByUserIdAndInterviewRoundId(userId, round.id())
                .forEach(linked -> linked.reschedule(round.startsAt(), round.endsAt(), now));
    }

    @Transactional(readOnly = true)
    public List<String> findRoundScheduleIds(Long userId, Long roundId) {
        return schedules.findAllByUserIdAndInterviewRoundId(userId, roundId).stream()
                .map(event -> Long.toString(event.id())).toList();
    }

    /** Removes schedules before the owning interview round is deleted. */
    @Transactional
    public void deleteLinkedToRound(Long userId, Long roundId) {
        schedules.findAllByUserIdAndInterviewRoundId(userId, roundId).forEach(schedules::delete);
    }

    /** Overdue pending events first, then the soonest events within the next 7 days. */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> upcomingForDashboard(Long userId, int limit) {
        Instant now = clock.instant();
        Instant horizon = now.plus(java.time.Duration.ofDays(7));
        List<ScheduleEvent> candidates = schedules.findAllByUserIdAndStatus(userId, ScheduleEvent.STATUS_PENDING)
                .stream()
                .filter(event -> event.referenceTime() != null
                        && (event.referenceTime().isBefore(now) || !event.referenceTime().isAfter(horizon)))
                .sorted(Comparator.comparing((ScheduleEvent event) -> event.referenceTime().isBefore(now) ? 0 : 1)
                        .thenComparing(ScheduleEvent::referenceTime))
                .limit(limit)
                .toList();
        Map<Long, String> titles = positionTitles(userId, candidates);
        return candidates.stream().map(event -> toResponse(event, now, titles)).toList();
    }

    @Transactional(readOnly = true)
    public long countPendingWithin(Long userId, Instant from, Instant to) {
        return schedules.findAllByUserIdAndStatus(userId, ScheduleEvent.STATUS_PENDING).stream()
                .filter(event -> event.referenceTime() != null
                        && !event.referenceTime().isBefore(from) && !event.referenceTime().isAfter(to))
                .count();
    }

    private ScheduleEvent buildFromRequest(Long userId, ScheduleRequest request) {
        requireEventType(request.eventType());
        requireTimes(request.startsAt(), request.endsAt());
        ResolvedLinks links = resolveLinks(userId, request.positionId(), request.interviewRoundId());
        return new ScheduleEvent(userId, request.title().trim(), request.eventType(), request.startsAt(),
                request.endsAt(), links.positionId(), links.roundId(), blankToNull(request.location()),
                blankToNull(request.notes()), clock.instant());
    }

    private record ResolvedLinks(Long positionId, Long roundId) {}

    private ResolvedLinks resolveLinks(Long userId, String positionIdRaw, String roundIdRaw) {
        Long positionId = null;
        if (positionIdRaw != null && !positionIdRaw.isBlank()) {
            Position position = positions.findByIdAndUserId(ResourceIds.parse(positionIdRaw), userId)
                    .orElseThrow(NotFoundException::new);
            positionId = position.id();
        }
        Long roundId = null;
        if (roundIdRaw != null && !roundIdRaw.isBlank()) {
            InterviewRound round = rounds.findByIdAndUserId(ResourceIds.parse(roundIdRaw), userId)
                    .orElseThrow(NotFoundException::new);
            if (positionId != null && !positionId.equals(round.positionId())) {
                throw new InvalidInputException("ROUND_POSITION_MISMATCH", "面试轮次不属于所选岗位");
            }
            roundId = round.id();
            positionId = round.positionId();
        }
        return new ResolvedLinks(positionId, roundId);
    }

    private ScheduleEvent requireOwned(Long userId, Long scheduleId) {
        return schedules.findByIdAndUserId(scheduleId, userId).orElseThrow(NotFoundException::new);
    }

    private void requireEventType(String eventType) {
        if (!ScheduleEvent.EVENT_TYPES.contains(eventType)) {
            throw new InvalidInputException("INVALID_EVENT_TYPE", "不支持的日程类型");
        }
    }

    private void requireTimes(Instant startsAt, Instant endsAt) {
        if (startsAt == null && endsAt == null) {
            throw new InvalidInputException("MISSING_EVENT_TIME", "开始时间或截止时间至少填写一项");
        }
        if (startsAt != null && endsAt != null && endsAt.isBefore(startsAt)) {
            throw new InvalidInputException("ENDS_BEFORE_STARTS", "结束时间不能早于开始时间");
        }
    }

    private void requireValidStatus(String status) {
        if (!STATUSES.contains(status)) {
            throw new InvalidInputException("INVALID_SCHEDULE_STATUS", "日程状态只能是 PENDING、COMPLETED 或 CANCELLED");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Map<Long, String> positionTitles(Long userId, List<ScheduleEvent> events) {
        List<Long> ids = events.stream().map(ScheduleEvent::positionId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> titles = new HashMap<>();
        if (!ids.isEmpty()) {
            positions.findAllById(ids).stream()
                    .filter(position -> userId.equals(position.userId()))
                    .forEach(position -> titles.put(position.id(), position.title()));
        }
        return titles;
    }

    private ScheduleResponse toResponse(ScheduleEvent event, Instant now, Map<Long, String> titles) {
        Urgency urgency = event.urgency(now);
        boolean overdue = event.pending() && event.referenceTime() != null && event.referenceTime().isBefore(now);
        return new ScheduleResponse(Long.toString(event.id()), event.title(), event.eventType(),
                event.startsAt(), event.endsAt(),
                event.positionId() == null ? null : Long.toString(event.positionId()),
                event.positionId() == null ? null : titles.getOrDefault(event.positionId(), ""),
                event.interviewRoundId() == null ? null : Long.toString(event.interviewRoundId()),
                event.location(), event.notes(), event.status(), urgency.name(), overdue,
                event.manualUrgency(), event.referenceTime(), event.version(), event.updatedAt());
    }
}
