# Interview Record

面向秋招与日常实习求职者的多用户响应式 Web 应用。V1 功能已按 PRD 完整落地:账号生命周期、公司与岗位追踪、多轮面试记录、日程与紧急程度、邮件提醒、只读分享、统计概览、数据导出、四套主题与暗色模式,并额外提供题库复习页。

## 产品与设计基线

- [秋招与日常实习面试记录平台 PRD](docs/superpowers/specs/2026-08-10-interview-tracker-prd-design.md)
- [V1 分阶段交付路线图](docs/superpowers/plans/2026-08-11-interview-platform-roadmap.md)
- [第一阶段实施计划](docs/superpowers/plans/2026-08-11-foundation-auth-implementation.md)
- [架构决策:模块化单体 + 会话认证](docs/architecture/0001-modular-monolith-and-session-auth.md)

## 功能总览

- **账号与安全**:邮箱注册(忽略大小写去重)、24 小时验证链接、60 秒冷却重发、登录/退出、忘记密码与一次性重置链接、账号删除需重新输入密码;注册/登录/重发/重置均有服务端速率限制;会话使用 Spring Session JDBC + `HttpOnly` Cookie + CSRF 令牌对。所有业务查询强制带 `user_id`,跨用户访问统一返回安全错误。
- **公司管理**:CRUD、同名重复提示可确认后继续、存在岗位时删除需二次确认并展示级联影响(岗位/面试/日程/分享数量)。
- **招聘类型与状态**:默认秋招/日常实习与 8 个有序默认状态(注册时按用户复制);支持新增、改名、排序、颜色、停用与统计分类映射(进行中/成功/未通过/放弃);被岗位使用的状态删除前强制迁移。
- **岗位管理**:CRUD、归档/恢复、新增岗位时可输入新公司名称快速创建公司(同名自动复用)、关键词搜索、按公司/招聘类型/状态/日期/归档筛选、排序与分页(默认每页 20)、列表内就地改状态;删除级联面试轮次、日程与分享,需二次确认。
- **多轮面试记录**:轮次序号唯一、面试类型与结果、多条题目与回答、过程记录与复盘总结(富文本经服务端 jsoup 白名单清洗);填写时间可同步创建/更新面试日程。
- **题库复习**(`/app/questions`):汇总全部面试题,按分类与关键词分页检索,支持随机抽题与答案遮盖复习。
- **日程与紧急程度**:投递截止/笔试测评/面试/HR 沟通/Offer 截止/其他六类;紧急程度按用户时区以逾期、24 小时、72 小时边界自动计算,支持手动覆盖与清除恢复;完成/取消状态;面试轮次与日程双向同步。
- **邮件提醒**:持久化提醒(V6),默认提前 24 小时与 2 小时(V11),单条日程可自定义覆盖(V8);数据库行级幂等领取,失败自动重试最多 3 次并在日程详情显示失败状态;后台任务每分钟轮询(`ReminderDeliveryJob`)。
- **只读分享**:岗位字段与面试轮次/字段级白名单、1/7/30 天或永久有效期、随时撤销;令牌仅存 SHA-256 摘要;匿名页 `/share/:token` 带 `noindex`,失效统一提示。
- **统计**:工作台四项指标(未归档岗位、进行中、未来 7 天日程、Offer);统计页 `/app/insights` 提供状态分布、按招聘类型统计、投递/面试趋势与三项转化率(面试触达率、Offer 转化率、轮次通过率),分母为零显示"暂无数据"。
- **数据导出**:设置页发起 CSV ZIP(UTF-8 BOM,含 companies/positions/interview_rounds/interview_questions/schedules/statuses)与 JSON 完整备份;一次性下载链接 30 分钟失效,后台每小时清理(`ExportCleanupJob`);不含密码散列、会话与令牌。
- **主题与体验**:原始靛蓝、森林青绿、暖杏棕、石墨灰+珊瑚红四套主题(新用户默认石墨珊瑚),顶栏快捷切换 + 设置页完整偏好;另提供暗色模式开关(V12);偏好随账号持久化;375 px 移动端适配。

## 技术栈

- Java 21、Spring Boot 4.1.0、Maven Wrapper 3.9.16
- Node 24 LTS、npm 11、Vue 3.5.x、TypeScript 6.0.x、Vite 8.1.x、Element Plus
- MySQL 8.4 LTS(Flyway 管理 schema)、Spring Session JDBC、jsoup(富文本清洗)
- 本地基础设施:根目录 `compose.yaml`(MySQL 8.4.9 + Mailpit 1.30.0)

## 前端路由

未登录:`/register`、`/verify-email`、`/login`、`/forgot-password`、`/reset-password`、`/share/:token`。

登录后:`/app`(工作台)、`/app/companies`、`/app/companies/:id`、`/app/positions`(表格/看板切换)、`/app/positions/new`、`/app/positions/:id`、`/app/positions/:id/edit`、`/app/questions`(题库)、`/app/schedules`、`/app/schedules/:id`、`/app/insights`、`/app/settings`。

## 数据库迁移(V1–V12)

| 迁移 | 内容 |
| --- | --- |
| V1 | 账号 schema:users、偏好、默认状态/招聘类型副本等 |
| V2 | Spring Session 表 |
| V3 | companies、positions |
| V4 | interview_rounds、interview_questions、schedule_events |
| V5 | 日程类型兼容迁移 |
| V6 | 持久化提醒 reminders |
| V7 | share_links、share_rounds(白名单分享) |
| V8 | 单条日程提醒自定义覆盖 |
| V9 | 富文本 LONGTEXT 字段 |
| V10 | export_files(导出与下载令牌) |
| V11 | 默认提醒偏移(24h/2h) |
| V12 | 暗色模式偏好 |

所有业务表含 `user_id` 与外键级联,高频查询带用户范围索引。

## 本地启动

Docker 环境(MySQL 3307、Mailpit 1025/8025):

```sh
docker compose up -d
```

无 Docker、使用本机 MySQL 3306 时,运行启动脚本(交互式询问数据库密码与邮箱授权码,不写入仓库):

```powershell
.\scripts\start-local-api.ps1
```

脚本默认连接 `interview_local` 用户并启动 `local` profile,也可覆盖参数,例如使用 Mailpit:

```powershell
.\scripts\start-local-api.ps1 `
  -DbUrl 'jdbc:mysql://127.0.0.1:3306/interview_record_local?serverTimezone=UTC' `
  -DbUsername 'interview_local' `
  -MailHost 'localhost' `
  -MailPort 1025
```

API 地址 `http://localhost:8080`;前端在 `apps/web` 运行 `npm.cmd run dev`,访问 `http://localhost:5173`。

前端源代码更新后需要重新执行 `npm.cmd run build`;如果通过静态服务器或 Nginx 托管,请将新的 `apps/web/dist` 发布到站点目录并 reload。浏览器仍显示旧版时使用 `Ctrl+F5` 清理旧构建缓存。

## 测试与验证命令

后端契约与单元测试(Windows;POSIX 用 `./mvnw`):

```powershell
Set-Location apps/api
.\mvnw.cmd -Dtest='*ApiTest' test
.\mvnw.cmd -Dtest=GlobalExceptionHandlerTest test
Set-Location ../..
```

真实 MySQL 迁移检查(需 Docker/Testcontainers,或同时提供以下三个变量且 schema 严格为 `interview_record_test`):

```powershell
$env:TEST_DB_URL='jdbc:mysql://localhost:3306/interview_record_test?serverTimezone=UTC'
$env:TEST_DB_USERNAME='interview_record_test'
$env:TEST_DB_PASSWORD='<dedicated-test-database-password>'
```

```powershell
Set-Location apps/api
.\mvnw.cmd -Dtest=MigrationTest test
Set-Location ../..
```

前端:

```powershell
Set-Location apps/web
npm.cmd run test:unit -- --run
npm.cmd run type-check
npm.cmd run build
Set-Location ../..
```

完整 Phase 1 门禁(需专用测试库、MySQL 客户端与 Playwright 浏览器;脚本只清理自己创建的子进程与捕获邮箱,不创建/删除/截断数据库):

```powershell
.\scripts\verify-phase-1.ps1
```

新建本地测试 schema 只能显式运行 `scripts/create-test-database.ps1`(支持 `-WhatIf`,仅创建 `interview_record_test`,不 drop/truncate/grant)。E2E 邮件捕获仅在 `e2e` profile 启用且强制 `E2E_MAILBOX_PATH`,常规与生产 profile 继续使用 SMTP。

## 验证证据

本工作区实际运行的结果:

- 后端 API 契约测试(`*ApiTest`,MockMvc):69 tests,0 failures,0 errors,BUILD SUCCESS。覆盖注册/验证/登录/重置/删号、公司与岗位隔离、状态与招聘类型、面试轮次与题库、日程紧急程度与提醒配置、分享白名单、统计、导出、偏好与错误契约。
- 前端 Vitest:27 个文件、86 个测试全部通过;`vue-tsc --build` 与 `vite build` 均成功。

环境限制(不视为已通过,由 CI 或具备条件的环境执行):

- 7 个 MySQL 依赖测试(`MigrationTest`、`TrackingMysqlIntegrationTest`、`RegistrationServiceTest`、`PasswordResetServiceTest`、`EmailVerificationServiceTest`、`RateLimitServiceTest`、`UserPreferenceJpaMappingTest`)需要 Docker/Testcontainers 或专用 `interview_record_test` 凭据;CI 使用 `mysql:8.4.9` 运行。
- Playwright 浏览器未安装,E2E 用例仅被发现、未执行。
- 沙箱曾阻断 Maven Wrapper 下载依赖,等价验证使用宿主 Maven 直调;网络可用环境请以 `mvnw` 命令为准。

## Repository layout

- `apps/api`:Spring Boot 模块化单体(auth、tracking、interviews、scheduling、reminders、sharing、insights、export、preference 等)与 JUnit 测试。
- `apps/web`:Vue SPA(app/views/features/shared 四层)、Vitest 单元测试与 Playwright 配置。
- `scripts`:本地启动、测试库创建与 Phase 1 门禁脚本。
- `.github/workflows`:CI 验证流水线。
- `docs`:PRD、路线图、实施计划与架构决策。

## 安全约束速览

- 密码使用强哈希;验证/重置/分享令牌仅存不可逆摘要。
- 富文本每次写入均经服务端白名单清洗,输出安全编码。
- 外部链接仅允许 http(s) 并防 opener 劫持;分享页 noindex。
- 日志不含密码、完整令牌、面试回答或复盘正文。
- `.env.example` 仅含变量名与示例值,真实凭据不入库。
