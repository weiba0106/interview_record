package com.interviewrecord.common.config;

import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieName("XSRF-TOKEN");
        csrf.setHeaderName("X-XSRF-TOKEN");
        return csrf;
    }

    @Bean
    UserDetailsService userDetailsService(JpaUserRepository users) {
        return email -> users.findByEmail(email)
                .map(this::asUserDetails)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("USER_NOT_FOUND"));
    }

    @Bean
    AuthenticationManager authenticationManager(JpaUserRepository users, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService(users));
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(List.of(provider));
    }

    private org.springframework.security.core.userdetails.UserDetails asUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.withUsername(user.email())
                .password(user.passwordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonAuthenticationEntryPoint entryPoint,
            JsonAccessDeniedHandler accessDeniedHandler, CsrfTokenRepository csrfTokenRepository) throws Exception {
        // Axios returns the raw value from the readable XSRF-TOKEN cookie. The
        // default XOR handler only accepts a masked request value, so use the
        // plain request-attribute handler for this SPA-only API.
        http.csrf(configurer -> configurer.csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/verify-email",
                                "/api/v1/auth/resend-verification", "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                                "/api/v1/auth/csrf", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(entryPoint).accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session.sessionFixation().changeSessionId())
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable());
        return http.build();
    }
}
