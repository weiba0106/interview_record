# Interview Record

用于记录秋招与日常实习求职过程的多用户响应式 Web 应用。当前仓库包含 Phase 1 的可运行工程基础：Spring Boot API、Vue SPA、MySQL Compose 服务与 CI。

## Runtime

- Java 21 and Maven Wrapper 3.9.16
- Node 24 LTS and npm 11
- MySQL 8.4.9 for local infrastructure
- Vue 3.5.x, TypeScript 6.0.x, and Vite 8.1.x

## Verified commands

From the repository root, run the backend check on Windows:

```powershell
Set-Location apps/api
.\mvnw.cmd verify
Set-Location ../..
```

On POSIX shells:

```sh
(cd apps/api && ./mvnw verify)
```

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
