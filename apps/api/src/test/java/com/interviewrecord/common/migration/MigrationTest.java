package com.interviewrecord.common.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.interviewrecord.support.MySqlIntegrationTestBase;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MigrationTest extends MySqlIntegrationTestBase {

    @Autowired
    Flyway flyway;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void appliesAccountAndSessionMigrations() {
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("2");

        Integer users = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = 'users'",
                Integer.class);
        Integer sessions = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = 'SPRING_SESSION'",
                Integer.class);

        assertThat(users).isEqualTo(1);
        assertThat(sessions).isEqualTo(1);
    }
}
