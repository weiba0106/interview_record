package com.interviewrecord.interviews.application;

import com.interviewrecord.common.error.ConflictException;
import com.interviewrecord.common.error.InvalidInputException;
import com.interviewrecord.common.error.NotFoundException;
import com.interviewrecord.interviews.api.InterviewDtos.QuestionResponse;
import com.interviewrecord.interviews.api.InterviewDtos.RoundRequest;
import com.interviewrecord.interviews.api.InterviewDtos.RoundResponse;
import com.interviewrecord.interviews.domain.InterviewQuestion;
import com.interviewrecord.interviews.domain.InterviewRound;
import com.interviewrecord.interviews.infrastructure.JpaInterviewQuestionRepository;
import com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository;
import com.interviewrecord.scheduling.application.ScheduleService;
import com.interviewrecord.tracking.domain.Company;
import com.interviewrecord.tracking.domain.Position;
import com.interviewrecord.tracking.infrastructure.JpaCompanyRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class InterviewService {
    private final JpaInterviewRoundRepository rounds;
    private final JpaInterviewQuestionRepository questions;
    private final JpaPositionRepository positions;
    private final JpaCompanyRepository companies;
    private final ScheduleService scheduleService;
    private final Clock clock;

    public InterviewService(JpaInterviewRoundRepository rounds, JpaInterviewQuestionRepository questions,
            JpaPositionRepository positions, JpaCompanyRepository companies,
            ScheduleService scheduleService, Clock clock) {
        this.rounds = rounds; this.questions = questions; this.positions = positions;
        this.companies = companies; this.scheduleService = scheduleService; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RoundResponse> listByPosition(Long userId, Long positionId) {
        requireOwnedPosition(userId, positionId);
        return rounds.findAllByUserIdAndPositionIdInDisplayOrder(userId, positionId).stream()
                .map(round -> toResponse(userId, round)).toList();
    }

    @Transactional(readOnly = true)
    public RoundResponse get(Long userId, Long roundId) {
        return toResponse(userId, requireOwnedRound(userId, roundId));
    }

    @Transactional
    public RoundResponse create(Long userId, Long positionId, RoundRequest request) {
        Position position = requireOwnedPosition(userId, positionId);
        requireRoundFields(request);
        requireRoundNumberAvailable(userId, positionId, request.roundNumber(), null);
        Instant now = clock.instant();
        InterviewRound round = rounds.save(new InterviewRound(userId, position.id(),
                request.roundName().trim(), request.roundNumber(), request.interviewType(),
                request.startsAt(), request.endsAt(), blankToNull(request.location()), request.result(),
                blankToNull(request.processNotes()), blankToNull(request.reviewSummary()), now));
        replaceQuestions(userId, round.id(), request);
        if (Boolean.TRUE.equals(request.createSchedule()) && round.startsAt() != null) {
            scheduleService.createLinked(userId, scheduleTitle(userId, position, round), "INTERVIEW",
                    round.startsAt(), round.endsAt(), position.id(), round.id(), round.location(), null);
        }
        return toResponse(userId, round);
    }

    @Transactional
    public RoundResponse update(Long userId, Long roundId, RoundRequest request) {
        InterviewRound round = requireOwnedRound(userId, roundId);
        if (request.version() != null && request.version() != round.version()) {
            throw new ConflictException("CONCURRENT_UPDATE", "面试记录已被更新，请刷新后重试");
        }
        requireRoundFields(request);
        requireRoundNumberAvailable(userId, round.positionId(), request.roundNumber(), roundId);
        round.update(request.roundName().trim(), request.roundNumber(), request.interviewType(),
                request.startsAt(), request.endsAt(), blankToNull(request.location()), request.result(),
                blankToNull(request.processNotes()), blankToNull(request.reviewSummary()), clock.instant());
        replaceQuestions(userId, round.id(), request);
        scheduleService.syncFromRound(userId, round);
        return toResponse(userId, round);
    }

    @Transactional
    public void delete(Long userId, Long roundId) {
        InterviewRound round = requireOwnedRound(userId, roundId);
        scheduleService.deleteLinkedToRound(userId, round.id());
        questions.deleteByUserIdAndRoundId(userId, round.id());
        rounds.delete(round);
    }

    private InterviewRound requireOwnedRound(Long userId, Long roundId) {
        return rounds.findByIdAndUserId(roundId, userId).orElseThrow(NotFoundException::new);
    }

    private Position requireOwnedPosition(Long userId, Long positionId) {
        return positions.findByIdAndUserId(positionId, userId).orElseThrow(NotFoundException::new);
    }

    private void requireRoundFields(RoundRequest request) {
        if (!InterviewRound.TYPES.contains(request.interviewType())) {
            throw new InvalidInputException("INVALID_INTERVIEW_TYPE", "不支持的面试类型");
        }
        if (!InterviewRound.RESULTS.contains(request.result())) {
            throw new InvalidInputException("INVALID_INTERVIEW_RESULT", "不支持的面试结果");
        }
        if (request.startsAt() != null && request.endsAt() != null
                && request.endsAt().isBefore(request.startsAt())) {
            throw new InvalidInputException("ENDS_BEFORE_STARTS", "结束时间不能早于开始时间");
        }
    }

    private void requireRoundNumberAvailable(Long userId, Long positionId, int roundNumber, Long excludeRoundId) {
        boolean taken = rounds.findAllByUserIdAndPositionId(userId, positionId).stream()
                .anyMatch(round -> round.roundNumber() == roundNumber
                        && (excludeRoundId == null || !excludeRoundId.equals(round.id())));
        if (taken) {
            throw new ConflictException("ROUND_NUMBER_TAKEN", "该岗位已存在相同轮次序号的面试");
        }
    }

    private void replaceQuestions(Long userId, Long roundId, RoundRequest request) {
        questions.deleteByUserIdAndRoundId(userId, roundId);
        if (request.questions() == null || request.questions().isEmpty()) {
            return;
        }
        Instant now = clock.instant();
        int order = 1;
        for (var item : request.questions()) {
            questions.save(new InterviewQuestion(userId, roundId, order++, item.question().trim(),
                    blankToNull(item.answer()), blankToNull(item.category()), now));
        }
    }

    private String scheduleTitle(Long userId, Position position, InterviewRound round) {
        String companyName = companies.findByIdAndUserId(position.companyId(), userId)
                .map(Company::name).orElse("");
        return "面试：" + companyName + " " + position.title() + " 第" + round.roundNumber() + "轮";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private RoundResponse toResponse(Long userId, InterviewRound round) {
        Position position = positions.findByIdAndUserId(round.positionId(), userId).orElse(null);
        String companyName = position == null ? "" : companies.findByIdAndUserId(position.companyId(), userId)
                .map(Company::name).orElse("");
        List<QuestionResponse> questionItems = questions
                .findAllByUserIdAndRoundIdOrderBySortOrderAsc(userId, round.id()).stream()
                .map(question -> new QuestionResponse(question.sortOrder(), question.question(),
                        question.answer(), question.category()))
                .toList();
        List<String> scheduleIds = scheduleService.findRoundScheduleIds(userId, round.id());
        return new RoundResponse(Long.toString(round.id()), Long.toString(round.positionId()),
                position == null ? "" : position.title(), companyName, round.roundName(), round.roundNumber(),
                round.interviewType(), round.startsAt(), round.endsAt(), round.location(), round.result(),
                round.processNotes(), round.reviewSummary(), questionItems, scheduleIds,
                round.version(), round.createdAt(), round.updatedAt());
    }
}
