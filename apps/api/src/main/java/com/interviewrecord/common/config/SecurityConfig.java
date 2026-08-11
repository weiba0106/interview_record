package com.interviewrecord.common.config;

import com.interviewrecord.common.security.JsonAccessDeniedHandler;
import com.interviewrecord.common.security.JsonAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import com.interviewrecord.auth.domain.User;
import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityConfig {
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrf.setCookieName("XSRF-TOKEN");
        csrf.setHeaderName("X-XSRF-TOKEN");
        return csrf;
    }

    @Bean
    @ConditionalOnBean(JpaUserRepository.class)
    UserDetailsService userDetailsService(JpaUserRepository users) {
        return email -> users.findByEmail(email)
                .map(this::asUserDetails)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("USER_NOT_FOUND"));
    }

    @Bean
    @ConditionalOnBean(JpaUserRepository.class)
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
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonAuthenticationEntryPoint entryPoint,
            JsonAccessDeniedHandler accessDeniedHandler, CsrfTokenRepository csrfTokenRepository) throws Exception {
        http.csrf(configurer -> configurer.csrfTokenRepository(csrfTokenRepository))
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
