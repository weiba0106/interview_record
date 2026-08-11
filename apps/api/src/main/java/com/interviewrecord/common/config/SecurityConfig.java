package com.interviewrecord.common.config;

import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonAuthenticationEntryPoint entryPoint,
            JsonAccessDeniedHandler accessDeniedHandler) throws Exception {
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieName("XSRF-TOKEN"); csrf.setHeaderName("X-XSRF-TOKEN");
        http.csrf(configurer -> configurer.csrfTokenRepository(csrf))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/verify-email",
                                "/api/v1/auth/resend-verification", "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                                "/api/v1/auth/csrf", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable());
        return http.build();
    }
}
