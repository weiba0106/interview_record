package com.interviewrecord.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewrecord.auth.application.EmailVerificationService;
import com.interviewrecord.auth.application.RegistrationService;
import com.interviewrecord.auth.application.AuthService;
import com.interviewrecord.auth.application.PasswordResetService;
import com.interviewrecord.common.config.SecurityConfig;
import com.interviewrecord.common.error.GlobalExceptionHandler;
import com.interviewrecord.common.error.InvalidRegistrationException;
import com.interviewrecord.common.error.RateLimitExceededException;
import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(value = AuthController.class, properties = "spring.datasource.url=jdbc:test")
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JsonAccessDeniedHandler.class, JsonAuthenticationEntryPoint.class})
class EmailVerificationApiTest {
    @MockitoBean com.interviewrecord.auth.infrastructure.JpaUserRepository jpaUserRepository;
    @MockitoBean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired MockMvc mvc;
    @MockitoBean RegistrationService registrationService;
    @MockitoBean EmailVerificationService verificationService;
    @MockitoBean AuthService authService;
    @MockitoBean PasswordResetService passwordResetService;

    @Test
    void verifiesTokenWithCsrfAndReturnsNoContent() throws Exception {
        mvc.perform(post("/api/v1/auth/verify-email").with(csrf()).contentType(APPLICATION_JSON)
                        .content("{\"token\":\"verification-token\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void mapsInvalidVerificationTokenToBadRequest() throws Exception {
        org.mockito.BDDMockito.willThrow(new InvalidRegistrationException("INVALID_OR_EXPIRED_TOKEN"))
                .given(verificationService).verify(any());

        mvc.perform(post("/api/v1/auth/verify-email").with(csrf()).contentType(APPLICATION_JSON)
                        .content("{\"token\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OR_EXPIRED_TOKEN"));
    }

    @Test
    void resendAlwaysReturnsAcceptedForKnownAndUnknownEmails() throws Exception {
        mvc.perform(post("/api/v1/auth/resend-verification").with(csrf()).contentType(APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void mapsResendLimitToRetryAfter() throws Exception {
        org.mockito.BDDMockito.willThrow(new RateLimitExceededException(Duration.ofSeconds(60)))
                .given(verificationService).resend(any(), any());

        mvc.perform(post("/api/v1/auth/resend-verification").with(csrf()).contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }

    @Test
    void rejectsVerificationMutationWithoutCsrf() throws Exception {
        mvc.perform(post("/api/v1/auth/verify-email").contentType(APPLICATION_JSON)
                        .content("{\"token\":\"verification-token\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
    }
}
