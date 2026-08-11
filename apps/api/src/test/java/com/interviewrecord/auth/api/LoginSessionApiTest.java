package com.interviewrecord.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

import com.interviewrecord.auth.application.AuthService;
import com.interviewrecord.auth.application.EmailVerificationService;
import com.interviewrecord.auth.application.RegistrationService;
import com.interviewrecord.auth.application.PasswordResetService;
import com.interviewrecord.common.config.SecurityConfig;
import com.interviewrecord.common.error.GlobalExceptionHandler;
import com.interviewrecord.common.security.AuthenticatedUser;
import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import com.interviewrecord.preference.api.MeController;
import com.interviewrecord.preference.api.PreferenceDtos;
import com.interviewrecord.preference.domain.Theme;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(value = {AuthController.class, MeController.class}, properties = "spring.datasource.url=jdbc:test")
@ImportAutoConfiguration({SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JsonAccessDeniedHandler.class, JsonAuthenticationEntryPoint.class,
        CurrentUser.class})
class LoginSessionApiTest {
    private static final AuthenticatedUser VERIFIED_USER = new AuthenticatedUser(
            42L, "user@example.com", "小林", true, "Asia/Shanghai", Theme.GRAPHITE_CORAL);

    @Autowired MockMvc mvc;
    @MockitoBean RegistrationService registrationService;
    @MockitoBean EmailVerificationService emailVerificationService;
    @MockitoBean AuthService authService;
    @MockitoBean PasswordResetService passwordResetService;
    @MockitoBean com.interviewrecord.preference.application.PreferenceService preferenceService;
    @MockitoBean com.interviewrecord.auth.application.AccountDeletionService accountDeletionService;

    @Test
    void verifiedUserLogsInAndSessionCanReadCurrentUser() throws Exception {
        given(authService.login(any(), any(), any())).willReturn(VERIFIED_USER);
        given(preferenceService.get(42L)).willReturn(new PreferenceDtos.PreferenceResponse(
                VERIFIED_USER.displayName(), VERIFIED_USER.timeZone(), VERIFIED_USER.theme(), java.util.List.of(), java.util.List.of()));

        MvcResult login = mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123\"}"))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/v1/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("42"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.displayName").value("小林"))
                .andExpect(jsonPath("$.emailVerified").value(true))
                .andExpect(jsonPath("$.timeZone").value("Asia/Shanghai"))
                .andExpect(jsonPath("$.theme").value("GRAPHITE_CORAL"));

        org.assertj.core.api.Assertions.assertThat(((java.security.Principal) VERIFIED_USER).getName())
                .isEqualTo("user@example.com");
    }

    @Test
    void loginDoesNotReuseSuppliedAnonymousSessionId() throws Exception {
        given(authService.login(any(), any(), any())).willReturn(VERIFIED_USER);
        MockHttpSession anonymousSession = new MockHttpSession();
        String anonymousSessionId = anonymousSession.getId();

        MvcResult login = mvc.perform(post("/api/v1/auth/login").session(anonymousSession).with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123\"}"))
                .andExpect(status().isNoContent())
                .andReturn();

        org.assertj.core.api.Assertions.assertThat(login.getRequest().getSession(false).getId()).isNotEqualTo(anonymousSessionId);
    }

    @Test
    void wrongCredentialsUseGenericUnauthorizedResponse() throws Exception {
        given(authService.login(any(), any(), any())).willThrow(new AuthService.InvalidCredentialsException());

        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void unverifiedAccountCannotLogIn() throws Exception {
        given(authService.login(any(), any(), any())).willThrow(new AuthService.EmailNotVerifiedException());

        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void loginMutationRequiresCsrf() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
    }

    @Test
    void unauthenticatedClientCannotReadCurrentUser() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void logoutInvalidatesCurrentSession() throws Exception {
        MockHttpSession session = authenticatedSession(VERIFIED_USER);

        mvc.perform(post("/api/v1/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("INTERVIEW_RECORD_SESSION", 0))
                .andExpect(cookie().secure("INTERVIEW_RECORD_SESSION", true))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
                        result.getResponse().getCookie("INTERVIEW_RECORD_SESSION").getAttribute("SameSite"))
                        .isEqualTo("Lax"));

        mvc.perform(get("/api/v1/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentUserIsAlwaysReadFromAuthenticatedSession() throws Exception {
        AuthenticatedUser otherUser = new AuthenticatedUser(
                84L, "other@example.com", "小周", true, "UTC", Theme.FOREST_TEAL);
        given(preferenceService.get(84L)).willReturn(new PreferenceDtos.PreferenceResponse(
                otherUser.displayName(), otherUser.timeZone(), otherUser.theme(), java.util.List.of(), java.util.List.of()));

        mvc.perform(get("/api/v1/me").with(authentication(authenticationFor(otherUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("84"))
                .andExpect(jsonPath("$.email").value("other@example.com"));
    }

    private MockHttpSession authenticatedSession(AuthenticatedUser user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("SPRING_SECURITY_CONTEXT",
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext());
        org.springframework.security.core.context.SecurityContext context =
                (org.springframework.security.core.context.SecurityContext) session.getAttribute("SPRING_SECURITY_CONTEXT");
        context.setAuthentication(authenticationFor(user));
        return session;
    }

    private UsernamePasswordAuthenticationToken authenticationFor(AuthenticatedUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
    }
}
