package com.interviewrecord.preference.api;

import com.interviewrecord.common.security.AuthenticatedUser;
import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.preference.domain.Theme;
import com.interviewrecord.preference.application.PreferenceService;
import com.interviewrecord.auth.application.AccountDeletionService;
import com.interviewrecord.auth.api.AuthDtos;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class MeController {
    private final CurrentUser currentUser;
    private final PreferenceService preferenceService;
    private final AccountDeletionService accountDeletionService;
    private final boolean secureSessionCookie;

    public MeController(CurrentUser currentUser, PreferenceService preferenceService,
            AccountDeletionService accountDeletionService,
            @Value("${server.servlet.session.cookie.secure:true}") boolean secureSessionCookie) {
        this.currentUser = currentUser;
        this.preferenceService = preferenceService;
        this.accountDeletionService = accountDeletionService;
        this.secureSessionCookie = secureSessionCookie;
    }

    @GetMapping
    MeResponse me() {
        AuthenticatedUser user = currentUser.require();
        return new MeResponse(Long.toString(user.id()), user.email(), user.displayName(), user.emailVerified(),
                user.timeZone(), user.theme());
    }

    record MeResponse(String id, String email, String displayName, boolean emailVerified, String timeZone, Theme theme) {
    }

    @PatchMapping("/preferences")
    PreferenceDtos.PreferenceResponse updatePreferences(
            @Valid @RequestBody PreferenceDtos.UpdatePreferencesRequest request) {
        return preferenceService.update(currentUser.require().id(), request);
    }

    @DeleteMapping
    @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAccount(@Valid @RequestBody AuthDtos.DeleteAccountRequest request, HttpServletRequest requestContext,
            HttpServletResponse response) {
        accountDeletionService.deleteCurrentUser(currentUser.require().id(), request.password());
        var session = requestContext.getSession(false);
        if (session != null) session.invalidate();
        var cookie = new jakarta.servlet.http.Cookie("INTERVIEW_RECORD_SESSION", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureSessionCookie);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
