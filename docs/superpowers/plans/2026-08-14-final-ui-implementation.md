# Final Interview Record UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

> **状态：已完成（2026-08-15）。** 全部四个任务均已落地，并在此基础上扩展为全站视觉重构：圆角应用容器与深色侧边栏、四主题设计令牌（含真实颜色预览的主题卡片）、概览双栏布局、紧凑筛选工具条、日程四维紧急程度表达、统一空状态与 375px 移动端适配。验证命令与真实结果见本文档「Verification」一节。

**Goal:** 将三个已确认的 HTML 设计稿转换为可运行的 Vue 界面：C 方案混合工作台、四主题快捷切换，以及带颜色/图标/文字/倒计时的日程紧急程度展示。

**Architecture:** 复用现有 Vue Router、Pinia、Element Plus 和 CSS design tokens。`AppShell` 负责全局导航与主题快捷菜单，Dashboard 负责指标、岗位表格和即将到来的日程，岗位页保留表格并增加看板切换；主题偏好仍通过现有 `/api/v1/me/preferences` 持久化，服务端继续定义紧急程度和权限边界。

**Tech Stack:** Vue 3.5、TypeScript 6、Vue Router、Pinia、Element Plus、Vitest、CSS custom properties。

## Global Constraints

- 默认主题为 `GRAPHITE_CORAL`，支持 `INDIGO`、`FOREST_TEAL`、`WARM_APRICOT`、`GRAPHITE_CORAL`。
- 日程紧急程度必须同时使用颜色、图标、文字和剩余时间表达，不能只依赖颜色。
- 桌面端使用侧栏和高密度工作区；375px 宽度下导航、岗位、日程和主题切换仍可完成。
- 前端只负责交互和展示，业务权限、紧急程度口径和主题保存由后端 API/领域规则保证。
- 不新增团队协作、第三方登录、外部日历、附件或 AI 功能。

### Task 1: 应用壳与主题快捷切换

**Files:**
- Modify: `apps/web/src/app/components/AppShell.vue`
- Modify: `apps/web/src/app/theme.ts`
- Modify: `apps/web/src/app/theme.css`
- Test: `apps/web/src/app/AppShell.spec.ts`

- [x] **Step 1: Write the failing tests**

  Cover a signed-in shell rendering the six navigation entries, opening a theme picker from the top bar, rendering all four themes, and applying the selected theme token before emitting the save event.

- [x] **Step 2: Run the focused test and confirm RED**

  Run `npm.cmd run test:unit -- --run src/app/AppShell.spec.ts` from `apps/web`. Expected failure: the shell has no theme picker and no theme option buttons.

- [x] **Step 3: Implement the minimal shell interaction**

  Add a top-bar theme button with `aria-expanded`, a popover/listbox containing `themeOptions`, and a selection handler that calls `applyTheme`, updates the Pinia user theme optimistically, then saves through `updatePreferences` while preserving the existing settings form as the full preference editor. Close the picker on outside click and Escape.

- [x] **Step 4: Run focused tests and type-check**

  Run `npm.cmd run test:unit -- --run src/app/AppShell.spec.ts` and `npm.cmd run type-check`; both must pass.

### Task 2: C 方案混合工作台与紧急日程视觉

**Files:**
- Modify: `apps/web/src/views/DashboardView.vue`
- Modify: `apps/web/src/features/scheduling/urgency.ts`
- Modify: `apps/web/src/app/theme.css`
- Test: `apps/web/src/views/DashboardView.spec.ts`
- Test: `apps/web/src/features/scheduling/urgency.spec.ts`

- [x] **Step 1: Write the failing tests**

  Assert that the dashboard renders four metric cards, a jobs panel, an upcoming agenda panel, and each urgency item includes a semantic icon, label, and countdown. Assert that `urgencyDisplay` exposes a stable icon for `URGENT`, `APPROACHING`, `NORMAL`, and `HANDLED`.

- [x] **Step 2: Run focused tests and confirm RED**

  Run `npm.cmd run test:unit -- --run src/views/DashboardView.spec.ts src/features/scheduling/urgency.spec.ts`; expected failure is the missing semantic icon and the incomplete C layout contract.

- [x] **Step 3: Implement the minimal dashboard composition**

  Use the final design’s two-column body: the jobs table on the left and the “即将到来” agenda on the right. Keep the existing API calls and status actions. Add a compact page header, panel boundaries, search/filter affordance placeholders that do not claim unsupported filtering, and an urgency icon/label/countdown row with text alternatives.

- [x] **Step 4: Run focused tests and type-check**

  Re-run the two focused Vitest files and `npm.cmd run type-check` until green.

### Task 3: 岗位表格/看板切换与 responsive behavior

**Files:**
- Modify: `apps/web/src/views/PositionsView.vue`
- Modify: `apps/web/src/app/theme.css`
- Test: `apps/web/src/views/PositionsView.spec.ts`

- [x] **Step 1: Write the failing tests**

  Assert that the positions page exposes “表格/看板” controls, defaults to the table, and switches to status columns without changing the API query or losing the position links. Assert that the mobile layout keeps controls reachable at 375px.

- [x] **Step 2: Run the focused test and confirm RED**

  Run `npm.cmd run test:unit -- --run src/views/PositionsView.spec.ts`; expected failure is the missing view toggle and board columns.

- [x] **Step 3: Implement the toggle and board**

  Add a local `viewMode` ref, accessible tab-like buttons, and a board grouped by the server-provided position status order. Keep the existing table as the default and preserve each position’s detail route. Add horizontal overflow only to the board region on narrow screens.

- [x] **Step 4: Run focused tests and build**

  Run `npm.cmd run test:unit -- --run src/views/PositionsView.spec.ts`, `npm.cmd run type-check`, and `npm.cmd run build`.

### Task 4: Final visual regression and handoff

**Files:**
- Modify: `apps/web/src/app/theme.spec.ts`
- Modify: `apps/web/src/views/DashboardView.spec.ts`
- Modify: `apps/web/src/views/SettingsView.spec.ts`
- Modify: `README.md`

- [x] **Step 1: Add regression coverage**

  Verify theme selection persists through the existing preference API mock, settings still exposes all four themes, the default is Graphite Coral, and urgency meaning is unchanged across themes.

- [x] **Step 2: Run the complete frontend gate**

  Run `npm.cmd run test:unit -- --run`, `npm.cmd run type-check`, `npm.cmd run build`, and `git diff --check` from `apps/web`/repository root as appropriate.

- [x] **Step 3: Update startup notes**

  Document that the design is served by the rebuilt Vite `dist`, and that a production/static deployment must rebuild after source changes.

- [x] **Step 4: Review the rendered routes**

  Manually verify `/app`, `/app/positions`, `/app/settings` at desktop width and 375px width; confirm the top-bar theme picker, side navigation, dashboard agenda, and board toggle are reachable by keyboard.

## Verification

在 `apps/web` 目录实际运行的命令与结果：

| 命令 | 结果 |
| --- | --- |
| `npm.cmd run test:unit -- --run` | 16 个测试文件、43 个用例全部通过（Vitest 直连退出码 0） |
| `npm.cmd run type-check` | 通过（退出码 0） |
| `npm.cmd run build` | 通过（退出码 0；仅有既存的 >500kB chunk 提示） |
| `npx.cmd oxlint .`（只读检查） | 4 个既存错误，均位于本次未修改的 `src/shared/api/http.ts` 与 `src/app/AppShell.spec.ts` |

E2E（`npm.cmd run test:e2e`）与 Playwright 浏览器级响应式验收未执行：工作区未安装 Playwright 浏览器，且完整 E2E 需要本地 API/Vite 服务与专用测试库凭据（见根目录 AGENTS.md）。
