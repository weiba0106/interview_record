# Interview Record Platform Delivery Roadmap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the complete V1 interview-record platform as five independently testable phases, beginning with a production-shaped foundation and email/password account system.

**Architecture:** Use a monorepo with a Spring Boot modular monolith in `apps/api` and a Vue single-page application in `apps/web`. Persist business data, sessions, rate-limit counters, and reminder state in MySQL; keep Redis out of V1 until measured load proves it is required. Organize both applications by business capability so later phases can add domains without bypassing user isolation.

**Tech Stack:** Java 21, Spring Boot 4.1.0, Maven Wrapper 3.9.16, Spring MVC/Security/Session JDBC/Data JPA/Flyway/Mail, MySQL 8.4 LTS, Node.js 24 LTS, npm 11, Vue 3.5.x, TypeScript 6.0.x, Vite 8.1.x, Vue Router, Pinia, Element Plus, Vitest, Playwright.

## Global Constraints

- The product baseline is `docs/superpowers/specs/2026-08-10-interview-tracker-prd-design.md`; do not add V1 features excluded there.
- The repository-wide rules in `AGENTS.md` apply to every phase.
- Java source and target compatibility remain exactly Java 21 throughout V1.
- Use Spring Boot 4.1.0 and Maven Wrapper 3.9.16; do not use Spring milestone, release-candidate, or snapshot dependencies.
- Use Node.js 24 LTS, Vue stable 3.5.x, Vite stable 8.1.x, and TypeScript stable 6.0.x; commit `package-lock.json`.
- Production and CI target MySQL 8.4 LTS with `utf8mb4`; local MySQL 8.0.42 may be used only through the explicit compatibility test profile.
- Do not introduce Redis, Kafka, RabbitMQ, Elasticsearch, Kubernetes, or microservices in V1.
- Authentication uses a same-origin server session stored with Spring Session JDBC; do not replace it with browser-stored JWTs.
- Every protected query includes the authenticated `user_id`, including lists, aggregates, exports, background jobs, and cache keys.
- Store all timestamps in UTC and calculate display/reminder time using the user's IANA time zone.
- Every behavioral change follows red-green-refactor TDD and ends with executable verification evidence.
- Support the latest two stable Chrome, Edge, Firefox, and Safari versions and viewport widths of 375 px and above.
- Theme names and urgency semantics must match the PRD exactly.

---

## 1. Verified Technology Baseline

The plan pins stable, mutually compatible lines available on 2026-08-11:

- Spring Boot 4.1.0 requires Java 17 or newer and supports Java through 26, so Java 21 is supported: <https://docs.spring.io/spring-boot/system-requirements.html>.
- Spring Boot 4.1.0 supports Maven 3.6.3 or newer; the repository wrapper pins current Maven 3.9.16: <https://maven.apache.org/download.cgi>.
- Vue's official scaffold uses Vite and supports Node 24; Node 24 is an LTS line: <https://vuejs.org/guide/quick-start.html> and <https://nodejs.org/en/about/previous-releases>.
- Vite 8.1 is the actively patched stable line: <https://vite.dev/releases>.
- Vue 3.5 is the stable line while 3.6 remains pre-release: <https://github.com/vuejs/core/releases>.
- MySQL 8.4 is the long-term-support line: <https://dev.mysql.com/doc/refman/8.4/en/mysql-releases.html>.

The current workstation already has Java 21.0.9, Node 24.12.0, npm 11.6.2, Maven 3.6.3, and local MySQL 8.0.42. Docker is not installed, so all detailed plans must provide both:

1. a reproducible `compose.yaml` path using MySQL 8.4.9 and Mailpit 1.30.0; and
2. a Docker-free test path using the dedicated `interview_record_test` schema and an `e2e`-only captured-mail adapter.

Never point automated tests at an unspecified database or a schema that may contain user data.

## 2. Repository Map

The first phase creates this layout and later phases extend it without changing the top-level boundaries:

```text
interview_record/
├── AGENTS.md
├── README.md
├── .editorconfig
├── .gitattributes
├── .env.example
├── compose.yaml
├── apps/
│   ├── api/
│   │   ├── pom.xml
│   │   ├── mvnw
│   │   ├── mvnw.cmd
│   │   ├── .mvn/wrapper/
│   │   └── src/
│   │       ├── main/java/com/interviewrecord/
│   │       ├── main/resources/
│   │       └── test/java/com/interviewrecord/
│   └── web/
│       ├── package.json
│       ├── package-lock.json
│       ├── vite.config.ts
│       ├── playwright.config.ts
│       └── src/
│           ├── app/
│           ├── features/
│           ├── shared/
│           └── views/
├── infra/
│   └── mysql/
├── scripts/
├── docs/
│   ├── architecture/
│   └── superpowers/
└── .github/workflows/ci.yml
```

### Backend dependency direction

Each business package repeats the concrete pattern shown here for `auth`:

```text
auth/api              HTTP DTOs and controllers
auth/application      use cases and transaction boundaries
auth/domain           business types and rules
auth/infrastructure   JPA and session adapters
```

Controllers may call application services. Application services may call domain types and declared ports. Infrastructure implements ports. Domain code must not import Spring MVC, JPA repositories, SMTP, or servlet types.

### Frontend dependency direction

```text
app      router, providers, global layout
views    route-level composition
features business UI, stores, API contracts
shared   transport, design tokens, generic UI and utilities
```

`shared` must not import `features` or `views`. A feature must not reach into another feature's private files; cross-feature data flows through typed public exports.

## 3. Stable Cross-Phase Interfaces

All phases use the following conventions:

- REST base path: `/api/v1`.
- Session cookie: `INTERVIEW_RECORD_SESSION`, `HttpOnly`, `Secure` outside local development, `SameSite=Lax`.
- CSRF cookie/header pair: `XSRF-TOKEN` and `X-XSRF-TOKEN`.
- Successful mutation responses use HTTP `201`, `200`, or `204` according to whether a resource is created, returned, or omitted.
- Error shape:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "请求参数有误",
  "fieldErrors": { "email": "邮箱格式不正确" },
  "traceId": "01J..."
}
```

- Public entity identifiers are opaque strings in API DTOs even if the first migration uses numeric database keys internally.
- List responses use:

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalItems": 0,
  "totalPages": 0
}
```

- Frontend dates cross the API as ISO 8601 strings with offsets; backend persistence normalizes them to UTC.
- OpenAPI generation is deferred until the API contracts stabilize in Phase 2; TypeScript API types remain handwritten and covered by contract tests during Phase 1.

## 4. Phase Sequence

### Phase 1: Foundation and account lifecycle

Detailed plan: `docs/superpowers/plans/2026-08-11-foundation-auth-implementation.md`

Deliverable:

- runnable Spring Boot API and Vue SPA;
- Flyway-managed MySQL schema and JDBC-backed sessions;
- registration, email verification, resend verification, login, logout, current-user, forgot/reset password, preferences, and account deletion;
- initial user defaults for statuses, job types, reminders, time zone, and theme;
- local/CI test database paths and a verified CI workflow.

Gate: a new user can register, verify through the test-only captured message, log in, update preferences, log out, reset a password, and delete the account; another user cannot access the first user's profile or defaults.

### Phase 2: Company, job type, status, and position tracking

Planned document: `docs/superpowers/plans/2026-08-11-application-tracking-implementation.md`

Deliverable:

- company CRUD with duplicate warning and guarded deletion;
- job type and ordered status management with statistical categories;
- position CRUD, archive, search, filter, sort, pagination, and status updates;
- desktop table workspace and mobile card/table adaptation.

Gate: two users can create identical company names but cannot observe each other's data; a user can track multiple positions per company and cannot delete a referenced status without migration.

### Phase 3: Interview records, schedules, reminders, and email worker

Planned document: `docs/superpowers/plans/2026-08-11-interviews-scheduling-implementation.md`

Deliverable:

- ordered interview rounds, question/answer items, sanitized rich text, and result tracking;
- schedule CRUD, interview/schedule synchronization, urgency calculation, manual override, and agenda/calendar UI;
- idempotent database-backed reminder claiming, retry, cancellation, and SMTP delivery.

Gate: one position supports at least three ordered interview rounds; reminder retries never duplicate a successful send; 24-hour and 72-hour urgency boundaries pass controlled-clock tests.

### Phase 4: Selective sharing, statistics, and export

Planned document: `docs/superpowers/plans/2026-08-11-sharing-insights-export-implementation.md`

Deliverable:

- hashed share tokens, field/round allowlists, expiration/revocation, and noindex public page;
- status, application, interview, and offer statistics using the exact PRD formulas;
- UTF-8 BOM CSV ZIP export and full JSON backup with expiring download links.

Gate: anonymous visitors can see only selected fields; revoked or expired links reveal nothing; exports contain user-owned business data but no password/session/token secrets.

### Phase 5: Four themes, responsive polish, observability, and release hardening

Planned document: `docs/superpowers/plans/2026-08-11-ui-release-hardening-implementation.md`

Deliverable:

- complete Indigo, Forest, Apricot, and Graphite/Coral token sets;
- persisted theme selection, WCAG 2.1 AA checks, keyboard paths, and 375 px workflows;
- structured logging, trace IDs, health checks, performance indexes, security headers, production configuration, and full regression suite.

Gate: all PRD acceptance criteria pass in CI, including isolation, responsive flows, accessibility, export, reminder, and four-theme coverage.

## 5. Plan Authoring Rule for Phases 2–5

Write each later detailed plan only after the previous phase is integrated and its stable interfaces are visible. Before authoring it:

- read the PRD sections covered by that phase;
- inspect the actual file tree and API/error conventions created earlier;
- keep the phase independently deployable and testable;
- list exact files and public interfaces;
- include failing tests, expected failure messages, minimal implementations, passing commands, and focused commits;
- repeat code rather than saying “same as the previous task”;
- scan for unfinished markers, vague error handling, missing types, and cross-phase name drift.

## 6. Cross-Phase Release Checks

- [ ] Phase 1 account and tenant-isolation suite passes.
- [ ] Phase 2 company/position/status suite passes without changing Phase 1 contracts.
- [ ] Phase 3 controlled-clock, sanitizer, schedule-sync, and reminder-idempotency suite passes.
- [ ] Phase 4 anonymous-share allowlist, formula, and export-secret suite passes.
- [ ] Phase 5 browser, accessibility, performance, and security regression suite passes.
- [ ] `AGENTS.md`, `README.md`, `.env.example`, migrations, and standard commands match the implemented repository.
- [ ] A clean clone can start with either Docker Compose or an explicitly named external MySQL test schema.
- [ ] No excluded V1 feature or unapproved infrastructure dependency has entered the repository.

## 7. Main Risks and Chosen Mitigations

| Risk | Decision |
| --- | --- |
| Cross-user data exposure | Require user-scoped repositories and an isolation test for every protected resource |
| Session revocation and multi-instance readiness | Persist sessions with Spring Session JDBC rather than process memory |
| Duplicate reminder email | Claim rows transactionally and enforce an idempotency key/unique constraint |
| Rich-text XSS | Sanitize on every server write and regression-test dangerous HTML |
| Theme drift | Centralize all visual values in semantic CSS tokens |
| Docker unavailable on the current machine | Support a named external MySQL test database while CI uses MySQL 8.4 |
| Overlarge first implementation | Execute five phase plans with explicit gates instead of one monolithic plan |
| Premature distributed infrastructure | Keep MySQL as the only stateful dependency until measured evidence justifies Redis or a broker |
