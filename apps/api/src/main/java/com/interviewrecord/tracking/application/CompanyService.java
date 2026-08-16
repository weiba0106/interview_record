package com.interviewrecord.tracking.application;

import com.interviewrecord.common.error.ConflictException;
import com.interviewrecord.common.error.NotFoundException;
import com.interviewrecord.interviews.infrastructure.JpaInterviewRoundRepository;
import com.interviewrecord.scheduling.infrastructure.JpaScheduleEventRepository;
import com.interviewrecord.tracking.api.TrackingDtos.CompanyDetailResponse;
import com.interviewrecord.tracking.api.TrackingDtos.CompanyRequest;
import com.interviewrecord.tracking.api.TrackingDtos.CompanyResponse;
import com.interviewrecord.tracking.api.TrackingDtos.PositionSummary;
import com.interviewrecord.tracking.api.TrackingDtos.StatusRef;
import com.interviewrecord.tracking.domain.Company;
import com.interviewrecord.tracking.domain.JobType;
import com.interviewrecord.tracking.domain.Position;
import com.interviewrecord.tracking.domain.PositionStatus;
import com.interviewrecord.tracking.infrastructure.JpaCompanyRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedJobTypeRepository;
import com.interviewrecord.tracking.infrastructure.JpaManagedPositionStatusRepository;
import com.interviewrecord.tracking.infrastructure.JpaPositionRepository;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class CompanyService {
    private final JpaCompanyRepository companies;
    private final JpaPositionRepository positions;
    private final JpaManagedJobTypeRepository jobTypes;
    private final JpaManagedPositionStatusRepository statuses;
    private final JpaInterviewRoundRepository rounds;
    private final JpaScheduleEventRepository schedules;
    private final Clock clock;

    public CompanyService(JpaCompanyRepository companies, JpaPositionRepository positions,
            JpaManagedJobTypeRepository jobTypes, JpaManagedPositionStatusRepository statuses,
            JpaInterviewRoundRepository rounds, JpaScheduleEventRepository schedules, Clock clock) {
        this.companies = companies; this.positions = positions; this.jobTypes = jobTypes;
        this.statuses = statuses; this.rounds = rounds; this.schedules = schedules; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> list(Long userId) {
        Map<Long, Long> counts = positionCounts(userId);
        return companies.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(company -> toResponse(company, counts.getOrDefault(company.id(), 0L)))
                .toList();
    }

    @Transactional
    public CompanyResponse create(Long userId, CompanyRequest request) {
        String name = request.name().trim();
        if (companies.existsByUserIdAndNameIgnoreCase(userId, name) && !Boolean.TRUE.equals(request.confirmDuplicate())) {
            throw new ConflictException("COMPANY_DUPLICATE", "已存在同名公司，确认后可继续创建");
        }
        Company saved = companies.save(new Company(userId, name, blankToNull(request.website()),
                blankToNull(request.notes()), clock.instant()));
        return toResponse(saved, 0);
    }

    @Transactional(readOnly = true)
    public CompanyDetailResponse get(Long userId, Long companyId) {
        Company company = requireOwned(userId, companyId);
        List<Position> companyPositions = positions.findAllByUserIdAndArchivedOrderByUpdatedAtDesc(userId, false)
                .stream().filter(position -> companyId.equals(position.companyId())).toList();
        List<Long> positionIds = companyPositions.stream().map(Position::id).toList();
        Map<Long, StatusRef> statusRefs = statusRefs(userId, companyPositions);
        Map<Long, JobType> jobTypeRefs = jobTypes.findAllByUserIdOrderByIdAsc(userId).stream()
                .collect(Collectors.toMap(JobType::id, Function.identity()));
        List<PositionSummary> summaries = companyPositions.stream()
                .map(position -> new PositionSummary(Long.toString(position.id()), position.title(),
                        Long.toString(position.companyId()), company.name(),
                        Long.toString(position.jobTypeId()),
                        jobTypeRefs.containsKey(position.jobTypeId()) ? jobTypeRefs.get(position.jobTypeId()).name() : "",
                        statusRefs.get(position.statusId()), position.appliedAt(), position.deadlineAt(),
                        position.archived(), position.updatedAt()))
                .toList();
        long roundCount = positionIds.isEmpty() ? 0 : rounds.countByUserIdAndPositionIdIn(userId, positionIds);
        long scheduleCount = positionIds.isEmpty() ? 0 : schedules.countByUserIdAndPositionIdIn(userId, positionIds);
        return new CompanyDetailResponse(Long.toString(company.id()), company.name(), company.website(),
                company.notes(), companyPositions.size(), roundCount, scheduleCount, summaries,
                company.createdAt(), company.updatedAt());
    }

    @Transactional
    public CompanyResponse update(Long userId, Long companyId, CompanyRequest request) {
        Company company = requireOwned(userId, companyId);
        String name = request.name().trim();
        boolean sameName = name.equalsIgnoreCase(company.name());
        if (!sameName && companies.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("COMPANY_DUPLICATE", "已存在同名公司");
        }
        company.update(name, blankToNull(request.website()), blankToNull(request.notes()), clock.instant());
        return toResponse(company, positionCounts(userId).getOrDefault(companyId, 0L));
    }

    @Transactional
    public void delete(Long userId, Long companyId, boolean confirmed) {
        Company company = requireOwned(userId, companyId);
        long positionCount = positions.countByUserIdAndCompanyId(userId, companyId);
        if (positionCount > 0 && !confirmed) {
            throw new ConflictException("COMPANY_HAS_POSITIONS",
                    "公司下仍有 " + positionCount + " 个岗位，确认后公司将连同岗位、面试记录和日程一起删除");
        }
        companies.delete(company);
    }

    private Company requireOwned(Long userId, Long companyId) {
        return companies.findByIdAndUserId(companyId, userId).orElseThrow(NotFoundException::new);
    }

    private Map<Long, Long> positionCounts(Long userId) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : positions.countByUserIdGroupedByCompany(userId)) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private Map<Long, StatusRef> statusRefs(Long userId, List<Position> companyPositions) {
        Map<Long, StatusRef> refs = new HashMap<>();
        for (PositionStatus status : statuses.findAllByUserIdOrderBySortOrderAsc(userId)) {
            refs.put(status.id(), new StatusRef(Long.toString(status.id()), status.name(),
                    status.color(), status.statisticsCategory()));
        }
        return refs;
    }

    private CompanyResponse toResponse(Company company, long positionCount) {
        return new CompanyResponse(Long.toString(company.id()), company.name(), company.website(),
                company.notes(), positionCount, company.createdAt(), company.updatedAt());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
