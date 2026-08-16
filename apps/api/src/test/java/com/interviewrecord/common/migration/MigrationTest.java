package com.interviewrecord.common.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("9");

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

    @Test
    void appliesCompanyPositionInterviewAndScheduleMigrations() {
        for (String table : new String[] {"companies", "positions", "interview_rounds",
                "interview_questions", "schedule_events", "reminders", "share_links", "share_rounds"}) {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables "
                            + "WHERE table_schema = DATABASE() AND table_name = ?",
                    Integer.class, table);
            assertThat(count).as("table " + table).isEqualTo(1);
        }

        Integer roundUnique = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE()"
                        + " AND table_name = 'interview_rounds' AND index_name = 'uk_rounds_position_number'",
                Integer.class);
        assertThat(roundUnique).isGreaterThanOrEqualTo(1);

        Integer scheduleUserIndex = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE()"
                        + " AND table_name = 'schedule_events' AND column_name = 'user_id'",
                Integer.class);
        assertThat(scheduleUserIndex).isGreaterThanOrEqualTo(1);

        Integer reminderOffsetsColumn = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()"
                        + " AND table_name = 'schedule_events' AND column_name = 'reminder_offsets'",
                Integer.class);
        assertThat(reminderOffsetsColumn).isEqualTo(1);

        for (String[] richTextColumn : new String[][] {
                {"positions", "description"}, {"interview_rounds", "process_notes"},
                {"interview_rounds", "review_summary"}}) {
            Integer longText = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()"
                            + " AND table_name = ? AND column_name = ? AND data_type = 'longtext'",
                    Integer.class, richTextColumn[0], richTextColumn[1]);
            assertThat(longText).as("longtext %s.%s", richTextColumn[0], richTextColumn[1]).isEqualTo(1);
        }
    }

    @Test
    void scheduleTypeCompatibilityMigrationAcceptsHrCommunicationAndRejectsLegacyAssessment() {
        jdbc.update("INSERT INTO schedule_events (user_id, title, event_type, status, version, created_at, updated_at)"
                + " VALUES (1, 'HR 沟通', 'HR_COMMUNICATION', 'PENDING', 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))");

        assertThatThrownBy(() -> jdbc.update("INSERT INTO schedule_events"
                + " (user_id, title, event_type, status, version, created_at, updated_at)"
                + " VALUES (1, '旧测评', 'ASSESSMENT', 'PENDING', 0, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
