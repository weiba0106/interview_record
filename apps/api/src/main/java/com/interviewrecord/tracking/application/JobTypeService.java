package com.interviewrecord.tracking.application;

import com.interviewrecord.common.error.ConflictException;
import com.interviewrecord.common.error.NotFoundException;
import com.interviewrecord.tracking.api.TrackingDtos.JobTypeRequest;
import com.interviewrecord.tracking.api.TrackingDtos.JobTypeResponse;
import com.interviewrecord.tracking.domain.JobType;
import com.interviewrecord.tracking.infrastructure.JpaManagedJobTypeRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class JobTypeService {
    private final JpaManagedJobTypeRepository jobTypes;
    private final Clock clock;

    public JobTypeService(JpaManagedJobTypeRepository jobTypes, Clock clock) {
        this.jobTypes = jobTypes; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<JobTypeResponse> list(Long userId) {
        return jobTypes.findAllByUserIdOrderByIdAsc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public JobTypeResponse create(Long userId, JobTypeRequest request) {
        String name = request.name().trim();
        if (jobTypes.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("JOB_TYPE_NAME_TAKEN", "已存在同名招聘类型");
        }
        return toResponse(jobTypes.save(new JobType(userId, name, clock.instant())));
    }

    @Transactional
    public JobTypeResponse update(Long userId, Long jobTypeId, JobTypeRequest request) {
        JobType jobType = jobTypes.findByIdAndUserId(jobTypeId, userId).orElseThrow(NotFoundException::new);
        String name = request.name().trim();
        if (!name.equalsIgnoreCase(jobType.name()) && jobTypes.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("JOB_TYPE_NAME_TAKEN", "已存在同名招聘类型");
        }
        jobType.rename(name, clock.instant());
        if (request.active() != null && request.active() != jobType.active()) {
            jobType.setActive(request.active(), clock.instant());
        }
        return toResponse(jobType);
    }

    private JobTypeResponse toResponse(JobType jobType) {
        return new JobTypeResponse(Long.toString(jobType.id()), jobType.name(), jobType.active());
    }
}
