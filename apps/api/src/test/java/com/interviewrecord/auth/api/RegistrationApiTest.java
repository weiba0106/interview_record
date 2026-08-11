package com.interviewrecord.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewrecord.auth.application.RegistrationResult;
import com.interviewrecord.auth.application.RegistrationService;
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
}
