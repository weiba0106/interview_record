package com.interviewrecord.scheduling.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewrecord.common.config.SecurityConfig;
import com.interviewrecord.common.error.GlobalExceptionHandler;
import com.interviewrecord.common.error.InvalidInputException;
import com.interviewrecord.common.security.AuthenticatedUser;
import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import com.interviewrecord.preference.domain.Theme;
import com.interviewrecord.scheduling.application.ScheduleService;
import java.time.Instant;
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

@WebMvcTest(value = ScheduleController.class, properties = "spring.datasource.url=jdbc:test")
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JsonAccessDeniedHandler.class,
        JsonAuthenticationEntryPoint.class, CurrentUser.class})
class ScheduleApiTest {
    @MockitoBean com.interviewrecord.auth.infrastructure.JpaUserRepository jpaUserRepository;
    @MockitoBean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private static final AuthenticatedUser ALICE = new AuthenticatedUser(
            42L, "alice@example.com", "Alice", true, "Asia/Shanghai", Theme.GRAPHITE_CORAL);

    @Autowired MockMvc mvc;
    @MockitoBean ScheduleService scheduleService;

    @Test
    void scheduleListRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/schedules"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void createdScheduleExposesComputedUrgencyFields() throws Exception {
        Instant startsAt = Instant.parse("2026-08-13T02:00:00Z");
        given(scheduleService.create(eq(42L), any())).willReturn(new ScheduleDtos.ScheduleResponse(
                "11", "笔试", "WRITTEN_TEST", startsAt, null, null, null, null, null, null,
                "PENDING", "URGENT", false, null, startsAt, 0L, startsAt, null, List.of()));

        mvc.perform(post("/api/v1/schedules").with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"笔试\",\"eventType\":\"WRITTEN_TEST\",\"startsAt\":\"2026-08-13T02:00:00Z\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.urgency").value("URGENT"))
                .andExpect(jsonPath("$.overdue").value(false))
                .andExpect(jsonPath("$.manualUrgency").doesNotExist());
    }

    @Test
    void invalidEventTypeIsRejected() throws Exception {
        given(scheduleService.create(eq(42L), any()))
                .willThrow(new InvalidInputException("INVALID_EVENT_TYPE", "不支持的日程类型"));

        mvc.perform(post("/api/v1/schedules").with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"title\":\"测试\",\"eventType\":\"PARTY\",\"startsAt\":\"2026-08-13T02:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EVENT_TYPE"));
    }

    @Test
    void manualUrgencyOverrideIsForwardedForTheAuthenticatedUser() throws Exception {
        Instant startsAt = Instant.parse("2026-09-01T02:00:00Z");
        given(scheduleService.overrideUrgency(42L, 11L, "URGENT")).willReturn(new ScheduleDtos.ScheduleResponse(
                "11", "笔试", "WRITTEN_TEST", startsAt, null, null, null, null, null, null,
                "PENDING", "URGENT", false, "URGENT", startsAt, 1L, startsAt, null, List.of()));

        mvc.perform(patch("/api/v1/schedules/11/urgency").with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"urgency\":\"URGENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manualUrgency").value("URGENT"))
                .andExpect(jsonPath("$.urgency").value("URGENT"));
    }

    @Test
    void reminderOverrideIsForwardedForTheAuthenticatedUser() throws Exception {
        Instant startsAt = Instant.parse("2026-09-01T02:00:00Z");
        given(scheduleService.updateReminders(42L, 11L, List.of(1440, 30)))
                .willReturn(new ScheduleDtos.ScheduleResponse(
                        "11", "笔试", "WRITTEN_TEST", startsAt, null, null, null, null, null, null,
                        "PENDING", "NORMAL", false, null, startsAt, 1L, startsAt, List.of(1440, 30), List.of()));

        mvc.perform(put("/api/v1/schedules/11/reminders").with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"offsets\":[1440,30]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reminderOffsets[0]").value(1440))
                .andExpect(jsonPath("$.reminderOffsets[1]").value(30));
    }

    @Test
    void reminderOverrideRequiresAuthentication() throws Exception {
        mvc.perform(put("/api/v1/schedules/11/reminders").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"offsets\":[1440]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void reminderOverrideRejectsTooManyOffsets() throws Exception {
        given(scheduleService.updateReminders(eq(42L), eq(11L), any()))
                .willThrow(new InvalidInputException("INVALID_REMINDER_OFFSETS", "单条日程最多设置 5 个提醒时间"));

        mvc.perform(put("/api/v1/schedules/11/reminders").with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"offsets\":[1440,720,360,180,60,30]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REMINDER_OFFSETS"));
    }

    @Test
    void statusChangeRejectsUnknownStatus() throws Exception {
        given(scheduleService.changeStatus(eq(42L), eq(11L), eq("DONE")))
                .willThrow(new InvalidInputException("INVALID_SCHEDULE_STATUS", "日程状态只能是 PENDING、COMPLETED 或 CANCELLED"));

        mvc.perform(patch("/api/v1/schedules/11/status").with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SCHEDULE_STATUS"));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(AuthenticatedUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
    }
}
