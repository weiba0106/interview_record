package com.interviewrecord.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewrecord.auth.application.AuthService;
import com.interviewrecord.auth.application.EmailVerificationService;
import com.interviewrecord.auth.application.PasswordResetService;
import com.interviewrecord.auth.application.RegistrationService;
import com.interviewrecord.common.config.SecurityConfig;
import com.interviewrecord.common.error.GlobalExceptionHandler;
import com.interviewrecord.common.error.InvalidRegistrationException;
import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
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
class PasswordResetApiTest {
    @MockitoBean com.interviewrecord.auth.infrastructure.JpaUserRepository jpaUserRepository;
    @MockitoBean org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired MockMvc mvc;
    @MockitoBean RegistrationService registrationService;
    @MockitoBean EmailVerificationService emailVerificationService;
    @MockitoBean AuthService authService;
    @MockitoBean PasswordResetService passwordResetService;

    @Test
    void forgotPasswordAlwaysAcceptsUnknownAccountRequests() throws Exception {
        mvc.perform(post("/api/v1/auth/forgot-password").with(csrf()).contentType(APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.com\"}"))
                .andExpect(status().isAccepted());
    }

    @Test
    void resetPasswordUsesStableInvalidTokenError() throws Exception {
        willThrow(new InvalidRegistrationException("INVALID_OR_EXPIRED_TOKEN"))
                .given(passwordResetService).reset(any(), any());

        mvc.perform(post("/api/v1/auth/reset-password").with(csrf()).contentType(APPLICATION_JSON)
                        .content("{\"token\":\"expired\",\"newPassword\":\"NewPassword123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OR_EXPIRED_TOKEN"));
    }

    @Test
    void resetAndForgotPasswordRequireCsrf() throws Exception {
        mvc.perform(post("/api/v1/auth/forgot-password").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));

        mvc.perform(post("/api/v1/auth/reset-password").contentType(APPLICATION_JSON)
                        .content("{\"token\":\"reset-token\",\"newPassword\":\"NewPassword123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
    }
}
