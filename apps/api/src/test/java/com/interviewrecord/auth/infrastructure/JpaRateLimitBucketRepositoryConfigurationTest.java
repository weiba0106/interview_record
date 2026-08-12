package com.interviewrecord.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

class JpaRateLimitBucketRepositoryConfigurationTest {

    @Test
    void repositoryIsAlwaysRegisteredWhenJdbcTemplateAutoConfigurationLoads() {
        assertThat(JpaRateLimitBucketRepository.class.getAnnotation(ConditionalOnBean.class))
                .as("the repository must not be conditionally skipped before JdbcTemplate auto-configuration")
                .isNull();
    }
}
