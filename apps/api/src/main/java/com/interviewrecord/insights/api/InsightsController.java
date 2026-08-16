package com.interviewrecord.insights.api;

import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.insights.application.InsightsService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

@RestController
@RequestMapping("/api/v1/insights")
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class InsightsController {
    private final CurrentUser currentUser;
    private final InsightsService insights;

    public InsightsController(CurrentUser currentUser, InsightsService insights) {
        this.currentUser = currentUser;
        this.insights = insights;
    }

    @GetMapping
    InsightDtos.InsightsResponse get(
            @RequestParam(required = false) Long jobTypeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedTo) {
        return insights.getInsights(currentUser.require().id(), new InsightDtos.InsightFilter(jobTypeId, appliedFrom, appliedTo));
    }
}
