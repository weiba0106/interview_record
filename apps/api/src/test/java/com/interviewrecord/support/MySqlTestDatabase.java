package com.interviewrecord.support;

import java.net.URI;
import java.util.stream.Stream;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MySQLContainer;

public final class MySqlTestDatabase {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.9")
            .withDatabaseName("interview_record_test")
            .withUsername("test")
            .withPassword("test");

    public static void configure(DynamicPropertyRegistry registry) {
        ExternalTestDatabase external = ExternalTestDatabase.fromEnvironment();
        if (external.present()) {
            external.assertDedicatedTestSchema();
            registry.add("spring.datasource.url", external::url);
            registry.add("spring.datasource.username", external::username);
            registry.add("spring.datasource.password", external::password);
            return;
        }

        MYSQL.start();
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    private MySqlTestDatabase() {
    }

    private record ExternalTestDatabase(String url, String username, String password) {

        static ExternalTestDatabase fromEnvironment() {
            String url = System.getenv("TEST_DB_URL");
            String username = System.getenv("TEST_DB_USERNAME");
            String password = System.getenv("TEST_DB_PASSWORD");
            long supplied = Stream.of(url, username, password)
                    .filter(value -> value != null && !value.isBlank())
                    .count();
            if (supplied != 0 && supplied != 3) {
                throw new IllegalStateException(
                        "TEST_DB_URL, TEST_DB_USERNAME and TEST_DB_PASSWORD must be supplied together");
            }
            return new ExternalTestDatabase(url, username, password);
        }

        boolean present() {
            return url != null && !url.isBlank();
        }

        void assertDedicatedTestSchema() {
            if (!url.startsWith("jdbc:")) {
                throw new IllegalStateException("External test database URL must be a JDBC URL");
            }

            URI jdbcUri = URI.create(url.substring("jdbc:".length()));
            String path = jdbcUri.getPath();
            String schema = path != null && path.startsWith("/") ? path.substring(1) : "";
            if (!"interview_record_test".equals(schema)) {
                throw new IllegalStateException("External tests require the interview_record_test schema");
            }
        }
    }
}
