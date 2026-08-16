package com.interviewrecord.tracking.api;

import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.util.ResourceIds;
import com.interviewrecord.tracking.application.JobTypeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/job-types")
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class JobTypeController {
    private final CurrentUser currentUser;
    private final JobTypeService jobTypeService;

    public JobTypeController(CurrentUser currentUser, JobTypeService jobTypeService) {
        this.currentUser = currentUser;
        this.jobTypeService = jobTypeService;
    }

    @GetMapping
    List<TrackingDtos.JobTypeResponse> list() {
        return jobTypeService.list(currentUser.require().id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TrackingDtos.JobTypeResponse create(@Valid @RequestBody TrackingDtos.JobTypeRequest request) {
        return jobTypeService.create(currentUser.require().id(), request);
    }

    @PutMapping("/{id}")
    TrackingDtos.JobTypeResponse update(@PathVariable String id,
            @Valid @RequestBody TrackingDtos.JobTypeRequest request) {
        return jobTypeService.update(currentUser.require().id(), ResourceIds.parse(id), request);
    }
}
