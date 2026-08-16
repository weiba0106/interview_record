package com.interviewrecord;

import com.interviewrecord.auth.infrastructure.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
        "spring.mail.host=localhost",
        "spring.mail.username=context-test@example.com",
        "app.frontend-base-url=http://localhost:5173"
})
class InterviewRecordApplicationTest {

    @Test
    void contextLoads() {
    }

    // The rate limit repository requires a JdbcTemplate bean even when the real
    // datasource auto-configuration is excluded, so provide an inert stub.
    @TestConfiguration
    static class JdbcTemplateStub {
        @Bean
        JdbcTemplate jdbcTemplate() {
            return new JdbcTemplate(DataSourceBuilder.create()
                    .url("jdbc:mysql://localhost:3306/interview_record_stub")
                    .username("stub")
                    .password("stub")
                    .build());
        }

        // SecurityConfig wires JpaUserRepository unconditionally; with JPA
        // auto-configuration excluded the repository bean must be stubbed.
        @Bean
        JpaUserRepository jpaUserRepository() {
            return Mockito.mock(JpaUserRepository.class);
        }
    }
}
