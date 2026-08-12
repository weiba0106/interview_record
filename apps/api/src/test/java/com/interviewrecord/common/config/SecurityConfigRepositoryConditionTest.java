package com.interviewrecord.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

class SecurityConfigRepositoryConditionTest {

    @Test
    void authenticationBeansAreNotSkippedBeforeSpringDataRepositoriesRegister() throws Exception {
        Method userDetailsService = SecurityConfig.class.getDeclaredMethod(
                "userDetailsService", com.interviewrecord.auth.infrastructure.JpaUserRepository.class);
        Method authenticationManager = SecurityConfig.class.getDeclaredMethod(
                "authenticationManager", com.interviewrecord.auth.infrastructure.JpaUserRepository.class,
                org.springframework.security.crypto.password.PasswordEncoder.class);

        assertThat(userDetailsService.getAnnotation(ConditionalOnBean.class)).isNull();
        assertThat(authenticationManager.getAnnotation(ConditionalOnBean.class)).isNull();
    }
}
