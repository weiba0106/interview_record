package com.interviewrecord.tracking.application;

import com.interviewrecord.common.error.ConflictException;
import com.interviewrecord.common.error.InvalidInputException;
import com.interviewrecord.common.error.NotFoundException;
import com.interviewrecord.common.html.RichTextSanitizer;
import com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository;
import com.interviewrecord.scheduling.application.ScheduleService;
import com.interviewrecord.scheduling.domain.ScheduleEvent;
import com.interviewrecord.scheduling.infrastructure.JpaScheduleEventRepository;
import com.interviewrecord.tracking.api.TrackingDtos.NextScheduleRef;
import com.interviewrecord.tracking.api.TrackingDtos.PositionListResponse;
import com.interviewrecord.tracking.api.TrackingDtos.PositionRequest;
import com.interviewrecord.tracking.api.TrackingDtos.PositionResponse;
import com.interviewrecord.tracking.api.TrackingDtos.StatusRef;
import com.interviewrecord.tracking.domain.Company;
import com.interviewrecord.tracking.domain.JobType;
import com.interviewrecord.tracking.domain.Position;
import com.interviewrecord.tracking.domain.PositionStatus;
import com.interviewrecord.tracking.infrastructure.JpaCompanyRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedJobTypeRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedPositionStatusRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class PositionService {
    private static final int MAX_PAGE_SIZE = 50;

    private final JpaPositionRepository positions;
    private final JpaCompanyRepository companies;
    private final JpaManagedJobTypeRepository jobTypes;
    private final JpaManagedPositionStatusRepository statuses;
    private final JpaInterviewRoundRepository rounds;
    private final JpaScheduleEventRepository schedules;
    private final ScheduleService scheduleService;
    private final RichTextSanitizer sanitizer;
    private final Clock clock;

    public PositionService(JpaPositionRepository positions, JpaCompanyRepository companies,
            JpaManagedJobTypeRepository jobTypes, JpaManagedPositionStatusRepository statuses,
            JpaInterviewRoundRepository rounds, JpaScheduleEventRepository schedules,
            ScheduleService scheduleService, RichTextSanitizer sanitizer, Clock clock) {
        this.positions = positions; this.companies = companies; this.jobTypes = jobTypes;
        this.statuses = statuses; this.rounds = rounds; this.schedules = schedules;
        this.scheduleService = scheduleService; this.sanitizer = sanitizer; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PositionListResponse search(Long userId, Long companyId, Long jobTypeId, Long statusId,
            Boolean archived, String keyword, int page, int size, String sortBy, String sortDir) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        String property = switch (sortBy == null ? "updatedAt" : sortBy) {
            case "appliedAt" -> "appliedAt";
            case "deadlineAt" -> "deadlineAt";
            default -> "updatedAt";
        };
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC, property);
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<Position> result = positions.search(userId, companyId, jobTypeId, statusId,
                archived, normalizedKeyword, PageRequest.of(safePage, safeSize, sort));
        List<PositionResponse> items = enrich(userId, result.getContent());
        return new PositionListResponse(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PositionResponse get(Long userId, Long positionId) {
        Position position = requireOwned(userId, positionId);
        return enrich(userId, List.of(position)).get(0);
    }

    @Transactional
    public PositionResponse create(Long userId, PositionRequest request) {
        Company company = resolveCompanyForCreate(userId, request);
        JobType jobType = requireActiveJobType(userId, request.jobTypeId());
        PositionStatus status = requireActiveStatus(userId, request.statusId());
        requireSafeUrl(request.applyUrl());
        Instant now = clock.instant();
        Position position = new Position(userId, company.id(), jobType.id(), status.id(),
                request.title().trim(), blankToNull(request.applyUrl()), request.appliedAt(),
                request.deadlineAt(), blankToNull(request.workLocation()), sanitized(request.description()), now);
        Position saved = positions.save(position);
        if (Boolean.TRUE.equals(request.createDeadlineSchedule()) && saved.deadlineAt() != null) {
            scheduleService.createLinked(userId, "投递截止：" + company.name() + " " + saved.title(),
                    "APPLY_DEADLINE", null, saved.deadlineAt(), saved.id(), null, null, null);
        }
        return enrich(userId, List.of(saved)).get(0);
    }

    @Transactional
    public PositionResponse update(Long userId, Long positionId, PositionRequest request) {
        Position position = requireOwned(userId, positionId);
        if (request.version() != null && request.version() != position.version()) {
            throw new ConflictException("CONCURRENT_UPDATE", "岗位已被更新，请刷新后重试");
        }
        if (request.companyId() == null || request.companyId().isBlank()) {
            throw new InvalidInputException("COMPANY_REQUIRED", "请选择公司或输入新公司名称");
        }
        Company company = companies.findByIdAndUserId(parseId(request.companyId()), userId)
                .orElseThrow(NotFoundException::new);
        JobType jobType = requireActiveJobType(userId, request.jobTypeId());
        PositionStatus status = requireActiveStatus(userId, request.statusId());
        requireSafeUrl(request.applyUrl());
        position.update(company.id(), jobType.id(), status.id(), request.title().trim(),
                blankToNull(request.applyUrl()), request.appliedAt(), request.deadlineAt(),
                blankToNull(request.workLocation()), sanitized(request.description()), clock.instant());
        return enrich(userId, List.of(position)).get(0);
    }

    @Transactional
    public PositionResponse changeStatus(Long userId, Long positionId, Long statusId) {
        Position position = requireOwned(userId, positionId);
        PositionStatus status = statuses.findByIdAndUserId(statusId, userId).orElseThrow(NotFoundException::new);
        if (!status.active()) {
            throw new InvalidInputException("STATUS_INACTIVE", "该状态已停用");
        }
        position.changeStatus(status.id(), clock.instant());
        return enrich(userId, List.of(position)).get(0);
    }

    @Transactional
    public PositionResponse setArchived(Long userId, Long positionId, boolean archived) {
        Position position = requireOwned(userId, positionId);
        position.setArchived(archived, clock.instant());
        return enrich(userId, List.of(position)).get(0);
    }

    @Transactional
    public void delete(Long userId, Long positionId, boolean confirmed) {
        Position position = requireOwned(userId, positionId);
        if (!confirmed) {
            throw new ConflictException("POSITION_DELETE_CONFIRM_REQUIRED",
                    "删除岗位将同时删除其面试轮次和关联日程，请确认后继续");
        }
        positions.delete(position);
    }

    private Position requireOwned(Long userId, Long positionId) {
        return positions.findByIdAndUserId(positionId, userId).orElseThrow(NotFoundException::new);
    }

    private Company resolveCompanyForCreate(Long userId, PositionRequest request) {
        String newName = request.newCompanyName() == null ? null : request.newCompanyName().trim();
        if (!newName.isBlank()) {
            // 快速新建：同名公司直接复用，避免重复；名称查找仅限当前用户范围
            return companies.findFirstByUserIdAndNameIgnoreCase(userId, newName)
                    .orElseGet(() -> companies.save(new Company(userId, newName, null, null, clock.instant())));
        }
        if (request.companyId() == null || request.companyId().isBlank()) {
            throw new InvalidInputException("COMPANY_REQUIRED", "请选择公司或输入新公司名称");
        }
        return companies.findByIdAndUserId(parseId(request.companyId()), userId)
                .orElseThrow(NotFoundException::new);
    }

    private JobType requireActiveJobType(Long userId, String rawId) {
        JobType jobType = jobTypes.findByIdAndUserId(parseId(rawId), userId).orElseThrow(NotFoundException::new);
        if (!jobType.active()) {
            throw new InvalidInputException("JOB_TYPE_INACTIVE", "该招聘类型已停用");
        }
        return jobType;
    }

    private PositionStatus requireActiveStatus(Long userId, String rawId) {
        PositionStatus status = statuses.findByIdAndUserId(parseId(rawId), userId)
                .orElseThrow(NotFoundException::new);
        if (!status.active()) {
            throw new InvalidInputException("STATUS_INACTIVE", "该状态已停用");
        }
        return status;
    }

    private void requireSafeUrl(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            String scheme = URI.create(url.trim()).getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new InvalidInputException("INVALID_APPLY_URL", "投递链接仅支持 http 或 https 地址");
            }
        } catch (IllegalArgumentException exception) {
            throw new InvalidInputException("INVALID_APPLY_URL", "投递链接格式不正确");
        }
    }

    private List<PositionResponse> enrich(Long userId, List<Position> page) {
        Instant now = clock.instant();
        Map<Long, Company> companiesById = companies.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .collect(Collectors.toMap(Company::id, Function.identity()));
        Map<Long, JobType> jobTypesById = jobTypes.findAllByUserIdOrderByIdAsc(userId).stream()
                .collect(Collectors.toMap(JobType::id, Function.identity()));
        Map<Long, StatusRef> statusRefs = new HashMap<>();
        for (PositionStatus status : statuses.findAllByUserIdOrderBySortOrderAsc(userId)) {
            statusRefs.put(status.id(), new StatusRef(Long.toString(status.id()), status.name(),
                    status.color(), status.statisticsCategory()));
        }
        List<Long> positionIds = page.stream().map(Position::id).toList();
        Map<Long, ScheduleEvent> nextSchedules = nextSchedulesByPosition(userId, positionIds, now);
        Map<Long, Long> roundCounts = toCountMap(positionIds.isEmpty() ? List.of()
                : rounds.countByUserIdGroupedByPosition(userId, positionIds));
        Map<Long, Long> scheduleCounts = toCountMap(positionIds.isEmpty() ? List.of()
                : schedules.countByUserIdGroupedByPosition(userId, positionIds));
        return page.stream().map(position -> {
            Company company = companiesById.get(position.companyId());
            JobType jobType = jobTypesById.get(position.jobTypeId());
            ScheduleEvent next = nextSchedules.get(position.id());
            return new PositionResponse(Long.toString(position.id()), position.title(),
                    Long.toString(position.companyId()), company == null ? "" : company.name(),
                    Long.toString(position.jobTypeId()), jobType == null ? "" : jobType.name(),
                    statusRefs.get(position.statusId()), position.applyUrl(), position.appliedAt(),
                    position.deadlineAt(), position.workLocation(), sanitizer.sanitize(position.description()),
                    position.archived(), roundCounts.getOrDefault(position.id(), 0L),
                    scheduleCounts.getOrDefault(position.id(), 0L),
                    next == null ? null : new NextScheduleRef(Long.toString(next.id()), next.title(),
                            next.eventType(), next.referenceTime()),
                    position.version(), position.createdAt(), position.updatedAt());
        }).toList();
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private Map<Long, ScheduleEvent> nextSchedulesByPosition(Long userId, List<Long> positionIds, Instant now) {
        Map<Long, ScheduleEvent> next = new HashMap<>();
        if (positionIds.isEmpty()) {
            return next;
        }
        for (ScheduleEvent event : schedules.findPendingForPositionsFrom(userId, positionIds, now)) {
            next.putIfAbsent(event.positionId(), event);
        }
        return next;
    }

    private Long parseId(String rawId) {
        return com.interviewrecord.common.util.ResourceIds.parse(rawId);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 写入路径的白名单清洗；读出时（enrich）对历史数据再清洗兜底。 */
    private String sanitized(String html) {
        return sanitizer.sanitize(html);
    }
}
