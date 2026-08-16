package com.interviewrecord.sharing.application;

import com.interviewrecord.auth.application.RateLimitService;
import com.interviewrecord.common.error.InvalidInputException;
import com.interviewrecord.common.error.NotFoundException;
import com.interviewrecord.common.token.IssuedToken;
import com.interviewrecord.common.token.SecureTokenService;
import com.interviewrecord.interviews.domain.InterviewQuestion;
import com.interviewrecord.interviews.domain.InterviewRound;
import com.interviewrecord.interviews.infrastructure.JpaInterviewQuestionRepository;
import com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository;
import com.interviewrecord.sharing.api.SharingDtos;
import com.interviewrecord.sharing.domain.ShareLink;
import com.interviewrecord.sharing.domain.ShareRound;
import com.interviewrecord.sharing.infrastructure.JpaShareLinkRepository;
import com.interviewrecord.sharing.infrastructure.JpaShareRoundRepository;
import com.interviewrecord.tracking.domain.Company;
import com.interviewrecord.tracking.domain.JobType;
import com.interviewrecord.tracking.domain.Position;
import com.interviewrecord.tracking.domain.PositionStatus;
import com.interviewrecord.tracking.infrastructure.JpaCompanyRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedJobTypeRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedPositionStatusRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class SharingService {
    private static final Set<String> POSITION_FIELDS = Set.of("COMPANY_NAME", "POSITION_TITLE", "JOB_TYPE", "STATUS");
    private static final Set<String> ROUND_FIELDS = Set.of("BASIC_INFO", "QUESTIONS", "ANSWERS", "PROCESS", "REVIEW", "RESULT");
    private final JpaShareLinkRepository links;
    private final JpaShareRoundRepository shareRounds;
    private final JpaPositionRepository positions;
    private final JpaCompanyRepository companies;
    private final JpaManagedJobTypeRepository jobTypes;
    private final JpaManagedPositionStatusRepository statuses;
    private final JpaInterviewRoundRepository rounds;
    private final JpaInterviewQuestionRepository questions;
    private final SecureTokenService tokens;
    private final RateLimitService rateLimits;
    private final Clock clock;

    public SharingService(JpaShareLinkRepository links, JpaShareRoundRepository shareRounds,
            JpaPositionRepository positions, JpaCompanyRepository companies, JpaManagedJobTypeRepository jobTypes,
            JpaManagedPositionStatusRepository statuses, JpaInterviewRoundRepository rounds,
            JpaInterviewQuestionRepository questions, SecureTokenService tokens, RateLimitService rateLimits, Clock clock) {
        this.links = links; this.shareRounds = shareRounds; this.positions = positions; this.companies = companies;
        this.jobTypes = jobTypes; this.statuses = statuses; this.rounds = rounds; this.questions = questions;
        this.tokens = tokens; this.rateLimits = rateLimits; this.clock = clock;
    }

    @Transactional
    public SharingDtos.CreatedShareResponse create(Long userId, Long positionId, SharingDtos.CreateShareRequest request) {
        Position position = positions.findByIdAndUserId(positionId, userId).orElseThrow(NotFoundException::new);
        validateSubset(request.positionFields(), POSITION_FIELDS, "INVALID_SHARE_POSITION_FIELDS");
        Instant now = clock.instant();
        IssuedToken issued = tokens.issue(expiry(request.expiry()));
        Instant expiresAt = "PERMANENT".equals(request.expiry()) ? null : issued.expiresAt();
        ShareLink link = links.save(new ShareLink(userId, position.id(), issued.sha256(), request.positionFields(), expiresAt, now));
        for (SharingDtos.RoundSelection selection : request.rounds()) {
            validateSubset(selection.visibleFields(), ROUND_FIELDS, "INVALID_SHARE_ROUND_FIELDS");
            InterviewRound round = rounds.findByIdAndUserId(parseId(selection.roundId()), userId)
                    .orElseThrow(NotFoundException::new);
            if (!position.id().equals(round.positionId())) throw new NotFoundException();
            shareRounds.save(new ShareRound(link.id(), round.id(), selection.visibleFields(), now));
        }
        return new SharingDtos.CreatedShareResponse(Long.toString(link.id()), issued.rawValue(), expiresAt,
                "/api/v1/shares/" + issued.rawValue());
    }

    @Transactional(readOnly = true)
    public List<SharingDtos.ShareLinkResponse> list(Long userId, Long positionId) {
        positions.findByIdAndUserId(positionId, userId).orElseThrow(NotFoundException::new);
        return links.findAllByUserIdAndPositionIdOrderByCreatedAtDesc(userId, positionId).stream()
                .map(this::toPrivateResponse).toList();
    }

    @Transactional
    public void revoke(Long userId, Long positionId, Long shareId) {
        positions.findByIdAndUserId(positionId, userId).orElseThrow(NotFoundException::new);
        ShareLink link = links.findByIdAndUserId(shareId, userId).filter(value -> value.positionId().equals(positionId))
                .orElseThrow(NotFoundException::new);
        link.revoke(clock.instant());
    }

    @Transactional(readOnly = true)
    public SharingDtos.PublicShareResponse getPublic(String rawToken, String remoteAddress) {
        rateLimits.check("PUBLIC_SHARE_VIEW", remoteAddress, 120, Duration.ofMinutes(1), Duration.ofMinutes(5));
        ShareLink link = links.findByTokenHash(tokens.sha256(rawToken)).orElseThrow(NotFoundException::new);
        Instant now = clock.instant();
        if (!link.isActiveAt(now)) throw new NotFoundException();
        Position position = positions.findByIdAndUserId(link.positionId(), link.userId()).orElseThrow(NotFoundException::new);
        return new SharingDtos.PublicShareResponse(positionContent(link, position), publicRounds(link, position), "noindex, nofollow");
    }

    private SharingDtos.ShareLinkResponse toPrivateResponse(ShareLink link) {
        List<SharingDtos.RoundSelection> selections = shareRounds.findAllByShareIdOrderByIdAsc(link.id()).stream()
                .map(value -> new SharingDtos.RoundSelection(Long.toString(value.roundId()), value.visibleFields())).toList();
        return new SharingDtos.ShareLinkResponse(Long.toString(link.id()), link.positionFields(), selections,
                link.expiresAt(), link.revokedAt(), link.createdAt());
    }

    private Map<String, Object> positionContent(ShareLink link, Position position) {
        Map<String, Object> result = new LinkedHashMap<>();
        Set<String> fields = link.positionFields();
        if (fields.contains("COMPANY_NAME")) {
            Company company = companies.findByIdAndUserId(position.companyId(), link.userId()).orElseThrow(NotFoundException::new);
            result.put("companyName", company.name());
        }
        if (fields.contains("POSITION_TITLE")) result.put("positionTitle", position.title());
        if (fields.contains("JOB_TYPE")) {
            JobType jobType = jobTypes.findByIdAndUserId(position.jobTypeId(), link.userId()).orElseThrow(NotFoundException::new);
            result.put("jobType", jobType.name());
        }
        if (fields.contains("STATUS")) {
            PositionStatus status = statuses.findByIdAndUserId(position.statusId(), link.userId()).orElseThrow(NotFoundException::new);
            result.put("status", status.name());
        }
        return result;
    }

    private List<SharingDtos.PublicRoundResponse> publicRounds(ShareLink link, Position position) {
        List<SharingDtos.PublicRoundResponse> result = new ArrayList<>();
        for (ShareRound selection : shareRounds.findAllByShareIdOrderByIdAsc(link.id())) {
            InterviewRound round = rounds.findByIdAndUserId(selection.roundId(), link.userId()).orElseThrow(NotFoundException::new);
            if (!position.id().equals(round.positionId())) throw new NotFoundException();
            Map<String, Object> content = new LinkedHashMap<>();
            Set<String> fields = selection.visibleFields();
            if (fields.contains("BASIC_INFO")) {
                Map<String, Object> basicInfo = new LinkedHashMap<>();
                basicInfo.put("roundName", round.roundName());
                basicInfo.put("roundNumber", round.roundNumber());
                basicInfo.put("interviewType", round.interviewType());
                basicInfo.put("startsAt", round.startsAt());
                basicInfo.put("endsAt", round.endsAt());
                basicInfo.put("location", round.location());
                content.put("basicInfo", basicInfo);
            }
            List<InterviewQuestion> savedQuestions = null;
            if (fields.contains("QUESTIONS") || fields.contains("ANSWERS")) {
                savedQuestions = questions.findAllByUserIdAndRoundIdOrderBySortOrderAsc(link.userId(), round.id());
            }
            if (fields.contains("QUESTIONS")) content.put("questions", savedQuestions.stream()
                    .map(question -> {
                        Map<String, Object> questionValue = new LinkedHashMap<>();
                        questionValue.put("sortOrder", question.sortOrder());
                        questionValue.put("question", question.question());
                        questionValue.put("category", question.category());
                        return questionValue;
                    }).toList());
            if (fields.contains("ANSWERS")) content.put("answers", savedQuestions.stream()
                    .map(question -> {
                        Map<String, Object> answer = new LinkedHashMap<>();
                        answer.put("sortOrder", question.sortOrder());
                        answer.put("answer", question.answer());
                        return answer;
                    }).toList());
            if (fields.contains("PROCESS")) content.put("processNotes", round.processNotes());
            if (fields.contains("REVIEW")) content.put("reviewSummary", round.reviewSummary());
            if (fields.contains("RESULT")) content.put("result", round.result());
            result.add(new SharingDtos.PublicRoundResponse(Long.toString(round.id()), content));
        }
        return result;
    }

    private Duration expiry(String value) {
        return switch (value) {
            case "ONE_DAY" -> Duration.ofDays(1);
            case "SEVEN_DAYS" -> Duration.ofDays(7);
            case "THIRTY_DAYS" -> Duration.ofDays(30);
            case "PERMANENT" -> Duration.ofDays(3650);
            default -> throw new InvalidInputException("INVALID_SHARE_EXPIRY", "Invalid share expiry");
        };
    }

    private void validateSubset(Set<String> supplied, Set<String> allowed, String code) {
        if (!allowed.containsAll(supplied)) throw new InvalidInputException(code, "Invalid share fields");
    }

    private Long parseId(String value) {
        try { return Long.valueOf(value); } catch (NumberFormatException exception) { throw new NotFoundException(); }
    }
}
