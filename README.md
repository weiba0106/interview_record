# Interview Record

用于记录秋招与日常实习求职过程的多用户响应式 Web 应用。当前仓库包含 Phase 1 的可运行工程基础：Spring Boot API、Vue SPA、MySQL Compose 服务与 CI。

## Runtime

- Java 21 and Maven Wrapper 3.9.16
- Node 24 LTS and npm 11
- MySQL 8.4.9 for local infrastructure
- Vue 3.5.x, TypeScript 6.0.x, and Vite 8.1.x

## Backend checks

Run the focused API validation contract on Windows:

```powershell
Set-Location apps/api
.\mvnw.cmd -Dtest=GlobalExceptionHandlerTest test
Set-Location ../..
```

The validation contract itself was verified through the approved host-Maven fallback: 1 test ran with 0 failures and 0 errors. The repaired wrapper reached its pinned Maven 3.9.16 distribution with `mvnw.cmd -version`; this workspace's sandbox then blocked the wrapper test command while it attempted to download the Spring Boot parent from Maven Central. Re-run the command in a network-enabled environment to verify the wrapper path end to end.

Run the real-MySQL Flyway migration check:

```powershell
Set-Location apps/api
.\mvnw.cmd -Dtest=MigrationTest test
Set-Location ../..
```

The migration test uses the exact external database variables only when all three are set:

```powershell
$env:TEST_DB_URL='jdbc:mysql://localhost:3306/interview_record_test?serverTimezone=UTC'
$env:TEST_DB_USERNAME='interview_record_test'
$env:TEST_DB_PASSWORD='<dedicated-test-database-password>'
```

`TEST_DB_URL` must select exactly the `interview_record_test` schema; partial variables and every other schema are rejected before a connection is made. If none of the variables are present, the test starts `mysql:8.4.9` with Testcontainers. Docker or an already-provisioned dedicated external test database is therefore required. Docker and external test-database credentials are unavailable in this workspace, so the real MySQL migration assertion has not been completed here.

## Complete Phase 1 verification

Use only an existing dedicated `interview_record_test` database, all three `TEST_DB_*` variables above, the MySQL client on `PATH`, and installed Playwright browsers. The command checks the exact schema before connecting, never creates, drops, or truncates a database, runs backend/frontend checks, starts profile-limited E2E services, and cleans up only its own child processes and captured mailbox.

```powershell
.\scripts\verify-phase-1.ps1
```

For a new local test schema, the explicit, interactive `scripts/create-test-database.ps1` command can create only `interview_record_test`; it never drops, truncates, or grants access. It requires an administrator user/password and honours PowerShell `-WhatIf`.

```powershell
.\scripts\create-test-database.ps1 -AdminUser root -AdminPassword (Read-Host -AsSecureString)
```

The account lifecycle Playwright test requires the `e2e` API profile, where SMTP is replaced by a local UTF-8 JSONL mailbox. This profile is automation-only and requires `E2E_MAILBOX_PATH`; normal profiles continue to use SMTP. In this workspace the test was exercised as RED but cannot launch because Playwright browser binaries are not installed.

The lifecycle test deletes each user through the authenticated account-deletion flow in `finally`. If a run fails after registration but before a user can verify and log in, there is deliberately no unauthenticated test-cleanup endpoint; that dedicated test database may need its normal reprovisioning process before another shared run.

Run the frontend checks on Windows:

```powershell
Set-Location apps/web
npm.cmd run test:unit -- --run
npm.cmd run type-check
npm.cmd run build
Set-Location ../..
```

On POSIX shells, replace `npm.cmd` with `npm`.

## Local services

Copy `.env.example` to `.env` only when local overrides are needed, then use Docker Compose to start MySQL on port 3307 and Mailpit on ports 1025 and 8025:

```sh
docker compose up -d
```

## Repository layout

- `apps/api`: Spring Boot API and backend tests.
- `apps/web`: Vue SPA, unit tests, and Playwright configuration.
- `.github/workflows`: CI verification workflow.
- `docs`: approved product and implementation documents.
