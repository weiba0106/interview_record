package com.interviewrecord.dashboard.api;

import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.dashboard.application.DashboardService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class DashboardController {
    private final CurrentUser currentUser;
    private final DashboardService dashboardService;

    public DashboardController(CurrentUser currentUser, DashboardService dashboardService) {
        this.currentUser = currentUser;
        this.dashboardService = dashboardService;
    }

    @GetMapping
    DashboardDtos.DashboardResponse overview() {
        return dashboardService.overview(currentUser.require().id());
    }
}
