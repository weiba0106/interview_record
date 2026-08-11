# Foundation and Account Lifecycle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable, tested monorepo with a Spring Boot API, Vue SPA, MySQL migrations, JDBC sessions, and the complete email/password account lifecycle required by V1.

**Architecture:** The backend is a package-by-feature Spring Boot modular monolith with domain/application/infrastructure boundaries and MySQL as the only shared state store. The frontend is a Vue SPA served from a separate Vite process in development and same-origin with the API in production. Authentication uses Spring Security sessions persisted by Spring Session JDBC, cookie-based CSRF protection, hashed one-time tokens, and database-backed rate limits.

**Tech Stack:** Java 21, Spring Boot 4.1.0, Maven Wrapper 3.9.16, Spring MVC, Spring Security, Spring Data JPA, Spring Session JDBC, Flyway, Spring Mail, MySQL 8.4 LTS, Node 24 LTS, npm 11, Vue 3.5.x, TypeScript 6.0.x, Vite 8.1.x, Vue Router, Pinia, Element Plus, Vitest, Playwright.

## Global Constraints

- Follow `AGENTS.md` and PRD sections 3, 5, 6, 7.1, 7.4.1, 7.7, 7.11, 9–14.
- Keep Java source/target at 21 and Spring Boot at 4.1.0.
- Use Maven Wrapper 3.9.16 for every backend command after Task 1.
- Use Node 24 LTS and commit `apps/web/package-lock.json`.
- Use MySQL with `utf8mb4` and UTC persistence; never use H2 as a behavioral substitute.
- Use the exact database names `interview_record` for development and `interview_record_test` for automated external-database tests.
- Never create or drop a database unless its resolved name equals `interview_record` or `interview_record_test`.
- Store sessions in MySQL with Spring Session JDBC; do not issue JWT access or refresh tokens.
- Store only SHA-256 hashes of email verification and password-reset tokens.
- New users cannot log in until email verification succeeds.
- Create default job types `秋招` and `日常实习`, the eight PRD statuses, Asia/Shanghai fallback time zone, default reminder offsets, and Graphite/Coral theme in the registration transaction.
- Every endpoint test includes unauthenticated and cross-user cases when a user-owned resource is involved.
- Do not start company, position, interview, schedule, share, statistics, or export features in this phase.

---

## Planned File Map

```text
apps/api/src/main/java/com/interviewrecord/
├── InterviewRecordApplication.java
├── common/
│   ├── config/{ClockConfig,PasswordConfig,SecurityConfig,WebConfig}.java
│   ├── error/{ApiError,FieldViolation,GlobalExceptionHandler}.java
│   ├── security/{AuthenticatedUser,CurrentUser}.java
│   └── token/{IssuedToken,SecureTokenService}.java
├── auth/
│   ├── api/{AuthController,AuthDtos}.java
│   ├── application/{AuthService,RateLimitService}.java
│   ├── domain/{User,EmailVerificationToken,PasswordResetToken}.java
│   └── infrastructure/{JpaUserRepository,JpaTokenRepositories,SpringSessionRevoker}.java
├── preference/
│   ├── api/{MeController,PreferenceDtos}.java
│   ├── application/PreferenceService.java
│   ├── domain/{UserPreference,Theme}.java
│   └── infrastructure/JpaUserPreferenceRepository.java
├── defaults/
│   ├── application/UserDefaultsService.java
│   ├── domain/{DefaultJobType,DefaultPositionStatus}.java
│   └── infrastructure/{JpaJobTypeRepository,JpaPositionStatusRepository}.java
└── mail/
    ├── application/MailGateway.java
    └── infrastructure/{SmtpMailGateway,CapturingMailGateway}.java

apps/web/src/
├── app/{App.vue,main.ts,router.ts}
├── shared/api/{http.ts,error.ts}
├── shared/auth/{auth.store.ts,auth.types.ts}
├── shared/ui/AppShell.vue
├── features/auth/
│   ├── api/auth.api.ts
│   └── components/{LoginForm,RegisterForm,ForgotPasswordForm,ResetPasswordForm}.vue
├── features/preferences/
│   ├── api/preferences.api.ts
│   └── components/PreferenceForm.vue
└── views/{LoginView,RegisterView,VerifyEmailView,ForgotPasswordView,ResetPasswordView,DashboardView,SettingsView}.vue
```

Later tasks may add focused files inside these folders but must not merge controller, service, and persistence responsibilities into a single class.

### Task 1: Scaffold the verified monorepo

**Files:**
- Create: `.editorconfig`
- Create: `.gitattributes`
- Create: `.env.example`
- Create: `README.md`
- Create: `compose.yaml`
- Create: `apps/api/pom.xml`
- Create: `apps/api/.mvn/wrapper/maven-wrapper.properties`
- Create: `apps/api/mvnw`
- Create: `apps/api/mvnw.cmd`
- Create: `apps/api/src/main/java/com/interviewrecord/InterviewRecordApplication.java`
- Create: `apps/api/src/main/resources/application.yml`
- Create: `apps/api/src/test/java/com/interviewrecord/InterviewRecordApplicationTest.java`
- Create: `apps/web/**` through the official Vue scaffold, then normalize files listed below
- Create: `apps/web/src/app/App.vue`
- Create: `apps/web/src/app/main.ts`
- Create: `apps/web/src/app/App.spec.ts`
- Create: `.github/workflows/ci.yml`
- Modify: `AGENTS.md`

**Interfaces:**
- Consumes: none; this is the repository foundation.
- Produces: backend command `apps/api/mvnw.cmd verify`, frontend commands `npm.cmd run test:unit`, `npm.cmd run type-check`, `npm.cmd run build`, and root service definitions `mysql` and `mailpit`.

- [ ] **Step 1: Write the backend context smoke test before the application exists**

```java
package com.interviewrecord;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class InterviewRecordApplicationTest {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: Run the smoke test and confirm the project is absent**

Run from the repository root:

```powershell
mvn -f apps/api/pom.xml test
```

Expected: FAIL because `apps/api/pom.xml` and `InterviewRecordApplication` do not exist yet.

- [ ] **Step 3: Create the Spring Boot POM and application entry point**

`apps/api/pom.xml` must use this dependency set; let the Spring Boot parent manage transitive versions:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.0</version>
        <relativePath/>
    </parent>
    <groupId>com.interviewrecord</groupId>
    <artifactId>interview-record-api</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <properties>
        <java.version>21</java.version>
        <maven.compiler.release>21</maven.compiler.release>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-session-jdbc</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-mysql</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

```java
package com.interviewrecord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InterviewRecordApplication {
    public static void main(String[] args) {
        SpringApplication.run(InterviewRecordApplication.class, args);
    }
}
```

Create the initial `application.yml` with only the application name so the smoke test has no environment prerequisites:

```yaml
spring:
  application:
    name: interview-record-api
```

- [ ] **Step 4: Generate and pin Maven Wrapper 3.9.16**

Run:

```powershell
mvn -f apps/api/pom.xml wrapper:wrapper -Dmaven=3.9.16
```

Verify `apps/api/.mvn/wrapper/maven-wrapper.properties` contains:

```properties
distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip
```

- [ ] **Step 5: Run the backend smoke test**

Run:

```powershell
Set-Location apps/api
.\mvnw.cmd test
Set-Location ../..
```

Expected: PASS with one `contextLoads` test.

- [ ] **Step 6: Scaffold the Vue app with the approved features**

Run:

```powershell
npm.cmd create vue@latest apps/web '--' --typescript --router --pinia --vitest --playwright --eslint --prettier
Set-Location apps/web
npm.cmd install
npm.cmd install element-plus axios dayjs
Set-Location ../..
```

After installation, inspect `package.json` and require Vue stable 3.5.x, Vite stable 8.1.x, TypeScript stable 6.0.x, and a committed `package-lock.json`. If the scaffold resolves a pre-release or a different major, stop and pin the approved stable line before continuing.

- [ ] **Step 7: Replace the demo UI and write the frontend smoke test**

`apps/web/src/app/App.vue`:

```vue
<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <RouterView />
</template>
```

`apps/web/src/app/App.spec.ts`:

```ts
import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import App from './App.vue'

describe('App', () => {
  it('renders the active route', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<h1>面试记录</h1>' } }],
    })
    router.push('/')
    await router.isReady()

    const wrapper = mount(App, { global: { plugins: [router] } })

    expect(wrapper.get('h1').text()).toBe('面试记录')
  })
})
```

Move the scaffold entry to `src/app/main.ts`, update `index.html` to import `/src/app/main.ts`, and delete unused demo assets/components.

- [ ] **Step 8: Run frontend verification**

Run:

```powershell
Set-Location apps/web
npm.cmd run test:unit -- --run
npm.cmd run type-check
npm.cmd run build
Set-Location ../..
```

Expected: all three commands exit 0.

- [ ] **Step 9: Add reproducible local infrastructure**

Create `compose.yaml` with pinned services:

```yaml
services:
  mysql:
    image: mysql:8.4.9
    environment:
      MYSQL_DATABASE: interview_record
      MYSQL_USER: interview_record
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-local-dev-password}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-local-root-password}
      TZ: UTC
    ports:
      - "3307:3306"
    command: ["--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci"]
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-p${MYSQL_ROOT_PASSWORD:-local-root-password}"]
      interval: 5s
      timeout: 5s
      retries: 20
    volumes:
      - interview-record-mysql:/var/lib/mysql

  mailpit:
    image: axllent/mailpit:v1.30.0
    ports:
      - "1025:1025"
      - "8025:8025"

volumes:
  interview-record-mysql:
```

`.env.example` must document only non-secret local defaults and the external-test path:

```dotenv
DB_URL=jdbc:mysql://localhost:3307/interview_record?serverTimezone=UTC
DB_USERNAME=interview_record
DB_PASSWORD=local-dev-password
TEST_DB_URL=jdbc:mysql://localhost:3306/interview_record_test?serverTimezone=UTC
TEST_DB_USERNAME=interview_record_test
TEST_DB_PASSWORD=local-test-password
MAIL_HOST=localhost
MAIL_PORT=1025
APP_BASE_URL=http://localhost:5173
```

- [ ] **Step 10: Add CI and document exact commands**

Create `.github/workflows/ci.yml` with this job shape, then update `README.md` and sections 3 and 17 of `AGENTS.md` with the exact commands verified in Steps 5 and 8; include `npm.cmd` for Windows and `npm` as the POSIX equivalent.

```yaml
name: ci
on:
  pull_request:
  push:
    branches: [master]

jobs:
  verify:
    runs-on: ubuntu-latest
    services:
      mysql:
        image: mysql:8.4.9
        env:
          MYSQL_DATABASE: interview_record_test
          MYSQL_USER: test
          MYSQL_PASSWORD: test
          MYSQL_ROOT_PASSWORD: root-test
        ports: ["3306:3306"]
        options: >-
          --health-cmd="mysqladmin ping -h 127.0.0.1 -uroot -proot-test"
          --health-interval=5s --health-timeout=5s --health-retries=20
    env:
      TEST_DB_URL: jdbc:mysql://127.0.0.1:3306/interview_record_test?serverTimezone=UTC
      TEST_DB_USERNAME: test
      TEST_DB_PASSWORD: test
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - uses: actions/setup-node@v4
        with:
          node-version: '24'
          cache: npm
          cache-dependency-path: apps/web/package-lock.json
      - run: ./mvnw verify
        working-directory: apps/api
      - run: npm ci
        working-directory: apps/web
      - run: npm run test:unit -- --run
        working-directory: apps/web
      - run: npm run type-check
        working-directory: apps/web
      - run: npm run build
        working-directory: apps/web
```

- [ ] **Step 11: Run the full scaffold verification**

Run:

```powershell
Set-Location apps/api
.\mvnw.cmd verify
Set-Location ../web
npm.cmd run test:unit -- --run
npm.cmd run type-check
npm.cmd run build
Set-Location ../..
git diff --check
```

Expected: every command exits 0 and `git diff --check` reports no whitespace errors.

- [ ] **Step 12: Commit the scaffold**

```powershell
git add .editorconfig .gitattributes .env.example README.md compose.yaml apps .github AGENTS.md
git commit -m "build: scaffold Spring Boot and Vue applications"
```

### Task 2: Establish MySQL migrations, test isolation, and API errors

**Files:**
- Create: `apps/api/src/main/resources/db/migration/V1__account_schema.sql`
- Create: `apps/api/src/main/resources/db/migration/V2__spring_session_schema.sql`
- Create: `apps/api/src/main/resources/application-local.yml`
- Create: `apps/api/src/test/resources/application-test.yml`
- Create: `apps/api/src/test/java/com/interviewrecord/support/MySqlTestDatabase.java`
- Create: `apps/api/src/test/java/com/interviewrecord/support/MySqlIntegrationTestBase.java`
- Create: `apps/api/src/test/java/com/interviewrecord/common/migration/MigrationTest.java`
- Create: `apps/api/src/main/java/com/interviewrecord/common/config/ClockConfig.java`
- Create: `apps/api/src/main/java/com/interviewrecord/common/error/ApiError.java`
- Create: `apps/api/src/main/java/com/interviewrecord/common/error/FieldViolation.java`
- Create: `apps/api/src/main/java/com/interviewrecord/common/error/GlobalExceptionHandler.java`
- Create: `apps/api/src/test/java/com/interviewrecord/common/error/GlobalExceptionHandlerTest.java`
- Modify: `apps/api/src/main/resources/application.yml`

**Interfaces:**
- Consumes: runnable API from Task 1.
- Produces: Flyway schema version 2, `Clock` bean, `ApiError(String code, String message, Map<String,String> fieldErrors, String traceId)`, `MySqlTestDatabase.configure(DynamicPropertyRegistry)`, and the shared `MySqlIntegrationTestBase`.

- [ ] **Step 1: Write a failing validation-error contract test**

```java
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ValidationProbeController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {
    @Autowired MockMvc mvc;

    @Test
    void returnsStableValidationShape() throws Exception {
        mvc.perform(post("/test/validation")
                        .with(user("probe"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.name").value("名称不能为空"));
    }

    @RestController
    static class ValidationProbeController {
        @PostMapping("/test/validation")
        void validate(@Valid @RequestBody ValidationProbe request) {
        }
    }

    record ValidationProbe(@NotBlank(message = "名称不能为空") String name) {
    }
}
```

- [ ] **Step 2: Run the focused test and verify failure**

```powershell
Set-Location apps/api
.\mvnw.cmd -Dtest=GlobalExceptionHandlerTest test
```

Expected: FAIL because `ApiError` and `GlobalExceptionHandler` do not exist.

- [ ] **Step 3: Create the account schema migration**

`V1__account_schema.sql` creates these tables with `BIGINT AUTO_INCREMENT` primary keys and `DATETIME(6)` UTC timestamps:

```sql
CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    email_verified_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_preferences (
    user_id BIGINT NOT NULL,
    time_zone VARCHAR(64) NOT NULL,
    theme VARCHAR(32) NOT NULL,
    interview_reminder_offsets JSON NOT NULL,
    deadline_reminder_offsets JSON NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE email_verification_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash BINARY(32) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_verification_hash (token_hash),
    KEY ix_email_verification_user (user_id),
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE password_reset_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash BINARY(32) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_hash (token_hash),
    KEY ix_password_reset_user (user_id),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rate_limit_buckets (
    action_name VARCHAR(40) NOT NULL,
    subject_hash BINARY(32) NOT NULL,
    window_started_at DATETIME(6) NOT NULL,
    attempt_count INT NOT NULL,
    blocked_until DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (action_name, subject_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE job_types (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_types_user_name (user_id, name),
    CONSTRAINT fk_job_types_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE position_statuses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(40) NOT NULL,
    sort_order INT NOT NULL,
    color VARCHAR(7) NOT NULL,
    statistics_category VARCHAR(16) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_statuses_user_name (user_id, name),
    UNIQUE KEY uk_statuses_user_order (user_id, sort_order),
    CONSTRAINT fk_statuses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_status_category CHECK (statistics_category IN ('ACTIVE','SUCCESS','REJECTED','WITHDRAWN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

Copy the official MySQL Spring Session schema into `V2__spring_session_schema.sql`, then set `spring.session.jdbc.initialize-schema=never` so Flyway remains the sole schema owner.

Replace `application.yml` with the production-shaped base configuration:

```yaml
spring:
  application:
    name: interview-record-api
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.jdbc.time_zone: UTC
  flyway:
    enabled: true
  session:
    timeout: 12h
    jdbc:
      initialize-schema: never
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT}
server:
  servlet:
    session:
      cookie:
        name: INTERVIEW_RECORD_SESSION
        http-only: true
        secure: true
        same-site: lax
app:
  frontend-base-url: ${APP_BASE_URL}
```

`application-local.yml` supplies only local Compose defaults and sets the session cookie `secure` flag to `false`. `application-test.yml` sets `spring.jpa.hibernate.ddl-auto=validate`, `spring.session.jdbc.initialize-schema=never`, `spring.mail.host=localhost`, `spring.mail.port=1`, and `app.frontend-base-url=http://localhost:5173`; datasource properties continue to come only from `MySqlTestDatabase`. Tests that exercise delivery import `FakeMailGateway` as a `@Primary` test bean, so port 1 is never contacted. Do not put a production fallback password in any profile.

- [ ] **Step 4: Implement external-or-container MySQL test configuration**

`MySqlTestDatabase.configure` must use `TEST_DB_URL`, `TEST_DB_USERNAME`, and `TEST_DB_PASSWORD` together when all are present. Otherwise it starts `mysql:8.4.9` through Testcontainers. It must throw an explicit error if only some external variables are supplied or if the JDBC schema name is not exactly `interview_record_test`.

```java
public final class MySqlTestDatabase {
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4.9")
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
            URI jdbcUri = URI.create(url.substring("jdbc:".length()));
            String schema = jdbcUri.getPath().substring(1);
            if (!"interview_record_test".equals(schema)) {
                throw new IllegalStateException(
                        "External tests require the interview_record_test schema");
            }
        }
    }
}
```

Every MySQL integration/API test extends this base class so the `test` profile and dedicated-database guard cannot be forgotten:

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class MySqlIntegrationTestBase {
    @DynamicPropertySource
    protected static void database(DynamicPropertyRegistry registry) {
        MySqlTestDatabase.configure(registry);
    }
}
```

`MigrationTest` extends it and verifies the real MySQL schema:

```java
class MigrationTest extends MySqlIntegrationTestBase {
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;

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
```

- [ ] **Step 5: Implement the error contract and UTC clock**

Use immutable Java records:

```java
public record ApiError(
        String code,
        String message,
        Map<String, String> fieldErrors,
        String traceId) {
}

@Configuration
class ClockConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
```

`GlobalExceptionHandler` maps Bean Validation failures to `VALIDATION_ERROR`, authentication to `UNAUTHENTICATED`, access denial to `FORBIDDEN`, conflicts to `CONFLICT`, and unexpected exceptions to `INTERNAL_ERROR` without returning stack traces.

- [ ] **Step 6: Run migration and error tests**

With Docker:

```powershell
docker compose up -d mysql
Set-Location apps/api
.\mvnw.cmd -Dtest=GlobalExceptionHandlerTest,MigrationTest test
```

Without Docker, first create the exact empty `interview_record_test` schema and least-privilege test user manually, then run:

```powershell
$env:TEST_DB_URL='jdbc:mysql://localhost:3306/interview_record_test?serverTimezone=UTC'
$env:TEST_DB_USERNAME='interview_record_test'
$testDbCredential = Get-Credential -UserName 'interview_record_test' -Message 'Enter the dedicated test database password'
$env:TEST_DB_PASSWORD=$testDbCredential.GetNetworkCredential().Password
Set-Location apps/api
.\mvnw.cmd -Dtest=GlobalExceptionHandlerTest,MigrationTest test
```

Expected: Flyway reaches version 2; tables and constraints exist; focused tests pass. Never put the actual password into the repository or shell output captured in documentation.

- [ ] **Step 7: Commit database and error foundations**

```powershell
git add apps/api/src/main apps/api/src/test
git commit -m "feat: add MySQL schema and API error contract"
```

### Task 3: Register users and create user defaults atomically

**Files:**
- Create: `apps/api/src/main/java/com/interviewrecord/auth/domain/User.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/application/RegisterCommand.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/application/RegistrationResult.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/application/RegistrationService.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/infrastructure/JpaUserRepository.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/domain/EmailVerificationToken.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/infrastructure/JpaEmailVerificationTokenRepository.java`
- Create: `apps/api/src/main/java/com/interviewrecord/defaults/application/UserDefaultsService.java`
- Create: `apps/api/src/main/java/com/interviewrecord/defaults/domain/DefaultJobType.java`
- Create: `apps/api/src/main/java/com/interviewrecord/defaults/domain/DefaultPositionStatus.java`
- Create: `apps/api/src/main/java/com/interviewrecord/defaults/infrastructure/JpaJobTypeRepository.java`
- Create: `apps/api/src/main/java/com/interviewrecord/defaults/infrastructure/JpaPositionStatusRepository.java`
- Create: `apps/api/src/main/java/com/interviewrecord/preference/domain/Theme.java`
- Create: `apps/api/src/main/java/com/interviewrecord/preference/domain/UserPreference.java`
- Create: `apps/api/src/main/java/com/interviewrecord/preference/infrastructure/JpaUserPreferenceRepository.java`
- Create: `apps/api/src/main/java/com/interviewrecord/common/token/IssuedToken.java`
- Create: `apps/api/src/main/java/com/interviewrecord/common/token/SecureTokenService.java`
- Create: `apps/api/src/main/java/com/interviewrecord/common/config/PasswordConfig.java`
- Create: `apps/api/src/main/java/com/interviewrecord/common/config/SecurityConfig.java`
- Create: `apps/api/src/main/java/com/interviewrecord/common/security/JsonAuthenticationEntryPoint.java`
- Create: `apps/api/src/main/java/com/interviewrecord/common/security/JsonAccessDeniedHandler.java`
- Create: `apps/api/src/main/java/com/interviewrecord/common/security/PasswordPolicy.java`
- Create: `apps/api/src/main/java/com/interviewrecord/mail/application/MailGateway.java`
- Create: `apps/api/src/main/java/com/interviewrecord/mail/infrastructure/SmtpMailGateway.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/application/RateLimitService.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/infrastructure/JpaRateLimitBucketRepository.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/api/AuthController.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/api/AuthDtos.java`
- Test: `apps/api/src/test/java/com/interviewrecord/auth/application/RegistrationServiceTest.java`
- Test: `apps/api/src/test/java/com/interviewrecord/auth/api/RegistrationApiTest.java`
- Test: `apps/api/src/test/java/com/interviewrecord/auth/application/RateLimitServiceTest.java`
- Test support: `apps/api/src/test/java/com/interviewrecord/support/FakeMailGateway.java`

**Interfaces:**
- Consumes: `Clock`, MySQL schema, `ApiError`.
- Produces: the public-auth/CSRF security baseline, `RegistrationService.register(RegisterCommand) -> RegistrationResult`, `RateLimitService.check(...)`, `PasswordPolicy.validate(String)`, `SecureTokenService.issue(Duration) -> IssuedToken`, `MailGateway.sendVerificationEmail(String email, String rawToken)`, and `POST /api/v1/auth/register`.

`FakeMailGateway` keeps verification/reset messages in thread-safe in-memory lists and exposes a nested `@TestConfiguration` with a `@Primary MailGateway` bean. Every integration test that triggers mail extends `MySqlIntegrationTestBase` and uses `@Import(FakeMailGateway.Config.class)`; no test may contact SMTP.

- [ ] **Step 1: Write a failing registration transaction test**

```java
@Test
void registrationCreatesUnverifiedUserAndAllDefaults() {
    RegistrationResult result = registrationService.register(
            new RegisterCommand(" USER@example.com ", "Password123", "小林",
                    "Asia/Shanghai", "127.0.0.1"));

    assertThat(result.normalizedEmail()).isEqualTo("user@example.com");
    assertThat(userRepository.requireById(result.userId()).isVerified()).isFalse();
    assertThat(jobTypeRepository.findNamesByUserId(result.userId()))
            .containsExactly("秋招", "日常实习");
    assertThat(statusRepository.findNamesByUserIdOrderBySortOrder(result.userId()))
            .containsExactly("待投递", "已投递", "简历筛选中", "笔试/测评中",
                    "面试中", "Offer", "未通过", "已放弃");
    assertThat(preferenceRepository.requireByUserId(result.userId()).theme())
            .isEqualTo(Theme.GRAPHITE_CORAL);
    assertThat(fakeMailGateway.verificationMessages()).hasSize(1);
}
```

Also test duplicate email with case differences, invalid time zone fallback to `Asia/Shanghai`, passwords shorter than eight characters, passwords without both a letter and a digit, passwords longer than 72 UTF-8 bytes, five registrations per email/IP per hour, and SMTP failure leaving the committed account and hashed verification token intact.

- [ ] **Step 2: Run registration tests and verify failure**

```powershell
Set-Location apps/api
.\mvnw.cmd -Dtest=RegistrationServiceTest test
```

Expected: FAIL because registration types and repositories do not exist.

- [ ] **Step 3: Implement secure token issuance**

```java
public record IssuedToken(String rawValue, byte[] sha256, Instant expiresAt) {
    public IssuedToken {
        sha256 = sha256.clone();
    }

    @Override
    public byte[] sha256() {
        return sha256.clone();
    }
}

@Component
final class SecureTokenService {
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    IssuedToken issue(Duration lifetime) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new IssuedToken(raw, sha256(raw), clock.instant().plus(lifetime));
    }
}
```

Use SHA-256 over UTF-8 raw token bytes and constant-time hash comparison. Never log or return the raw token from API JSON.

`PasswordConfig` exposes `PasswordEncoderFactories.createDelegatingPasswordEncoder()`, which stores new hashes with the `{bcrypt}` identifier and keeps the format upgradeable. `PasswordPolicy` requires 8–72 UTF-8 bytes with at least one Unicode letter and one digit; registration and reset reuse this policy, while login and deletion-password checks use the same encoder without reapplying the current creation policy to an existing password.

- [ ] **Step 4: Implement user/default entities and registration service**

First implement the reusable database-backed limiter used throughout the auth phase:

```java
public interface RateLimitService {
    void check(String action, String subject, int limit, Duration window, Duration blockDuration);
    void reset(String action, String subject);
}
```

Hash `action + ':' + normalizedSubject` with SHA-256 before persistence and lock the bucket row during increment. Registration uses separate email and IP buckets with a five-request, one-hour window.

Create a missing bucket with `INSERT IGNORE`, then load that exact `(action_name, subject_hash)` row with `SELECT ... FOR UPDATE` and update it in the same transaction. Normalize emails by trim/lowercase and IP addresses to their canonical textual form before hashing. This makes the first concurrent attempts converge on one row instead of bypassing the counter through a duplicate-insert race.

`RegistrationService.register` must be `@Transactional` and perform this order:

1. normalize and validate email;
2. rate-limit registration using separate normalized email and IP buckets;
3. reject duplicate normalized email;
4. hash password with Spring Security `PasswordEncoder`;
5. save unverified user;
6. save preference defaults: requested valid zone or `Asia/Shanghai`, `GRAPHITE_CORAL`, interview offsets `[1440,30]`, deadline offsets `[1440]` in minutes;
7. insert the two default job types and eight statuses with exact categories/order/colors;
8. issue a 24-hour verification token and save only its hash;
9. register an after-commit callback that sends the raw token to `MailGateway` and catches `MailException` without logging the token.

Use these exact status defaults:

| Order | Name | Color | Statistics category |
| --- | --- | --- | --- |
| 1 | 待投递 | `#6B7280` | `ACTIVE` |
| 2 | 已投递 | `#3B82F6` | `ACTIVE` |
| 3 | 简历筛选中 | `#8B5CF6` | `ACTIVE` |
| 4 | 笔试/测评中 | `#F59E0B` | `ACTIVE` |
| 5 | 面试中 | `#E15F55` | `ACTIVE` |
| 6 | Offer | `#10B981` | `SUCCESS` |
| 7 | 未通过 | `#EF4444` | `REJECTED` |
| 8 | 已放弃 | `#9CA3AF` | `WITHDRAWN` |

If mail delivery fails after commit, the account and hashed token remain valid, the API still returns `201`, and the user can invoke resend to create and send a new token. Record only a structured delivery-failure metric and error code; do not persist or log the raw token.

`SmtpMailGateway` constructs the verification link by appending the URL-encoded raw token to `${app.frontend-base-url}/verify-email?token=`, sends a Chinese plain-text message, and never includes the password or any user business data. The token may exist only in that outbound message and the short-lived in-memory call stack.

- [ ] **Step 5: Implement registration API**

```java
public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 80) String displayName,
        @Size(max = 64) String timeZone) {
}

public record RegisterResponse(String email, boolean verificationRequired) {
}
```

`POST /api/v1/auth/register` returns `201` and `{ "email": "user@example.com", "verificationRequired": true }`. It never returns database IDs or tokens.

Create the security baseline in the same step: permit `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/v1/auth/verify-email`, `/api/v1/auth/resend-verification`, `/api/v1/auth/forgot-password`, `/api/v1/auth/reset-password`, `/api/v1/auth/csrf`, and `/actuator/health`; require authentication for every other request; use `CookieCsrfTokenRepository` with `XSRF-TOKEN`/`X-XSRF-TOKEN`; disable form login and HTTP Basic. `JsonAuthenticationEntryPoint` returns `UNAUTHENTICATED`; `JsonAccessDeniedHandler` returns `INVALID_CSRF_TOKEN` for CSRF failures and `FORBIDDEN` otherwise, both using the same `ApiError` JSON shape without stack traces. Registration and every later public mutation remain CSRF-protected, so `RegistrationApiTest` and other MockMvc mutation tests must include `.with(csrf())`.

- [ ] **Step 6: Run unit and API registration tests**

```powershell
.\mvnw.cmd -Dtest=RegistrationServiceTest,RegistrationApiTest test
```

Expected: PASS, including duplicate, rate-limit, default creation, and post-commit mail-failure cases.

- [ ] **Step 7: Commit registration**

```powershell
git add apps/api/src/main apps/api/src/test
git commit -m "feat: add transactional user registration"
```

### Task 4: Verify email and support controlled resend

**Files:**
- Create: `apps/api/src/main/java/com/interviewrecord/auth/application/EmailVerificationService.java`
- Modify: `apps/api/src/main/java/com/interviewrecord/auth/domain/EmailVerificationToken.java`
- Modify: `apps/api/src/main/java/com/interviewrecord/auth/infrastructure/JpaEmailVerificationTokenRepository.java`
- Modify: `apps/api/src/main/java/com/interviewrecord/auth/api/AuthController.java`
- Modify: `apps/api/src/main/java/com/interviewrecord/auth/api/AuthDtos.java`
- Test: `apps/api/src/test/java/com/interviewrecord/auth/application/EmailVerificationServiceTest.java`
- Test: `apps/api/src/test/java/com/interviewrecord/auth/api/EmailVerificationApiTest.java`

**Interfaces:**
- Consumes: `SecureTokenService`, `RateLimitService`, user and token repositories, `MailGateway`.
- Produces: `EmailVerificationService.verify(String rawToken)`, `EmailVerificationService.resend(String email, String clientIp)`, `POST /api/v1/auth/verify-email`, and `POST /api/v1/auth/resend-verification`.

- [ ] **Step 1: Write failing expiry, one-time-use, and resend tests**

```java
@Test
void verificationConsumesTokenAndMarksUserVerified() {
    String raw = fixture.createUnverifiedUserWithToken(clock.instant().plus(Duration.ofHours(1)));

    verificationService.verify(raw);

    assertThat(fixture.user().isVerified()).isTrue();
    assertThat(fixture.token().consumedAt()).isEqualTo(clock.instant());
    assertThatThrownBy(() -> verificationService.verify(raw))
            .isInstanceOf(InvalidTokenException.class);
}
```

Use a mutable test clock. Add tests for expired token, unknown token, already verified resend, recovery after the registration mail gateway failed, 60-second resend cooldown, and maximum five sends per email and per IP per hour.

- [ ] **Step 2: Run the focused tests and verify failure**

```powershell
.\mvnw.cmd -Dtest=EmailVerificationServiceTest,EmailVerificationApiTest test
```

Expected: FAIL because verification behavior is not implemented.

- [ ] **Step 3: Implement verification and resend**

Hash the supplied raw token, query by hash with a row lock, require `consumed_at IS NULL` and `expires_at > now`, mark the user verified and token consumed in one transaction. Resend applies cooldown and hourly email/IP buckets before any account lookup, invalidates all prior unconsumed verification tokens for an existing unverified user, issues a new 24-hour token, and returns the same public response whether the email exists or not. Unknown emails therefore receive the same `202`/`429` timing and status pattern as known emails.

- [ ] **Step 4: Implement HTTP contracts**

- `POST /api/v1/auth/verify-email` with body `{ "token": "..." }` returns `204` on success.
- Invalid, consumed, and expired tokens return `400` with code `INVALID_OR_EXPIRED_TOKEN`.
- `POST /api/v1/auth/resend-verification` always returns `202` with no account-existence signal.
- A request inside the 60-second cooldown returns `429` with `Retry-After`.

- [ ] **Step 5: Run verification tests**

```powershell
.\mvnw.cmd -Dtest=EmailVerificationServiceTest,EmailVerificationApiTest test
```

Expected: PASS.

- [ ] **Step 6: Commit verification**

```powershell
git add apps/api/src/main apps/api/src/test
git commit -m "feat: add email verification lifecycle"
```

### Task 5: Add secure login, JDBC sessions, CSRF, and rate limits

**Files:**
- Modify: `apps/api/src/main/java/com/interviewrecord/common/config/SecurityConfig.java`
- Create: `apps/api/src/main/java/com/interviewrecord/common/security/AuthenticatedUser.java`
- Create: `apps/api/src/main/java/com/interviewrecord/common/security/CurrentUser.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/application/AuthService.java`
- Create: `apps/api/src/main/java/com/interviewrecord/preference/api/MeController.java`
- Modify: `apps/api/src/main/java/com/interviewrecord/auth/application/RateLimitService.java`
- Modify: `apps/api/src/main/java/com/interviewrecord/auth/infrastructure/JpaRateLimitBucketRepository.java`
- Modify: `apps/api/src/main/java/com/interviewrecord/auth/api/AuthController.java`
- Modify: `apps/api/src/main/resources/application.yml`
- Test: `apps/api/src/test/java/com/interviewrecord/auth/api/LoginSessionApiTest.java`
- Modify: `apps/api/src/test/java/com/interviewrecord/auth/application/RateLimitServiceTest.java`

**Interfaces:**
- Consumes: verified users and MySQL session schema.
- Produces: `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/csrf`, `GET /api/v1/me`, and `CurrentUser.require()`.

- [ ] **Step 1: Write failing login/session tests**

```java
@Test
void verifiedUserLogsInAndSessionCanReadCurrentUser() {
    fixture.verifiedUser("user@example.com", "Password123");

    MvcResult login = mvc.perform(post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("{\"email\":\"user@example.com\",\"password\":\"Password123\"}"))
            .andExpect(status().isNoContent())
            .andReturn();

    MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
    mvc.perform(get("/api/v1/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("user@example.com"));
}
```

Also test unverified rejection, wrong-password generic response, session fixation protection, logout invalidation, missing CSRF rejection, unauthenticated `/me`, and two-user separation.

- [ ] **Step 2: Run login tests and verify failure**

```powershell
.\mvnw.cmd -Dtest=LoginSessionApiTest,RateLimitServiceTest test
```

Expected: FAIL because security configuration and endpoints are absent.

- [ ] **Step 3: Implement database-backed rate limiting**

Reuse the interface created in Task 3:

```java
public interface RateLimitService {
    void check(String action, String subject, int limit, Duration window, Duration blockDuration);
    void reset(String action, String subject);
}
```

Hash normalized subjects before persistence. Use `SELECT ... FOR UPDATE` inside a transaction so concurrent attempts cannot bypass counts. Apply login limit 10 failures per email+IP combination within 15 minutes, blocked for 15 minutes. Reset the login failure bucket after successful authentication. Apply separate email and IP buckets to resend and password-reset endpoints.

- [ ] **Step 4: Configure Spring Security and JDBC sessions**

Required rules:

```java
http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login",
                "/api/v1/auth/verify-email", "/api/v1/auth/resend-verification",
                "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                "/api/v1/auth/csrf", "/actuator/health").permitAll()
        .anyRequest().authenticated())
    .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
    .sessionManagement(session -> session.sessionFixation().changeSessionId())
    .requestCache(cache -> cache.disable())
    .formLogin(form -> form.disable())
    .httpBasic(basic -> basic.disable());
```

Set session cookie name `INTERVIEW_RECORD_SESSION`, timeout 12 hours, `SameSite=Lax`, `HttpOnly=true`, and secure outside local profile. Set `spring.session.jdbc.initialize-schema=never`.

`GET /api/v1/auth/csrf` accepts the injected `CsrfToken`, forces token materialization, returns `204`, and causes `CookieCsrfTokenRepository` to set `XSRF-TOKEN`. It must not create an authenticated session by itself.

- [ ] **Step 5: Implement JSON login/logout/current-user endpoints**

Authenticate using Spring Security's `AuthenticationManager`; save the `SecurityContext` to the HTTP session. Return `204` for login/logout. `GET /api/v1/me` returns:

Login accepts `{ "email": "user@example.com", "password": "Password123" }`. Logout accepts no body, invalidates the current session, clears the session cookie, and returns `204`. Wrong credentials and unknown accounts both return `401` with code `INVALID_CREDENTIALS`; unverified accounts return `403` with code `EMAIL_NOT_VERIFIED`.

```json
{
  "id": "42",
  "email": "user@example.com",
  "displayName": "小林",
  "emailVerified": true,
  "timeZone": "Asia/Shanghai",
  "theme": "GRAPHITE_CORAL"
}
```

The public `id` is a string. Never accept it back as authorization context.

- [ ] **Step 6: Run security tests**

```powershell
.\mvnw.cmd -Dtest=LoginSessionApiTest,RateLimitServiceTest test
```

Expected: PASS, including CSRF, fixation, unverified, rate-limit, and two-user cases.

- [ ] **Step 7: Commit session authentication**

```powershell
git add apps/api/src/main apps/api/src/test
git commit -m "feat: add secure session authentication"
```

### Task 6: Implement forgot/reset password and session revocation

**Files:**
- Create: `apps/api/src/main/java/com/interviewrecord/auth/application/PasswordResetService.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/domain/PasswordResetToken.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/infrastructure/JpaPasswordResetTokenRepository.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/infrastructure/SpringSessionRevoker.java`
- Modify: `apps/api/src/main/java/com/interviewrecord/auth/api/AuthController.java`
- Modify: `apps/api/src/main/java/com/interviewrecord/mail/application/MailGateway.java`
- Modify: `apps/api/src/main/java/com/interviewrecord/mail/infrastructure/SmtpMailGateway.java`
- Test: `apps/api/src/test/java/com/interviewrecord/auth/application/PasswordResetServiceTest.java`
- Test: `apps/api/src/test/java/com/interviewrecord/auth/api/PasswordResetApiTest.java`

**Interfaces:**
- Consumes: token service, rate limiter, users, mail, Spring Session JDBC.
- Produces: `PasswordResetService.request(String email, String clientIp)`, `PasswordResetService.reset(String rawToken, String newPassword)`, `POST /api/v1/auth/forgot-password`, and `POST /api/v1/auth/reset-password`.

- [ ] **Step 1: Write failing reset and revocation tests**

```java
@Test
void resetConsumesTokenChangesPasswordAndRevokesEverySession() {
    UserFixture user = fixture.verifiedUserWithTwoSessions();
    String raw = fixture.passwordResetToken(user.id(), clock.instant().plus(Duration.ofMinutes(30)));

    resetService.reset(raw, "NewPassword123");

    assertThat(passwordEncoder.matches("NewPassword123", fixture.reload(user).passwordHash())).isTrue();
    assertThat(fixture.resetToken(raw).consumedAt()).isEqualTo(clock.instant());
    assertThat(sessionRepository.findByPrincipalName(user.email())).isEmpty();
}
```

Also test one-hour expiry, unknown/consumed token, password policy, uniform forgot response for unknown email, five requests per email/IP per hour, and rejection of old password after reset.

- [ ] **Step 2: Run reset tests and verify failure**

```powershell
.\mvnw.cmd -Dtest=PasswordResetServiceTest,PasswordResetApiTest test
```

Expected: FAIL because reset behavior does not exist.

- [ ] **Step 3: Implement request and reset transactions**

Forgot password applies its email/IP limiter before account lookup and otherwise always returns `202`. For a verified existing user, invalidate prior unconsumed reset tokens, create a one-hour token, and send it after commit. Reset locks the token row, verifies expiry/consumption, validates and hashes the new password, consumes the token, invalidates all remaining reset tokens, and revokes all sessions through `FindByIndexNameSessionRepository`.

`POST /api/v1/auth/forgot-password` accepts `{ "email": "user@example.com" }`. `POST /api/v1/auth/reset-password` accepts `{ "token": "...", "newPassword": "NewPassword123" }`. Both are CSRF-protected. `MailGateway.sendPasswordResetEmail` appends the URL-encoded raw token to `${app.frontend-base-url}/reset-password?token=`; invalid, expired, or consumed tokens return `400` with `INVALID_OR_EXPIRED_TOKEN`.

- [ ] **Step 4: Run reset tests**

```powershell
.\mvnw.cmd -Dtest=PasswordResetServiceTest,PasswordResetApiTest test
```

Expected: PASS.

- [ ] **Step 5: Commit password reset**

```powershell
git add apps/api/src/main apps/api/src/test
git commit -m "feat: add password reset and session revocation"
```

### Task 7: Build the Vue authentication flow

**Files:**
- Create: `apps/web/src/shared/api/http.ts`
- Create: `apps/web/src/shared/api/error.ts`
- Create: `apps/web/src/shared/auth/auth.types.ts`
- Create: `apps/web/src/shared/auth/auth.store.ts`
- Create: `apps/web/src/features/auth/api/auth.api.ts`
- Create: `apps/web/src/features/auth/components/LoginForm.vue`
- Create: `apps/web/src/features/auth/components/RegisterForm.vue`
- Create: `apps/web/src/features/auth/components/ForgotPasswordForm.vue`
- Create: `apps/web/src/features/auth/components/ResetPasswordForm.vue`
- Create: `apps/web/src/views/LoginView.vue`
- Create: `apps/web/src/views/RegisterView.vue`
- Create: `apps/web/src/views/VerifyEmailView.vue`
- Create: `apps/web/src/views/ForgotPasswordView.vue`
- Create: `apps/web/src/views/ResetPasswordView.vue`
- Create: `apps/web/src/views/DashboardView.vue`
- Modify: `apps/web/src/app/router.ts`
- Create: `apps/web/src/test/msw-server.ts`
- Test: `apps/web/src/shared/auth/auth.store.spec.ts`
- Test: `apps/web/src/app/router.spec.ts`
- Test: `apps/web/src/features/auth/components/LoginForm.spec.ts`
- Test: `apps/web/src/features/auth/components/RegisterForm.spec.ts`
- Test: `apps/web/src/features/auth/components/ResetPasswordForm.spec.ts`

**Interfaces:**
- Consumes: Phase 1 auth HTTP endpoints and `ApiError`.
- Produces: `useAuthStore()`, route guard metadata `requiresAuth`, and browser routes `/login`, `/register`, `/verify-email`, `/forgot-password`, `/reset-password`, `/app`.

- [ ] **Step 1: Write failing auth-store and route-guard tests**

```ts
it('loads the user once and redirects guests from protected routes', async () => {
  server.use(http.get('/api/v1/me', () => HttpResponse.json({}, { status: 401 })))
  const store = useAuthStore()

  await store.loadCurrentUser()

  expect(store.status).toBe('guest')
  expect(resolveGuard({ meta: { requiresAuth: true } }, store)).toEqual({ name: 'login' })
})
```

Install MSW as the fetch-level test adapter; do not mock internal store methods.

```powershell
Set-Location apps/web
npm.cmd install --save-dev msw
```

- [ ] **Step 2: Run frontend tests and verify failure**

```powershell
Set-Location apps/web
npm.cmd run test:unit -- --run
```

Expected: FAIL because auth API/store/routes do not exist.

- [ ] **Step 3: Implement the CSRF-aware HTTP client**

Create one Axios instance with `baseURL: '/api/v1'`, `withCredentials: true`, `xsrfCookieName: 'XSRF-TOKEN'`, and `xsrfHeaderName: 'X-XSRF-TOKEN'`. Before the first mutation, call `/auth/csrf`; retry a mutation once only when the server returns the explicit CSRF error code. Normalize `ApiError` without discarding `fieldErrors`.

- [ ] **Step 4: Implement auth API and Pinia store**

```ts
export interface CurrentUser {
  id: string
  email: string
  displayName: string
  emailVerified: boolean
  timeZone: string
  theme: 'INDIGO' | 'FOREST' | 'APRICOT' | 'GRAPHITE_CORAL'
}

export type AuthStatus = 'unknown' | 'loading' | 'authenticated' | 'guest'
```

The store exposes `loadCurrentUser`, `login`, and `logout`; it never stores passwords or session tokens. A 401 from `/me` sets `guest`; other errors remain visible for retry.

- [ ] **Step 5: Implement accessible forms and routes**

Use native labels plus Element Plus inputs, inline server `fieldErrors`, disabled submit while pending, and a focusable error summary. Registration success routes to a “check your email” state. Verification reads the token from the query string and never places it in logs. Reset clears password fields after success.

- [ ] **Step 6: Run unit, type, and build checks**

```powershell
npm.cmd run test:unit -- --run
npm.cmd run type-check
npm.cmd run build
```

Expected: all commands pass.

- [ ] **Step 7: Commit frontend authentication**

```powershell
Set-Location ../..
git add apps/web
git commit -m "feat: add Vue authentication flow"
```

### Task 8: Add preferences and account deletion

**Files:**
- Modify: `apps/api/src/main/java/com/interviewrecord/preference/api/MeController.java`
- Create: `apps/api/src/main/java/com/interviewrecord/preference/api/PreferenceDtos.java`
- Create: `apps/api/src/main/java/com/interviewrecord/preference/application/PreferenceService.java`
- Modify: `apps/api/src/main/java/com/interviewrecord/preference/infrastructure/JpaUserPreferenceRepository.java`
- Create: `apps/api/src/main/java/com/interviewrecord/auth/application/AccountDeletionService.java`
- Create: `apps/web/src/features/preferences/api/preferences.api.ts`
- Create: `apps/web/src/features/preferences/components/PreferenceForm.vue`
- Create: `apps/web/src/views/SettingsView.vue`
- Modify: `apps/web/src/app/router.ts`
- Test: `apps/api/src/test/java/com/interviewrecord/preference/api/PreferenceApiTest.java`
- Test: `apps/api/src/test/java/com/interviewrecord/auth/api/AccountDeletionApiTest.java`
- Test: `apps/web/src/features/preferences/components/PreferenceForm.spec.ts`
- Test: `apps/web/src/views/SettingsView.spec.ts`

**Interfaces:**
- Consumes: authenticated user/session and preference defaults.
- Produces: `PATCH /api/v1/me/preferences`, `DELETE /api/v1/me`, and route `/app/settings`.

- [ ] **Step 1: Write failing ownership, validation, and deletion tests**

```java
@Test
void updateUsesAuthenticatedUserNotRequestUserId() {
    SessionCookie alice = fixture.loggedInUser("alice@example.com");
    fixture.verifiedUser("bob@example.com", "Password123");

    mvc.perform(patch("/api/v1/me/preferences")
                    .cookie(alice.cookie())
                    .with(csrf())
                    .contentType(APPLICATION_JSON)
                    .content("{\"timeZone\":\"Asia/Tokyo\",\"theme\":\"FOREST\"}"))
            .andExpect(status().isOk());

    assertThat(fixture.preferences("alice@example.com").timeZone()).isEqualTo("Asia/Tokyo");
    assertThat(fixture.preferences("bob@example.com").timeZone()).isEqualTo("Asia/Shanghai");
}
```

Also test invalid IANA zone, invalid theme, reminder offsets outside 0–10080 minutes, wrong deletion password, cascade deletion of current phase rows, and session revocation.

- [ ] **Step 2: Run focused tests and verify failure**

```powershell
Set-Location apps/api
.\mvnw.cmd -Dtest=PreferenceApiTest,AccountDeletionApiTest test
```

Expected: FAIL because endpoints do not exist.

- [ ] **Step 3: Implement preference and deletion services**

Preference request:

```java
public record UpdatePreferencesRequest(
        @NotBlank @Size(max = 80) String displayName,
        @NotBlank @Size(max = 64) String timeZone,
        @NotNull Theme theme,
        @NotNull List<@Min(0) @Max(10080) Integer> interviewReminderOffsets,
        @NotNull List<@Min(0) @Max(10080) Integer> deadlineReminderOffsets) {
}
```

Normalize reminder offsets to unique descending values. `AccountDeletionService.deleteCurrentUser(userId, password)` rechecks the password, deletes the user in one transaction, then revokes all sessions. Future migrations must retain `ON DELETE CASCADE` to this user root or extend the service explicitly.

`PATCH /api/v1/me/preferences` returns `200` with `displayName`, `timeZone`, `theme`, `interviewReminderOffsets`, and `deadlineReminderOffsets`. `DELETE /api/v1/me` accepts `{ "password": "Password123" }`, captures the normalized email before deleting the user, commits the cascade, revokes sessions by that principal, clears the current cookie, and returns `204`. A wrong password returns `400` with `INVALID_PASSWORD` without deleting anything.

- [ ] **Step 4: Implement settings UI**

The settings page edits display name, time zone, reminder offsets, and theme. Show the four theme names but apply only the Graphite/Coral foundation tokens in this phase; Phase 5 implements complete visual token sets. Account deletion requires a destructive confirmation dialog and password re-entry.

- [ ] **Step 5: Run backend and frontend tests**

```powershell
Set-Location apps/api
.\mvnw.cmd -Dtest=PreferenceApiTest,AccountDeletionApiTest test
Set-Location ../web
npm.cmd run test:unit -- --run
npm.cmd run type-check
```

Expected: all commands pass.

- [ ] **Step 6: Commit preferences and deletion**

```powershell
Set-Location ../..
git add apps/api apps/web
git commit -m "feat: add account preferences and deletion"
```

### Task 9: Add end-to-end account coverage and phase documentation

**Files:**
- Create: `apps/api/src/main/java/com/interviewrecord/mail/infrastructure/CapturingMailGateway.java`
- Create: `apps/api/src/main/resources/application-e2e.yml`
- Modify: `apps/api/src/main/java/com/interviewrecord/mail/infrastructure/SmtpMailGateway.java`
- Create: `apps/web/e2e/account-lifecycle.spec.ts`
- Create: `apps/web/e2e/helpers/captured-mail.ts`
- Create: `scripts/create-test-database.ps1`
- Create: `scripts/verify-phase-1.ps1`
- Modify: `.github/workflows/ci.yml`
- Modify: `README.md`
- Modify: `AGENTS.md`
- Create: `docs/architecture/0001-modular-monolith-and-session-auth.md`

**Interfaces:**
- Consumes: all Phase 1 API and UI contracts.
- Produces: one-command Phase 1 verification and documented architecture decision.

- [ ] **Step 1: Write the failing Playwright account journey**

```ts
test('register, verify, login, update preferences, reset, and delete', async ({ page }) => {
  const email = `e2e-${crypto.randomUUID()}@example.test`
  await page.goto('/register')
  await page.getByLabel('邮箱').fill(email)
  await page.getByLabel('密码').fill('Password123')
  await page.getByLabel('显示名称').fill('端到端用户')
  await page.getByRole('button', { name: '注册' }).click()

  const verificationUrl = await waitForCapturedEmailLink(email, 'VERIFY_EMAIL')
  await page.goto(verificationUrl)
  await expect(page.getByText('邮箱验证成功')).toBeVisible()

  await page.goto('/login')
  await page.getByLabel('邮箱').fill(email)
  await page.getByLabel('密码').fill('Password123')
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/app$/)
})
```

Continue the same test through theme/time-zone update, logout, forgot/reset password using the captured reset link, old-password rejection, new-password login, and account deletion. Add a separate browser context proving another user cannot access the first user's authenticated API.

- [ ] **Step 2: Run the E2E test and verify it fails before missing pieces are fixed**

```powershell
Set-Location apps/web
npm.cmd run test:e2e -- account-lifecycle.spec.ts
```

Expected: FAIL at the first unimplemented or incorrectly wired lifecycle step, not because services failed to start.

- [ ] **Step 3: Implement deterministic service orchestration**

Annotate `SmtpMailGateway` with `@Profile("!e2e")`. Implement `CapturingMailGateway` as an `@Profile("e2e")` `MailGateway` that appends one UTF-8 JSON object per line to the path configured by `app.e2e-mailbox-path`. Each line has this exact shape:

```json
{"recipient":"e2e-user@example.test","type":"VERIFY_EMAIL","url":"http://localhost:5173/verify-email?token=raw-token","createdAt":"2026-08-11T08:00:00Z"}
```

Use `type` values `VERIFY_EMAIL` and `RESET_PASSWORD`. Build URLs with the configured frontend base URL. Create the parent directory when the profile starts, serialize through Jackson, and synchronize file appends so parallel requests cannot interleave. Never log the raw URL or token. `application-e2e.yml` must set the mailbox to `${E2E_MAILBOX_PATH}` and fail startup when it is absent.

`waitForCapturedEmailLink(recipient, type)` reads `E2E_MAILBOX_PATH`, polls for at most ten seconds, returns the newest matching URL, and fails with recipient/type diagnostics that do not print any URL or token. This adapter is permitted only under the `e2e` profile; production and normal development always use SMTP. The verification script deletes the mailbox before and after the run, and `apps/api/target/` remains gitignored.

Use this exact `application-e2e.yml` shape so HTTP localhost E2E can receive cookies without weakening any non-E2E profile:

```yaml
spring:
  mail:
    host: localhost
    port: 1
server:
  servlet:
    session:
      cookie:
        secure: false
app:
  frontend-base-url: http://localhost:5173
  e2e-mailbox-path: ${E2E_MAILBOX_PATH}
```

`scripts/verify-phase-1.ps1` must:

1. validate that the selected database is exactly `interview_record_test`;
2. verify the named MySQL connection without creating, dropping, or truncating unrelated databases;
3. run backend `verify` and frontend unit/type/build checks before starting long-lived child processes;
4. set `E2E_MAILBOX_PATH` to the absolute path `apps/api/target/e2e-mailbox/messages.jsonl` and remove a stale file at that exact path;
5. map `TEST_DB_URL`, `TEST_DB_USERNAME`, and `TEST_DB_PASSWORD` to `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` only in the API child-process environment, then run Flyway by starting that API with the `e2e` profile and start Vite, both as hidden child processes;
6. wait for `/actuator/health` and the Vite port;
7. run the Playwright account lifecycle;
8. stop only child processes started by the script and remove the captured mailbox in `finally`;
9. return a nonzero exit code on any failed step.

Do not embed database passwords in the script. The Docker Compose path may still run Mailpit for manual email preview, but Phase 1 automation must not require Docker or Mailpit.

- [ ] **Step 4: Add the architecture decision and update commands**

`docs/architecture/0001-modular-monolith-and-session-auth.md` records:

- why a modular monolith was selected over microservices;
- why Spring Session JDBC was selected over JWT/local storage;
- why MySQL remains the only shared state store in V1;
- package dependency direction;
- the condition for reconsidering Redis: measured reminder-claim or rate-limit contention that MySQL cannot meet within the PRD latency targets.

Update `README.md` and `AGENTS.md` with the verified Phase 1 commands only.

- [ ] **Step 5: Run the complete phase gate**

```powershell
.\scripts\verify-phase-1.ps1
git diff --check
git status --short
```

Expected:

- backend unit/integration tests pass;
- frontend unit, type, and production build checks pass;
- Playwright account lifecycle passes;
- no whitespace errors;
- `git status` contains only intended Phase 1 files.

- [ ] **Step 6: Commit Phase 1 verification**

```powershell
git add .github README.md AGENTS.md apps scripts docs/architecture
git commit -m "test: verify complete account lifecycle"
```

## Phase 1 Completion Evidence

Before declaring Phase 1 complete, record the exact output summaries from:

```powershell
Set-Location apps/api
.\mvnw.cmd verify
Set-Location ../web
npm.cmd run test:unit -- --run
npm.cmd run type-check
npm.cmd run build
npm.cmd run test:e2e -- account-lifecycle.spec.ts
Set-Location ../..
git diff --check
git status --short --branch
```

The phase is blocked—not complete—only when no explicitly named `interview_record_test` database credentials are available. Docker is optional because E2E email delivery uses the profile-limited capture adapter. Do not substitute an in-memory database or silently skip integration/E2E coverage.
