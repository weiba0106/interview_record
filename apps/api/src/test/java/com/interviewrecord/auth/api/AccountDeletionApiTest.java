package com.interviewrecord.auth.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewrecord.common.config.SecurityConfig;
import com.interviewrecord.common.error.GlobalExceptionHandler;
import com.interviewrecord.common.security.AuthenticatedUser;
import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import com.interviewrecord.preference.api.MeController;
import com.interviewrecord.preference.domain.Theme;
import com.interviewrecord.auth.application.AccountDeletionService;
import com.interviewrecord.common.error.InvalidRegistrationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(value = MeController.class, properties = "spring.datasource.url=jdbc:test")
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JsonAccessDeniedHandler.class, JsonAuthenticationEntryPoint.class,
        CurrentUser.class})
class AccountDeletionApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean AccountDeletionService accountDeletionService;
    @MockitoBean com.interviewrecord.preference.application.PreferenceService preferenceService;

    @Test
    void wrongPasswordReturnsStableValidationErrorWithoutSuccessfulDeletion() throws Exception {
        willThrow(new InvalidRegistrationException("INVALID_PASSWORD")).given(accountDeletionService)
                .deleteCurrentUser(42L, "not-the-password");
        mvc.perform(delete("/api/v1/me").with(authentication(authenticationFor(alice()))).with(csrf())
                        .contentType(APPLICATION_JSON).content("{\"password\":\"not-the-password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
    }

    @Test
    void successfulDeletionClearsTheCurrentSessionCookie() throws Exception {
        mvc.perform(delete("/api/v1/me").with(authentication(authenticationFor(alice()))).with(csrf())
                        .contentType(APPLICATION_JSON).content("{\"password\":\"Password123\"}"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("INTERVIEW_RECORD_SESSION", 0));
    }

    private AuthenticatedUser alice() {
        return new AuthenticatedUser(42L, "alice@example.com", "Alice", true, "Asia/Shanghai", Theme.GRAPHITE_CORAL);
    }

    private UsernamePasswordAuthenticationToken authenticationFor(AuthenticatedUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
    }
}
