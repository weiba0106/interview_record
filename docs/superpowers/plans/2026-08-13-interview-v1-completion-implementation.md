# Interview Record V1 Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 PRD 和 `AGENTS.md` 将当前项目整理为可发布的面试记录 V1，并补齐提醒、分享、统计、导出和产品级主题 UI。

**Architecture:** 保留 Spring Boot 4.1 模块化单体和 Vue 3 feature 结构。后端新增 reminders/sharing/insights/export 模块，所有查询通过当前用户上下文隔离；前端新增统一 AppShell、设计令牌和响应式页面，业务组件只负责展示与交互。

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Data JPA, Flyway, MySQL 8.4, Spring Session JDBC, Vue 3, TypeScript, Pinia, Vue Router, Element Plus, Vitest, Playwright。

## Global Constraints

- 产品和行为以 `docs/superpowers/specs/2026-08-10-interview-tracker-prd-design.md` 为准。
- 工程和安全约束以 `AGENTS.md` 为准；不得使用 H2 替代 MySQL。
- 所有业务数据必须绑定 `user_id`，资源查询必须同时带资源 ID 和当前用户 ID。
- 时间统一保存 UTC；自动紧急程度按用户 IANA 时区和 24/72 小时边界计算。
- 迁移只新增版本文件，不修改已发布的 V1-V4。
- 保留用户现有未提交改动，禁止 `git reset --hard`、强制覆盖和批量删除。
- 任何新增行为先写失败测试，再写最小实现。

## File Map

- 后端核心修复：`apps/api/src/main/java/com/interviewrecord/tracking/**`, `interviews/**`, `scheduling/**`, `dashboard/**`。
- 后端新增：`apps/api/src/main/java/com/interviewrecord/reminders/**`, `sharing/**`, `insights/**`, `export/**`。
- 数据库新增：`V5__schedule_type_compatibility.sql`, `V6__reminder_schema.sql`, `V7__sharing_schema.sql`, `V8__export_schema.sql`；不得修改已发布的 V1-V4。
- 前端壳层：`apps/web/src/app/components/AppShell.vue`, `apps/web/src/app/theme.css`, `apps/web/src/app/router.ts`。
- 前端新增：`apps/web/src/features/reminders/**`, `sharing/**`, `insights/**`, `export/**`, 以及对应视图。
- 文档：`README.md`, `.env.example` 和本计划关联的运行说明。

---

### Task 1: Establish a clean baseline and capture regressions

**Files:**
- Modify: existing backend/frontend tests only where current behavior is explicitly covered.
- Create: `docs/superpowers/reports/2026-08-13-v1-audit.md`

- [ ] 记录 `git status --short`、当前提交和现有未提交文件，不回退任何文件。
- [ ] 运行前端 `npm.cmd run test:unit -- --run`, `npm.cmd run type-check`, `npm.cmd run build`。
- [ ] 运行后端 `./mvnw.cmd -DskipTests test-compile` 和可用的 API 契约测试。
- [ ] 把失败项按“数据/权限/API/UI/环境”分类写入审查报告，后续每项必须有回归测试。

### Task 2: Harden existing tracking, interviews, schedules and dashboard

**Files:**
- Modify: `apps/api/src/main/java/com/interviewrecord/tracking/**`
- Modify: `apps/api/src/main/java/com/interviewrecord/interviews/**`
- Modify: `apps/api/src/main/java/com/interviewrecord/scheduling/**`
- Modify: `apps/api/src/main/java/com/interviewrecord/dashboard/**`
- Test: corresponding `apps/api/src/test/java/**` API and service tests

- [ ] 为公司、岗位、状态、招聘类型、面试轮次、日程和 Dashboard 各补一个跨用户拒绝测试。
- [ ] 验证岗位删除、公司删除保护/级联、状态删除迁移、父子资源同用户校验和版本冲突。
- [ ] 让日程类型与 PRD 对齐，统一自动紧急程度结果和前端显示标签。
- [ ] 新增 `V5__schedule_type_compatibility.sql`，在不修改 V4 的前提下把已存在的 `schedule_events` 类型约束兼容为 PRD 的“投递截止、笔试/测评、面试、HR 沟通、Offer 截止、其他”；同步更新领域枚举和 API 标签。
- [ ] 验证面试轮次与日程的双向同步使用事务且不会产生重复日程。
- [ ] 运行相应 API 测试和 `test-compile`，不以放宽断言解决失败。

### Task 3: Add reminder persistence and idempotent delivery

**Files:**
- Create: `apps/api/src/main/resources/db/migration/V6__reminder_schema.sql`
- Create: `apps/api/src/main/java/com/interviewrecord/reminders/domain/**`
- Create: `apps/api/src/main/java/com/interviewrecord/reminders/application/**`
- Create: `apps/api/src/main/java/com/interviewrecord/reminders/api/**`
- Test: reminder unit, MySQL and API tests

- [ ] 先写受控时钟下的 24 小时、30 分钟、取消、修改和重试失败测试。
- [ ] 建立 `reminders` 表，包含 `user_id`, `schedule_id`, `idempotency_key`, `scheduled_at`, `status`, `attempt_count`, `next_attempt_at`, `sent_at` 和唯一键。
- [ ] 实现事务性 claim，成功发送只允许一次；失败最多重试三次并记录最终失败。
- [ ] 修改、完成、取消日程时同步更新或取消未发送提醒。
- [ ] 提供设置和单条日程覆盖 API，复用现有偏好数据结构。

### Task 4: Add selective sharing

**Files:**
- Create: `apps/api/src/main/resources/db/migration/V7__sharing_schema.sql`
- Create: `apps/api/src/main/java/com/interviewrecord/sharing/**`
- Create: `apps/web/src/features/sharing/**`
- Create: `apps/web/src/views/SharedPositionView.vue`
- Test: sharing backend isolation/allowlist tests and frontend form tests

- [ ] 先写匿名访问、过期、撤销、删除后失效和字段白名单失败测试。
- [ ] 保存令牌哈希，不在数据库、日志或错误中写明文令牌。
- [ ] 分享创建时保存岗位字段和轮次字段白名单，访问时只按白名单投影。
- [ ] 分享页面不显示编辑入口，设置 `noindex`，并使用统一失效页面。
- [ ] 岗位详情增加创建、复制、撤销和预览流程。

### Task 5: Add statistics and export

**Files:**
- Create: `apps/api/src/main/java/com/interviewrecord/insights/**`
- Create: `apps/api/src/main/java/com/interviewrecord/export/**`
- Create: `apps/web/src/features/insights/**`, `apps/web/src/features/export/**`
- Create: statistics and export API/view tests

- [ ] 先写 PRD 三个转化率的分母为零、日期范围和招聘类型筛选测试。
- [ ] 实现状态分布、招聘类型、投递趋势、面试轮次和 Offer 指标，所有查询绑定当前用户。
- [ ] 实现 UTF-8 BOM CSV ZIP，至少包含 companies/positions/interview_rounds/interview_questions/schedules/statuses。
- [ ] 实现 JSON 完整备份，排除密码、会话和所有认证令牌。
- [ ] 下载令牌短期、一次性、按用户隔离，补充过期和重复下载测试；导出数据库迁移使用 `V8__export_schema.sql`。

### Task 6: Rebuild the product UI shell and themes

**Files:**
- Create: `apps/web/src/app/components/AppShell.vue`, `PageHeader.vue`, `StatusMessage.vue`
- Modify: `apps/web/src/app/theme.css`, `apps/web/src/app/App.vue`, `apps/web/src/app/router.ts`
- Modify: all authenticated views and shared feature components
- Test: responsive, theme and accessibility unit tests

- [ ] 先写主题切换、默认主题、刷新持久化、375px 导航和紧急状态语义测试。
- [ ] 用 CSS design tokens 实现 INDIGO、FOREST、APRICOT、GRAPHITE_CORAL 四套完整主题。
- [ ] 使用桌面左侧导航、顶部用户区和移动底部导航/折叠菜单替换现有简易顶部导航。
- [ ] Dashboard 移动端先显示紧急日程和指标，再显示岗位；桌面端显示高密度表格。
- [ ] 每个紧急状态同时提供颜色、图标、文字和剩余时间，不依赖颜色单独传达信息。
- [ ] 统一空状态、加载、错误、无权限和成功反馈，补键盘焦点和 `aria-live`。

### Task 7: Finish missing settings and navigation flows

**Files:**
- Modify: `apps/web/src/views/SettingsView.vue`
- Modify: status/job-type managers, router and navigation
- Test: settings, navigation and responsive view tests

- [ ] 在设置中加入状态管理、招聘类型管理、提醒偏好、主题切换、导出和删除账号。
- [ ] 增加统计、分享和导出页面入口，保证未实现或无权限状态有明确反馈。
- [ ] 保持账号删除前密码确认和删除后会话失效。

### Task 8: Documentation, migration verification and release gate

**Files:**
- Modify: `README.md`, `.env.example`
- Create/modify: deployment and verification notes

- [ ] 更新本地 MySQL、Flyway、SMTP、前端和生产部署命令，删除与当前实现不一致的说明。
- [ ] 在干净 MySQL 和代表性旧数据数据库上运行 V1-V7 迁移检查。
- [ ] 运行前端 Vitest、类型检查、生产构建；运行后端 API/单元/迁移测试。
- [ ] 安装浏览器后运行 Playwright 核心流程：注册、创建公司岗位、多轮面试、日程、主题和删除账号。
- [ ] 生成最终审查报告，明确任何因本机没有 Docker、专用测试库或浏览器而未验证的项目。
