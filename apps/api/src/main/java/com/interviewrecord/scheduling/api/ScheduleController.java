package com.interviewrecord.scheduling.api;

import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.util.ResourceIds;
import com.interviewrecord.scheduling.application.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schedules")
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class ScheduleController {
    private final CurrentUser currentUser;
    private final ScheduleService scheduleService;

    public ScheduleController(CurrentUser currentUser, ScheduleService scheduleService) {
        this.currentUser = currentUser;
        this.scheduleService = scheduleService;
    }

    @GetMapping
    ScheduleDtos.ScheduleListResponse list(@RequestParam(required = false) String status) {
        return new ScheduleDtos.ScheduleListResponse(
                scheduleService.list(currentUser.require().id(), status));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ScheduleDtos.ScheduleResponse create(@Valid @RequestBody ScheduleDtos.ScheduleRequest request) {
        return scheduleService.create(currentUser.require().id(), request);
    }

    @GetMapping("/{id}")
    ScheduleDtos.ScheduleResponse get(@PathVariable String id) {
        return scheduleService.get(currentUser.require().id(), ResourceIds.parse(id));
    }

    @PutMapping("/{id}")
    ScheduleDtos.ScheduleResponse update(@PathVariable String id,
            @Valid @RequestBody ScheduleDtos.ScheduleRequest request) {
        return scheduleService.update(currentUser.require().id(), ResourceIds.parse(id), request);
    }

    @PatchMapping("/{id}/status")
    ScheduleDtos.ScheduleResponse changeStatus(@PathVariable String id,
            @Valid @RequestBody ScheduleDtos.StatusUpdateRequest request) {
        return scheduleService.changeStatus(currentUser.require().id(), ResourceIds.parse(id), request.status());
    }

    @PatchMapping("/{id}/urgency")
    ScheduleDtos.ScheduleResponse overrideUrgency(@PathVariable String id,
            @RequestBody ScheduleDtos.UrgencyOverrideRequest request) {
        return scheduleService.overrideUrgency(currentUser.require().id(), ResourceIds.parse(id),
                request.urgency());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id) {
        scheduleService.delete(currentUser.require().id(), ResourceIds.parse(id));
    }
}
