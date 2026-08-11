# Task 9 report — account lifecycle E2E and phase documentation

## Delivered

- Added the RED-first Playwright account lifecycle test and JSONL mailbox polling helper. The journey covers register, email verification, login, preference update, logout, password reset, old-password rejection, new-password login, a second browser context with separate preferences, and account deletion.
- Added an `e2e`-only `CapturingMailGateway`; SMTP is now excluded only for that profile. Captured messages use synchronized UTF-8 JSONL writes and are never logged with their URL or token.
- Added the required `application-e2e.yml`, Vite API proxy, safe test-schema creation helper, and complete Phase 1 verification orchestrator. The latter validates an existing exact `interview_record_test` connection, maps database variables only into its API child process, cleans up only child processes it starts and its own mailbox, and does not require Docker or Mailpit.
- Added CI account-lifecycle coverage, the modular-monolith/session-auth ADR, and command/safety documentation in `README.md` and `AGENTS.md`.

## Verification actually run

- `npm.cmd run test:e2e -- account-lifecycle.spec.ts` — RED confirmed, but cannot launch because Chromium, Firefox, and WebKit Playwright browser executables are not installed in this workspace. The test runner discovered all three configured projects before failing at browser launch.
- `npm.cmd run type-check` — passed.
- `npm.cmd run test:unit -- --run` — passed: 9 files, 16 tests.
- `npm.cmd run build` — passed. Vite emitted its existing large-chunk warning only.
- `./mvnw.cmd -DskipTests compile` — passed after aligning the capture gateway with the project’s Spring Boot 4 `tools.jackson.databind.ObjectMapper` namespace.
- PowerShell parser checks for both scripts — passed.
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\\scripts\\verify-phase-1.ps1` with no `TEST_DB_*` values — failed as designed before starting services: it requires all three values for the existing dedicated `interview_record_test` schema.
- `git diff --check` — passed.

## Remaining verification limits

- This workspace has no Playwright browser binaries and no usable `interview_record_test` credentials/Docker, so the real MySQL-backed lifecycle and the complete `verify-phase-1.ps1` gate could not run here. No H2 or alternate schema was used.
- A direct `.ps1` invocation is blocked by the local PowerShell execution policy; parser validation and the explicit `-ExecutionPolicy Bypass` guard-path run were used only to inspect behavior in this constrained workspace.

## Review fix round 1

- Replaced the invalid native `selectOption` call with Element Plus combobox/open-option interaction and an assertion that the selected theme is visible.
- Wrapped both browser users in `try`/`finally`: any registered account not already deleted is deleted through the same password-confirmed UI flow, and the secondary browser context always closes. The normal journey deletes both accounts before final cleanup.
- The MySQL connection probe now defaults an omitted JDBC port to `3306`; `AGENTS.md` now explicitly labels the full Phase 1 gate as unverified in this workspace because the browser binaries and dedicated credentials are unavailable.
- Re-ran `npm.cmd run type-check`, `npm.cmd run test:unit -- --run` (9 files, 16 tests), the PowerShell parser check, and `git diff --check`; all passed. Full browser execution remains unavailable for the previously recorded missing-browser reason.

## Review fix round 2

- Split cleanup state into registered, logged-in, and deleted states. Cleanup now invokes the authenticated deletion flow only for a currently authenticated browser session.
- Secondary-account deletion, secondary context closing, and primary-account deletion now run in independent cleanup branches; a failure in one cannot skip the remaining cleanup work. A cleanup error fails an otherwise successful journey and is attached as an annotation when the journey had already failed.
- The README documents the intentionally unavoidable limit: an account that fails before verification/login cannot be removed through the production, authenticated deletion API, because no unauthenticated E2E cleanup endpoint exists.
- Re-ran `npm.cmd run type-check`, `npm.cmd run test:unit -- --run` (9 files, 16 tests), the PowerShell parser check, and `git diff --check`; all passed.

## Final regression fix

- Added the missing MVC-slice mocks for every `AuthController` dependency in the registration and email-verification tests. The combined login/current-user slice also now mocks `MeController`'s preference and account-deletion dependencies; the dedicated account-deletion slice already contained those mocks.
- `./mvnw.cmd verify` ran to completion of the non-container MVC checks: `EmailVerificationApiTest` (5/5), `LoginSessionApiTest` (8/8), and `AccountDeletionApiTest` (2/2) now start and pass. The earlier missing-`AuthService` context failure is resolved.
- Full verify remains non-green for two separately retained failures: `RegistrationApiTest.materializesPublicCsrfTokenWithoutCreatingAuthenticatedSession` expects an `XSRF-TOKEN` response cookie but the current MVC test uses Spring's HTTP-session token repository, and 21 MySQL integration tests cannot start because Docker and dedicated `TEST_DB_*` credentials are unavailable. No assertions were weakened and no alternate database was used.

## CSRF regression fix

- `AuthController.csrf` now explicitly loads or generates and saves a token through the injected `CsrfTokenRepository`. This makes the configured Cookie repository write `XSRF-TOKEN` while preserving the `204` response and avoiding an authenticated session.
- `./mvnw.cmd -Dtest=RegistrationApiTest,EmailVerificationApiTest,LoginSessionApiTest,AccountDeletionApiTest test` passed: 21 tests, 0 failures, 0 errors. The full verify's remaining MySQL integration limitation is unchanged: Docker and dedicated `TEST_DB_*` credentials are unavailable in this workspace.

## Review fix round 3

- Added explicit verified state for both accounts. A verified account that is logged out when the journey fails is now re-authenticated with its current password and deleted before cleanup exits.
- Only accounts that fail before email verification are marked for manual dedicated-test-database cleanup; authenticated cleanup failures are separately reported and never suppress another cleanup branch.
- Updated README wording to distinguish the pre-verification limitation from authenticated cleanup failures.
- Re-ran `npm.cmd run type-check`, `npm.cmd run test:unit -- --run` (9 files, 16 tests), the PowerShell parser check, and `git diff --check`; all passed.

## Runtime hardening follow-up

- Configured the API CSRF filter with `CookieCsrfTokenRepository` and Spring Security's plain `CsrfTokenRequestAttributeHandler`, which accepts the raw `XSRF-TOKEN`/`X-XSRF-TOKEN` pair sent by Axios. `SpaCsrfFilterTest` covers that browser contract, while the public CSRF endpoint remains a `204` that materializes the cookie without creating an authenticated session.
- Made `AuthenticatedUser` serializable and added a SecurityContext Java-serialization regression test for Spring Session JDBC.
- `GET /api/v1/me` now reads current persisted preferences instead of stale fields in the session principal; the preference MVC slice covers the changed response.
- Registration consumes the verification resend cooldown for its initial delivery. Resend and forgot-password paths now perform token generation/hash-query work for unknown addresses before returning their existing non-enumerating response.
- The phase script's external-DB integration tests now clean the exact guarded test schema before and after every integration test, including fixed users and rate-limit buckets. The cleanup SQL is reachable only through `MySqlIntegrationTestBase`, whose external URL guard requires `interview_record_test`.
- CI exports `E2E_MAILBOX_PATH` to both child services and Playwright. The web app imports Element Plus CSS plus Graphite/Coral foundation tokens, sets `no-referrer`, and clears verification/reset query tokens from the browser URL before the API call. Account deletion now catches post-commit revocation failures so controller-side session/cookie cleanup still runs.

### Follow-up verification

- `./mvnw.cmd -Dtest=RegistrationApiTest,SpaCsrfFilterTest,AuthenticatedUserSerializationTest test` passed: 8 tests, 0 failures, 0 errors.
- `git diff --check` passed after the final change set.
- A broader focused MVC command was started after adding fresh `/me` behavior and exposed two legacy `LoginSessionApiTest` mock responses missing for the new persisted-preferences lookup. Those two mocks have been added, but the parent requested an immediate commit before rerunning the longer command. This final mock adjustment and MySQL integration cleanup were therefore not re-executed in this workspace.
- Frontend type/unit/build checks were not rerun after the final CSS/referrer changes. Earlier Task 9 frontend checks remain recorded above; browser E2E and full MySQL verification remain blocked by missing Playwright binaries and dedicated database credentials/Docker.

## Enumeration and deletion follow-up

- Verification and reset email delivery now go through a bounded background executor outside the request thread. The `test` profile deliberately uses a synchronous executor so service assertions remain deterministic; normal and E2E profiles use the bounded asynchronous executor. Unknown or ineligible paths perform the same limiter, token-generation, hash-query, and return path without invoking SMTP, so SMTP latency is not an account-existence signal.
- Registration still seeds the 60-second verification cooldown. A resend during that cooldown is silently accepted and does not deliver a second mail; an unknown first resend receives the same public accepted outcome. The hourly email/IP limit remains the public 429 condition. The verification-service test now covers the first-known/unknown accepted result and corrected cap count.
- Session revocation now happens before the irreversible user cascade. If the JDBC session store is unavailable, deletion is aborted before the user is deleted, preventing remaining sessions from belonging to a deleted account. `AccountDeletionServiceTest` covers both successful order and revocation-failure rollback safety.
- `./mvnw.cmd -Dtest=AccountDeletionServiceTest test` passed: 3 tests, 0 failures, 0 errors. MySQL integration service tests for the revised resend/reset behavior remain unrun because this workspace lacks Docker and dedicated `TEST_DB_*` credentials; no alternate database was used.

### Consolidated hardening commit

- Commit `909fc82 fix: harden phase one account flows` records the staged runtime-hardening follow-up.
- `git diff --cached --check` passed immediately before the commit. The MySQL integration base now applies the same narrow cleanup script before and after each test; it is reached only after the existing exact-`interview_record_test` external-database guard or through Testcontainers.
- A final frontend type/unit rerun was started but intentionally stopped to honor the requested immediate commit; it remains unverified after the CSS/referrer additions.
