package com.interviewrecord.auth.api;

import com.interviewrecord.auth.application.RegisterCommand;
import com.interviewrecord.auth.application.RegistrationResult;
import com.interviewrecord.auth.application.RegistrationService;
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
import org.springframework.security.web.csrf.CsrfTokenRepository;

@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class AuthController {
    private final RegistrationService registrationService;
    private final CsrfTokenRepository csrfTokens;
    public AuthController(RegistrationService registrationService, CsrfTokenRepository csrfTokens) {
        this.registrationService = registrationService; this.csrfTokens = csrfTokens;
    }
    @GetMapping("/csrf") @ResponseStatus(HttpStatus.NO_CONTENT)
    void csrf(HttpServletRequest request, HttpServletResponse response) {
        csrfTokens.saveToken(csrfTokens.generateToken(request), request, response);
    }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    AuthDtos.RegisterResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request, HttpServletRequest servletRequest) {
        RegistrationResult result = registrationService.register(new RegisterCommand(request.email(), request.password(), request.displayName(),
                request.timeZone(), servletRequest.getRemoteAddr()));
        return new AuthDtos.RegisterResponse(result.normalizedEmail(), true);
    }
}
