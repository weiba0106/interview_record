package com.interviewrecord.auth.api;

import com.interviewrecord.auth.application.RegisterCommand;
import com.interviewrecord.auth.application.RegistrationResult;
import com.interviewrecord.auth.application.RegistrationService;
import com.interviewrecord.auth.application.EmailVerificationService;
import com.interviewrecord.auth.application.AuthService;
import com.interviewrecord.auth.application.PasswordResetService;
import com.interviewrecord.common.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class AuthController {
    private final RegistrationService registrationService;
    private final EmailVerificationService emailVerificationService;
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final boolean secureSessionCookie;
    public AuthController(RegistrationService registrationService, EmailVerificationService emailVerificationService,
            AuthService authService, PasswordResetService passwordResetService,
            @Value("${server.servlet.session.cookie.secure:true}") boolean secureSessionCookie) {
        this.registrationService = registrationService; this.emailVerificationService = emailVerificationService;
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.secureSessionCookie = secureSessionCookie;
    }
    @GetMapping("/csrf") @ResponseStatus(HttpStatus.NO_CONTENT)
    void csrf(CsrfToken token) {
        token.getToken();
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    AuthDtos.RegisterResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request, HttpServletRequest servletRequest) {
        RegistrationResult result = registrationService.register(new RegisterCommand(request.email(), request.password(), request.displayName(),
                request.timeZone(), servletRequest.getRemoteAddr()));
        return new AuthDtos.RegisterResponse(result.normalizedEmail(), true);
    }
    @PostMapping("/verify-email") @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyEmail(@Valid @RequestBody AuthDtos.VerifyEmailRequest request) {
        emailVerificationService.verify(request.token());
    }
    @PostMapping("/resend-verification") @ResponseStatus(HttpStatus.ACCEPTED)
    void resendVerification(@Valid @RequestBody AuthDtos.ResendVerificationRequest request, HttpServletRequest servletRequest) {
        emailVerificationService.resend(request.email(), servletRequest.getRemoteAddr());
    }
    @PostMapping("/forgot-password") @ResponseStatus(HttpStatus.ACCEPTED)
    void forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request, HttpServletRequest servletRequest) {
        passwordResetService.request(request.email(), servletRequest.getRemoteAddr());
    }
    @PostMapping("/reset-password") @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        passwordResetService.reset(request.token(), request.newPassword());
    }
    @PostMapping("/login") @ResponseStatus(HttpStatus.NO_CONTENT)
    void login(@Valid @RequestBody AuthDtos.LoginRequest request, HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthenticatedUser user = authService.login(request.email(), request.password(), servletRequest.getRemoteAddr());
        servletRequest.getSession(true);
        servletRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities()));
        new HttpSessionSecurityContextRepository().saveContext(context, servletRequest, servletResponse);
    }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest request, HttpServletResponse response) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        var cookie = new jakarta.servlet.http.Cookie("INTERVIEW_RECORD_SESSION", "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(secureSessionCookie);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
