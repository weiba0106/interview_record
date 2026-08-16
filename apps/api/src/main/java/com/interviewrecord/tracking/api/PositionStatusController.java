package com.interviewrecord.tracking.api;

import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.util.ResourceIds;
import com.interviewrecord.tracking.application.PositionStatusService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statuses")
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class PositionStatusController {
    private final CurrentUser currentUser;
    private final PositionStatusService statusService;

    public PositionStatusController(CurrentUser currentUser, PositionStatusService statusService) {
        this.currentUser = currentUser;
        this.statusService = statusService;
    }

    @GetMapping
    List<TrackingDtos.StatusResponse> list() {
        return statusService.list(currentUser.require().id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TrackingDtos.StatusResponse create(@Valid @RequestBody TrackingDtos.StatusRequest request) {
        return statusService.create(currentUser.require().id(), request);
    }

    @PutMapping("/{id}")
    TrackingDtos.StatusResponse update(@PathVariable String id,
            @Valid @RequestBody TrackingDtos.StatusRequest request) {
        return statusService.update(currentUser.require().id(), ResourceIds.parse(id), request);
    }

    @PutMapping("/order")
    List<TrackingDtos.StatusResponse> reorder(@Valid @RequestBody TrackingDtos.StatusReorderRequest request) {
        return statusService.reorder(currentUser.require().id(), request.orderedIds());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id, @RequestParam(required = false) String migrateToId) {
        statusService.delete(currentUser.require().id(), ResourceIds.parse(id), migrateToId);
    }
}
