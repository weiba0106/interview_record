package com.interviewrecord.interviews.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewrecord.common.config.SecurityConfig;
import com.interviewrecord.common.error.ConflictException;
import com.interviewrecord.common.error.GlobalExceptionHandler;
import com.interviewrecord.common.security.AuthenticatedUser;
import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import com.interviewrecord.interviews.application.InterviewService;
import com.interviewrecord.preference.domain.Theme;
import com.interviewrecord.tracking.api.PositionController;
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

@WebMvcTest(value = {PositionController.class, InterviewRoundController.class},
        properties = "spring.datasource.url=jdbc:test")
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JsonAccessDeniedHandler.class,
        JsonAuthenticationEntryPoint.class, CurrentUser.class})
class InterviewRoundApiTest {
    @MockitoBean com.interviewrecord.auth.infrastructure.JpaUserRepository jpaUserRepository;
    @MockitoBean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private static final AuthenticatedUser ALICE = new AuthenticatedUser(
            42L, "alice@example.com", "Alice", true, "Asia/Shanghai", Theme.GRAPHITE_CORAL);
    private static final AuthenticatedUser BOB = new AuthenticatedUser(
            84L, "bob@example.com", "Bob", true, "Asia/Shanghai", Theme.GRAPHITE_CORAL);

    @Autowired MockMvc mvc;
    @MockitoBean InterviewService interviewService;
    @MockitoBean com.interviewrecord.tracking.application.PositionService positionService;

    @Test
    void roundCreationRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/positions/1/interview-rounds").with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"roundName\":\"一面\",\"roundNumber\":1,\"interviewType\":\"VIDEO\",\"result\":\"UPCOMING\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void duplicateRoundNumberWithinSamePositionIsRejected() throws Exception {
        given(interviewService.create(eq(42L), eq(1L), any()))
                .willThrow(new ConflictException("ROUND_NUMBER_TAKEN", "该岗位已存在相同轮次序号的面试"));

        mvc.perform(post("/api/v1/positions/1/interview-rounds")
                        .with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"roundName\":\"一面\",\"roundNumber\":1,\"interviewType\":\"VIDEO\",\"result\":\"UPCOMING\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROUND_NUMBER_TAKEN"));
    }

    @Test
    void missingRoundNumberFailsValidation() throws Exception {
        mvc.perform(post("/api/v1/positions/1/interview-rounds")
                        .with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"roundName\":\"一面\",\"interviewType\":\"VIDEO\",\"result\":\"UPCOMING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.roundNumber").exists());
    }

    @Test
    void roundUpdateGoesThroughTheRoundScopedEndpointWithAuthenticatedUser() throws Exception {
        given(interviewService.update(eq(42L), eq(9L), any())).willThrow(
                new com.interviewrecord.common.error.NotFoundException());

        mvc.perform(put("/api/v1/interview-rounds/9").with(authentication(authenticationFor(ALICE))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"roundName\":\"二面\",\"roundNumber\":2,\"interviewType\":\"ONSITE\",\"result\":\"UPCOMING\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        verify(interviewService).update(eq(42L), eq(9L), any());
    }

    @Test
    void foreignPositionRoundListDoesNotReachTheOtherUsersRecords() throws Exception {
        given(interviewService.listByPosition(84L, 9L))
                .willThrow(new com.interviewrecord.common.error.NotFoundException());

        mvc.perform(get("/api/v1/positions/9/interview-rounds")
                        .with(authentication(authenticationFor(BOB))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(AuthenticatedUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
    }
}
