# Interview Record V1 completion report

## 本轮完成

- V5：兼容历史 `ASSESSMENT` 日程类型，并支持 PRD 的 `HR_COMMUNICATION`。
- V6：提醒计划持久化、幂等 claim、发送失败最多重试 3 次，日程修改/取消同步未发送提醒。
- V7：岗位分享链接，数据库只保存令牌摘要；匿名页严格按岗位/轮次白名单展示并加 noindex。
- 统计：按用户隔离的状态分布、招聘类型、投递趋势和三项转化率；分母为零返回不可用。
- 导出：认证 JSON 备份，明确排除密码、会话、认证/重置令牌和邮件内部字段。
- 前端：桌面侧栏、移动抽屉、四套主题、统计页、岗位分享弹窗、匿名分享页、设置页 JSON 导出。

## 已运行验证

- `apps/api/.\mvnw.cmd -Dtest=InsightsServiceTest,SharingServiceTest,ScheduleServiceTest,ReminderPlanTest,ReminderServiceTest test`：15/15 通过。
- `apps/api/.\mvnw.cmd -Dtest=ExportServiceTest test`：1/1 通过。
- `apps/api/.\mvnw.cmd -Dtest=InterviewRecordApplicationTest test`：1/1 通过。
- `apps/api/.\mvnw.cmd -DskipTests compile`：成功。
- `apps/web/npm.cmd run test:unit -- --run`：15 个文件、38 个测试通过。
- `apps/web/npm.cmd run type-check`：通过。
- `apps/web/npm.cmd run build`：通过；仅有 Vite 大 chunk 提示。
- `git diff --check`：通过。

## 明确未完成/未验证

- PRD 的 CSV/ZIP 多文件导出和短期下载令牌尚未实现；当前提供安全的 JSON 备份。
- 单条日程的自定义提醒覆盖 API 尚未实现；当前支持用户默认提醒偏移。
- 未运行真实 MySQL/Testcontainers 集成门禁：本机没有 Docker 或专用 `TEST_DB_*` 凭据。
- Playwright 浏览器未安装，完整浏览器 E2E 尚未执行。
