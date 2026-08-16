package com.interviewrecord.tracking.api;

import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.util.ResourceIds;
import com.interviewrecord.interviews.api.InterviewDtos;
import com.interviewrecord.interviews.application.InterviewService;
import com.interviewrecord.tracking.application.PositionService;
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
@RequestMapping("/api/v1/positions")
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class PositionController {
    private final CurrentUser currentUser;
    private final PositionService positionService;
    private final InterviewService interviewService;

    public PositionController(CurrentUser currentUser, PositionService positionService,
            InterviewService interviewService) {
        this.currentUser = currentUser;
        this.positionService = positionService;
        this.interviewService = interviewService;
    }

    @GetMapping
    TrackingDtos.PositionListResponse search(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String jobTypeId,
            @RequestParam(required = false) String statusId,
            @RequestParam(required = false) Boolean archived,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {
        return positionService.search(currentUser.require().id(), parseOrNull(companyId), parseOrNull(jobTypeId),
                parseOrNull(statusId), archived, keyword, page, size, sortBy, sortDir);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TrackingDtos.PositionResponse create(@Valid @RequestBody TrackingDtos.PositionRequest request) {
        return positionService.create(currentUser.require().id(), request);
    }

    @GetMapping("/{id}")
    TrackingDtos.PositionResponse get(@PathVariable String id) {
        return positionService.get(currentUser.require().id(), ResourceIds.parse(id));
    }

    @PutMapping("/{id}")
    TrackingDtos.PositionResponse update(@PathVariable String id,
            @Valid @RequestBody TrackingDtos.PositionRequest request) {
        return positionService.update(currentUser.require().id(), ResourceIds.parse(id), request);
    }

    @PatchMapping("/{id}/status")
    TrackingDtos.PositionResponse changeStatus(@PathVariable String id,
            @Valid @RequestBody TrackingDtos.StatusChangeRequest request) {
        return positionService.changeStatus(currentUser.require().id(), ResourceIds.parse(id),
                ResourceIds.parse(request.statusId()));
    }

    @PatchMapping("/{id}/archive")
    TrackingDtos.PositionResponse setArchived(@PathVariable String id,
            @RequestParam boolean archived) {
        return positionService.setArchived(currentUser.require().id(), ResourceIds.parse(id), archived);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id, @RequestParam(defaultValue = "false") boolean confirmed) {
        positionService.delete(currentUser.require().id(), ResourceIds.parse(id), confirmed);
    }

    @GetMapping("/{id}/interview-rounds")
    InterviewDtos.RoundListResponse listRounds(@PathVariable String id) {
        return new InterviewDtos.RoundListResponse(
                interviewService.listByPosition(currentUser.require().id(), ResourceIds.parse(id)));
    }

    @PostMapping("/{id}/interview-rounds")
    @ResponseStatus(HttpStatus.CREATED)
    InterviewDtos.RoundResponse createRound(@PathVariable String id,
            @Valid @RequestBody InterviewDtos.RoundRequest request) {
        return interviewService.create(currentUser.require().id(), ResourceIds.parse(id), request);
    }

    private Long parseOrNull(String rawId) {
        return rawId == null || rawId.isBlank() ? null : ResourceIds.parse(rawId);
    }
}
