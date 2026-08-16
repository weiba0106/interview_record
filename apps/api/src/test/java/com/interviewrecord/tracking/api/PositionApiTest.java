package com.interviewrecord.tracking.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewrecord.common.config.SecurityConfig;
import com.interviewrecord.common.error.ConflictException;
import com.interviewrecord.common.error.GlobalExceptionHandler;
import com.interviewrecord.common.error.InvalidInputException;
import com.interviewrecord.common.security.AuthenticatedUser;
import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import com.interviewrecord.interviews.application.InterviewService;
import com.interviewrecord.preference.domain.Theme;
import com.interviewrecord.tracking.application.PositionService;
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

@WebMvcTest(value = PositionController.class, properties = "spring.datasource.url=jdbc:test")
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JsonAccessDeniedHandler.class,
        JsonAuthenticationEntryPoint.class, CurrentUser.class})
class PositionApiTest {
    @MockitoBean com.interviewrecord.auth.infrastructure.JpaUserRepository jpaUserRepository;
    @MockitoBean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private static final AuthenticatedUser ALICE = new AuthenticatedUser(
            42L, "alice@example.com", "Alice", true, "Asia/Shanghai", Theme.GRAPHITE_CORAL);

    @Autowired MockMvc mvc;
    @MockitoBean PositionService positionService;
    @MockitoBean InterviewService interviewService;

    @Test
    void unauthenticatedClientGets401() throws Exception {
        mvc.perform(get("/api/v1/positions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void searchForwardsFiltersPaginationAndSortToServiceScopedByUser() throws Exception {
        given(positionService.search(eq(42L), eq(5L), isNull(), isNull(), eq(false), eq("后端"),
                isNull(), isNull(), eq(1), eq(10), eq("appliedAt"), eq("desc")))
                .willReturn(new TrackingDtos.PositionListResponse(List.of(), 1, 10, 0, 0));

        mvc.perform(get("/api/v1/positions").with(authentication(authenticationFor(ALICE)))
                        .param("companyId", "5").param("archived", "false").param("keyword", "后端")
                        .param("page", "1").param("size", "10").param("sortBy", "appliedAt").param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
        verify(positionService).search(42L, 5L, null, null, false, "后端", null, null, 1, 10, "appliedAt", "desc");
    }

    @Test
    void searchForwardsAppliedDateRangeAndNextScheduleSort() throws Exception {
        java.time.LocalDate from = java.time.LocalDate.parse("2026-08-01");
        java.time.LocalDate to = java.time.LocalDate.parse("2026-08-31");
        given(positionService.search(eq(42L), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq(from), eq(to), eq(0), eq(20), eq("nextSchedule"), eq("asc")))
                .willReturn(new TrackingDtos.PositionListResponse(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/v1/positions").with(authentication(authenticationFor(ALICE)))
                        .param("appliedFrom", "2026-08-01").param("appliedTo", "2026-08-31")
                        .param("sortBy", "nextSchedule").param("sortDir", "asc"))
                .andExpect(status().isOk());

        verify(positionService).search(42L, null, null, null, null, null, from, to, 0, 20, "nextSchedule", "asc");
    }

    @Test
    void createRejectsUnsafeApplyUrlScheme() throws Exception {
        given(positionService.create(eq(42L), any()))
                .willThrow(new InvalidInputException("INVALID_APPLY_URL", "投递链接仅支持 http 或 https 地址"));

        mvc.perform(post("/api/v1/positions").with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"companyId\":\"1\",\"jobTypeId\":\"1\",\"statusId\":\"1\",\"title\":\"后端开发\",\"applyUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_APPLY_URL"));
    }

    @Test
    void createWithoutAnyCompanyReferenceIsRejected() throws Exception {
        given(positionService.create(eq(42L), any()))
                .willThrow(new InvalidInputException("COMPANY_REQUIRED", "请选择公司或输入新公司名称"));

        mvc.perform(post("/api/v1/positions").with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"jobTypeId\":\"1\",\"statusId\":\"1\",\"title\":\"后端开发\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMPANY_REQUIRED"));
    }

    @Test
    void deleteRequiresExplicitConfirmation() throws Exception {
        org.mockito.Mockito.doThrow(new ConflictException("POSITION_DELETE_CONFIRM_REQUIRED", "请确认删除影响"))
                .when(positionService).delete(42L, 3L, false);

        mvc.perform(delete("/api/v1/positions/3").with(authentication(authenticationFor(ALICE))).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POSITION_DELETE_CONFIRM_REQUIRED"));
    }

    @Test
    void staleVersionUpdateMapsToConcurrentUpdateConflict() throws Exception {
        given(positionService.update(eq(42L), eq(3L), any()))
                .willThrow(new ConflictException("CONCURRENT_UPDATE", "岗位已被更新，请刷新后重试"));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/positions/3").with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"companyId\":\"1\",\"jobTypeId\":\"1\",\"statusId\":\"1\",\"title\":\"后端开发\",\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_UPDATE"));
    }

    @Test
    void malformedPositionIdReturnsStableNotFoundInsteadOf500() throws Exception {
        mvc.perform(get("/api/v1/positions/not-a-number").with(authentication(authenticationFor(ALICE))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(AuthenticatedUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
    }
}
