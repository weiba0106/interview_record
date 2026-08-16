package com.interviewrecord.dashboard.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewrecord.common.config.SecurityConfig;
import com.interviewrecord.common.error.GlobalExceptionHandler;
import com.interviewrecord.common.security.AuthenticatedUser;
import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import com.interviewrecord.dashboard.application.DashboardService;
import com.interviewrecord.preference.domain.Theme;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = DashboardController.class, properties = "spring.datasource.url=jdbc:test")
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JsonAccessDeniedHandler.class,
        JsonAuthenticationEntryPoint.class, CurrentUser.class})
class DashboardApiTest {
    @MockitoBean com.interviewrecord.auth.infrastructure.JpaUserRepository jpaUserRepository;
    @MockitoBean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private static final AuthenticatedUser ALICE = new AuthenticatedUser(
            42L, "alice@example.com", "Alice", true, "Asia/Shanghai", Theme.GRAPHITE_CORAL);

    @Autowired MockMvc mvc;
    @MockitoBean DashboardService dashboardService;

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void dashboardReturnsMetricsPositionRowsAndSchedulesForTheAuthenticatedUser() throws Exception {
        given(dashboardService.overview(42L)).willReturn(new DashboardDtos.DashboardResponse(
                new DashboardDtos.Metrics(6, 4, 3, 1), List.of(), List.of()));

        mvc.perform(get("/api/v1/dashboard").with(authentication(authenticationFor(ALICE))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.totalPositions").value(6))
                .andExpect(jsonPath("$.metrics.activePositions").value(4))
                .andExpect(jsonPath("$.metrics.upcomingScheduleCount").value(3))
                .andExpect(jsonPath("$.metrics.offerCount").value(1))
                .andExpect(jsonPath("$.positions").isArray())
                .andExpect(jsonPath("$.schedules").isArray());
    }

    private UsernamePasswordAuthenticationToken authenticationFor(AuthenticatedUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
    }
}
