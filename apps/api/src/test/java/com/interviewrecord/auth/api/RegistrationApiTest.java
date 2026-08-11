package com.interviewrecord.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewrecord.auth.application.RegistrationResult;
import com.interviewrecord.auth.application.RegistrationService;
import com.interviewrecord.auth.application.EmailVerificationService;
import com.interviewrecord.auth.application.AuthService;
import com.interviewrecord.auth.application.PasswordResetService;
import com.interviewrecord.common.error.EmailAlreadyRegisteredException;
import com.interviewrecord.common.error.InvalidRegistrationException;
import com.interviewrecord.common.error.RateLimitExceededException;
import com.interviewrecord.common.config.SecurityConfig;
import com.interviewrecord.common.error.GlobalExceptionHandler;
import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = AuthController.class, properties = "spring.datasource.url=jdbc:test")
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JsonAccessDeniedHandler.class, JsonAuthenticationEntryPoint.class})
class RegistrationApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean RegistrationService registrationService;
    @MockitoBean EmailVerificationService emailVerificationService;
    @MockitoBean AuthService authService;
    @MockitoBean PasswordResetService passwordResetService;

    @Test
    void registersWithCsrfAndReturnsOnlyPublicFields() throws Exception {
        given(registrationService.register(any())).willReturn(new RegistrationResult(42L, "user@example.com"));

        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123\",\"displayName\":\"小林\",\"timeZone\":\"Asia/Shanghai\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.verificationRequired").value(true))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void rejectsPublicMutationWithoutCsrf() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123\",\"displayName\":\"小林\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
    }

    @Test
    void materializesPublicCsrfTokenWithoutCreatingAuthenticatedSession() throws Exception {
        mvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getCookie("XSRF-TOKEN")).isNotNull())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getRequest().getSession(false)).isNull());
    }

    @Test
    void mapsDuplicateEmailToConflict() throws Exception {
        given(registrationService.register(any())).willThrow(new EmailAlreadyRegisteredException());

        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123\",\"displayName\":\"小林\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void mapsRateLimitToTooManyRequestsWithRetryAfter() throws Exception {
        given(registrationService.register(any())).willThrow(new RateLimitExceededException(java.time.Duration.ofMinutes(10)));

        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123\",\"displayName\":\"小林\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Retry-After", "600"));
    }

    @Test
    void mapsCreationPolicyViolationToValidationError() throws Exception {
        given(registrationService.register(any())).willThrow(new InvalidRegistrationException("INVALID_PASSWORD"));

        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123\",\"displayName\":\"小林\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
    }
}
