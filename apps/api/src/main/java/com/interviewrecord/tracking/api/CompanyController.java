package com.interviewrecord.tracking.api;

import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.util.ResourceIds;
import com.interviewrecord.tracking.application.CompanyService;
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
@RequestMapping("/api/v1/companies")
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class CompanyController {
    private final CurrentUser currentUser;
    private final CompanyService companyService;

    public CompanyController(CurrentUser currentUser, CompanyService companyService) {
        this.currentUser = currentUser;
        this.companyService = companyService;
    }

    @GetMapping
    List<TrackingDtos.CompanyResponse> list() {
        return companyService.list(currentUser.require().id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TrackingDtos.CompanyResponse create(@Valid @RequestBody TrackingDtos.CompanyRequest request) {
        return companyService.create(currentUser.require().id(), request);
    }

    @GetMapping("/{id}")
    TrackingDtos.CompanyDetailResponse get(@PathVariable String id) {
        return companyService.get(currentUser.require().id(), ResourceIds.parse(id));
    }

    @PutMapping("/{id}")
    TrackingDtos.CompanyResponse update(@PathVariable String id,
            @Valid @RequestBody TrackingDtos.CompanyRequest request) {
        return companyService.update(currentUser.require().id(), ResourceIds.parse(id), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id, @RequestParam(defaultValue = "false") boolean confirmed) {
        companyService.delete(currentUser.require().id(), ResourceIds.parse(id), confirmed);
    }
}
