package com.interviewrecord.preference.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.interviewrecord.common.config.SecurityConfig;
import com.interviewrecord.common.error.GlobalExceptionHandler;
import com.interviewrecord.common.security.AuthenticatedUser;
import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import com.interviewrecord.preference.domain.Theme;
import com.interviewrecord.preference.application.PreferenceService;
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
class PreferenceApiTest {
    @Autowired MockMvc mvc;
    @MockitoBean PreferenceService preferenceService;
    @MockitoBean com.interviewrecord.auth.application.AccountDeletionService accountDeletionService;

    @Test
    void meReadsCurrentPersistedPreferencesRatherThanTheSessionSnapshot() throws Exception {
        AuthenticatedUser alice = new AuthenticatedUser(42L, "alice@example.com", "Old display name", true,
                "Asia/Shanghai", Theme.GRAPHITE_CORAL);
        given(preferenceService.get(42L)).willReturn(new PreferenceDtos.PreferenceResponse(
                "Alice Tokyo", "Asia/Tokyo", Theme.FOREST_TEAL, java.util.List.of(60), java.util.List.of(10)));

        mvc.perform(get("/api/v1/me").with(authentication(authenticationFor(alice))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice Tokyo"))
                .andExpect(jsonPath("$.timeZone").value("Asia/Tokyo"))
                .andExpect(jsonPath("$.theme").value("FOREST_TEAL"));
        verify(preferenceService).get(42L);
    }

    @Test
    void readsPersistedPreferencesForTheAuthenticatedUser() throws Exception {
        AuthenticatedUser alice = new AuthenticatedUser(42L, "alice@example.com", "Alice", true,
                "Asia/Shanghai", Theme.GRAPHITE_CORAL);
        given(preferenceService.get(42L)).willReturn(new PreferenceDtos.PreferenceResponse(
                "Alice Tokyo", "Asia/Tokyo", Theme.FOREST_TEAL, java.util.List.of(60, 5), java.util.List.of(10)));

        mvc.perform(get("/api/v1/me/preferences").with(authentication(authenticationFor(alice))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice Tokyo"))
                .andExpect(jsonPath("$.timeZone").value("Asia/Tokyo"))
                .andExpect(jsonPath("$.interviewReminderOffsets[0]").value(60))
                .andExpect(jsonPath("$.interviewReminderOffsets[1]").value(5));
        verify(preferenceService).get(42L);
    }

    @Test
    void updateUsesAuthenticatedUserInsteadOfAnyRequestUserId() throws Exception {
        AuthenticatedUser alice = new AuthenticatedUser(42L, "alice@example.com", "Alice", true,
                "Asia/Shanghai", Theme.GRAPHITE_CORAL);

        given(preferenceService.update(eq(42L), any())).willReturn(new PreferenceDtos.PreferenceResponse(
                "Alice Tokyo", "Asia/Tokyo", Theme.FOREST_TEAL, java.util.List.of(1440, 30), java.util.List.of(60)));

        mvc.perform(patch("/api/v1/me/preferences").with(authentication(authenticationFor(alice))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"displayName\":\"Alice Tokyo\",\"timeZone\":\"Asia/Tokyo\",\"theme\":\"FOREST_TEAL\",\"interviewReminderOffsets\":[30,1440,30],\"deadlineReminderOffsets\":[60,60]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice Tokyo"))
                .andExpect(jsonPath("$.timeZone").value("Asia/Tokyo"))
                .andExpect(jsonPath("$.theme").value("FOREST_TEAL"))
                .andExpect(jsonPath("$.interviewReminderOffsets[0]").value(1440))
                .andExpect(jsonPath("$.interviewReminderOffsets[1]").value(30));
        verify(preferenceService).update(eq(42L), any());
    }

    @Test
    void updateRejectsAnInvalidIanaTimeZone() throws Exception {
        AuthenticatedUser alice = new AuthenticatedUser(42L, "alice@example.com", "Alice", true,
                "Asia/Shanghai", Theme.GRAPHITE_CORAL);

        given(preferenceService.update(eq(42L), any())).willThrow(new InvalidRegistrationException("INVALID_TIME_ZONE"));
        mvc.perform(patch("/api/v1/me/preferences").with(authentication(authenticationFor(alice))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"displayName\":\"Alice\",\"timeZone\":\"Mars/Olympus\",\"theme\":\"FOREST_TEAL\",\"interviewReminderOffsets\":[30],\"deadlineReminderOffsets\":[60]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_ZONE"));
    }

    @Test
    void updateRejectsReminderOffsetsOutsideTheAllowedRange() throws Exception {
        AuthenticatedUser alice = new AuthenticatedUser(42L, "alice@example.com", "Alice", true,
                "Asia/Shanghai", Theme.GRAPHITE_CORAL);

        mvc.perform(patch("/api/v1/me/preferences").with(authentication(authenticationFor(alice))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"displayName\":\"Alice\",\"timeZone\":\"Asia/Shanghai\",\"theme\":\"FOREST_TEAL\",\"interviewReminderOffsets\":[10081],\"deadlineReminderOffsets\":[60]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateRejectsAnInvalidTheme() throws Exception {
        mvc.perform(patch("/api/v1/me/preferences").with(authentication(authenticationFor(new AuthenticatedUser(
                        42L, "alice@example.com", "Alice", true, "Asia/Shanghai", Theme.GRAPHITE_CORAL)))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"displayName\":\"Alice\",\"timeZone\":\"Asia/Shanghai\",\"theme\":\"PURPLE\",\"interviewReminderOffsets\":[30],\"deadlineReminderOffsets\":[60]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateRejectsNullReminderOffsetsWithStableValidationError() throws Exception {
        mvc.perform(patch("/api/v1/me/preferences").with(authentication(authenticationFor(new AuthenticatedUser(
                        42L, "alice@example.com", "Alice", true, "Asia/Shanghai", Theme.GRAPHITE_CORAL)))).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"displayName\":\"Alice\",\"timeZone\":\"Asia/Shanghai\",\"theme\":\"FOREST_TEAL\",\"interviewReminderOffsets\":[null],\"deadlineReminderOffsets\":[60]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private UsernamePasswordAuthenticationToken authenticationFor(AuthenticatedUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
    }
}
