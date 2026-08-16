# Interview Record

用于记录秋招与日常实习求职过程的多用户响应式 Web 应用。仓库包含 Phase 1 的可运行工程基础(Spring Boot API、Vue SPA、MySQL Compose 服务与 CI),以及 Phase 2/3 的面试记录主体功能。

## Product baseline

Phase 1:注册、邮箱验证、登录、会话认证、密码重置、账号设置与偏好(含四套主题)。

Phase 2/3(本次新增):

- 公司管理:CRUD、同名重复提示可确认继续、有岗位时二次确认删除并展示级联影响。
- 招聘类型与状态:默认秋招/日常实习,支持新增、编辑、停用;状态带统计分类(进行中/成功/未通过/放弃),删除前强制迁移岗位。
- 岗位管理:CRUD、归档/恢复、新增岗位时可直接输入新公司名称快速创建公司(同名自动复用)、按公司/招聘类型/状态筛选、关键词搜索、排序与分页、列表内直接改状态;删除级联面试轮次与日程,需二次确认。
- 面试记录:一岗位多轮(轮次序号唯一)、面试类型与结果、过程记录、复盘、问题与回答列表、可同步创建面试日程,面试时间变更自动同步关联日程。
- 日程:面试/笔试/测评/投递截止/Offer 截止/自定义六种类型;紧急程度按逾期、24 小时、72 小时边界自动计算,支持手动覆盖;支持完成、取消。
- Dashboard(`/app`):岗位总数、进行中岗位、未来 7 天待处理日程、Offer 数量四项指标;岗位列表;待处理日程按紧急程度排序、逾期置顶、可直接完成/取消;新用户空状态引导。

后端 Flyway 迁移:`V3__company_position_schema.sql`(companies、positions)、`V4__interview_schedule_schema.sql`(interview_rounds、interview_questions、schedule_events)。job_types 与 position_statuses 在 Phase 1 的 V1 中已预置。所有新表均包含 `user_id` 与外键级联,全部查询按用户隔离。

前端页面路由:`/app`(C 方案混合工作台)、`/app/companies`、`/app/positions`（支持表格/看板切换）、`/app/positions/new`、`/app/positions/:id`、`/app/positions/:id/edit`、`/app/schedules`、`/app/insights`、`/app/settings`。登录后的顶部主题按钮提供四套主题快捷切换，设置页仍可编辑完整偏好。

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

The lifecycle test deletes each verified user through the authenticated account-deletion flow in `finally`, restoring a session first when needed. If a run fails after registration but before email verification, there is deliberately no unauthenticated test-cleanup endpoint; that dedicated test database may need its normal reprovisioning process before another shared run. Authenticated cleanup failures make a successful journey fail or are attached to an already-failing journey as diagnostics.

Run the frontend checks on Windows:

```powershell
Set-Location apps/web
npm.cmd run test:unit -- --run
npm.cmd run type-check
npm.cmd run build
Set-Location ../..
```

On POSIX shells, replace `npm.cmd` with `npm`.

前端源代码更新后需要重新执行 `npm.cmd run build`；如果通过静态服务器或 Nginx 托管，请将新的 `apps/web/dist` 发布到站点目录并 reload。浏览器仍显示旧版时使用 `Ctrl+F5` 清理旧构建缓存。

## Phase 2/3 verification evidence

Phase 2/3 changes were verified on this workspace with the commands above (where the sandbox blocks `npm.cmd`/`mvnw.cmd`, the equivalent local binaries were invoked directly):

- Backend API contract tests (`*ApiTest`, MockMvc over WebApplicationContext): 54 tests, 0 failures, 0 errors. Coverage includes company CRUD and duplicate confirmation, position filtering/sorting/paging, cross-user isolation for every resource, interview round order and duplicate-round rejection, schedule urgency calculation, cascade delete and delete protection, and dashboard statistics.
- Backend unit tests (urgency rules, auth, rate limiting config, CSRF filter, error contract): 18 tests passed.
- Application context smoke tests: 2 tests passed.
- Frontend unit tests (Vitest): 14 files, 34 tests passed, including dashboard metrics/empty state, position table filtering, position form validation, interview round form, and urgency color mapping.
- Frontend type check (`vue-tsc --build`) and production build (`vite build`): both succeed.

Known environment limitations on this workspace:

- The 7 MySQL-backed tests (`MigrationTest`, `TrackingMysqlIntegrationTest`, `RegistrationServiceTest`, `PasswordResetServiceTest`, `EmailVerificationServiceTest`, `RateLimitServiceTest`, `UserPreferenceJpaMappingTest`) require Docker/Testcontainers or the dedicated `interview_record_test` credentials and have not run here; CI runs them against `mysql:8.4.9`.
- Playwright browsers are not installed, so E2E specs are only discovered, not executed.
- The Maven wrapper path is sandbox-blocked during dependency download; host Maven (3.6.3, offline) was used instead.

## Local services

Copy `.env.example` to `.env` only when local overrides are needed, then use Docker Compose to start MySQL on port 3307 and Mailpit on ports 1025 and 8025:

```sh
docker compose up -d
```

### Windows 本地启动 API

如果 MySQL 使用本机 `3306` 端口、数据库为 `interview_record_local`，可以直接运行启动脚本。脚本会安全地交互式询问数据库密码和 QQ 邮箱授权码，不会把凭据写入仓库：

```powershell
.\scripts\start-local-api.ps1
```

脚本默认连接 `interview_local` 用户，并启动 `local` profile。也可以覆盖连接参数，例如使用 Mailpit：

```powershell
.\scripts\start-local-api.ps1 `
  -DbUrl 'jdbc:mysql://127.0.0.1:3306/interview_record_local?serverTimezone=UTC' `
  -DbUsername 'interview_local' `
  -MailHost 'localhost' `
  -MailPort 1025
```

API 启动后地址为 `http://localhost:8080`；前端仍在 `apps/web` 运行 `npm.cmd run dev`，访问 `http://localhost:5173`。

## Repository layout

- `apps/api`: Spring Boot API and backend tests.
- `apps/web`: Vue SPA, unit tests, and Playwright configuration.
- `.github/workflows`: CI verification workflow.
- `docs`: approved product and implementation documents.

## 当前 V1 完成度补充

本轮按 PRD 补齐了 V5 日程类型兼容迁移、V6 持久化提醒、V7 白名单分享、用户隔离统计，以及前端四主题应用壳、统计页、分享配置弹窗和匿名分享页。统计接口为 `GET /api/v1/insights`，匿名分享页为 `/share/:token`。

仍需单独补齐或在发布门禁中验证的项目：CSV/ZIP 导出、单条日程提醒自定义覆盖、真实 MySQL/Testcontainers 集成测试，以及安装 Playwright 浏览器后的完整 E2E。不能把这些环境未验证项视为已通过。
