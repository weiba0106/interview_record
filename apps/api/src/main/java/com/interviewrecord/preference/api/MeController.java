package com.interviewrecord.preference.api;

import com.interviewrecord.common.security.AuthenticatedUser;
import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.preference.domain.Theme;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
    private final CurrentUser currentUser;

    public MeController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping
    MeResponse me() {
        AuthenticatedUser user = currentUser.require();
        return new MeResponse(Long.toString(user.id()), user.email(), user.displayName(), user.emailVerified(),
                user.timeZone(), user.theme());
    }

    record MeResponse(String id, String email, String displayName, boolean emailVerified, String timeZone, Theme theme) {
    }
}
