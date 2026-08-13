# Task 2 core-module audit and repair

## Scope completed

- Audited the tracking, interview, schedule and dashboard modules against the PRD and `AGENTS.md`.
- Added a forward-only Flyway V5 compatibility migration. It converts existing `ASSESSMENT` schedule rows to `WRITTEN_TEST`, replaces the V4 check constraint, and admits the PRD `HR_COMMUNICATION` type.
- Added optimistic-concurrency checking to schedule edits. A schedule update now requires the displayed version and rejects a stale or absent version with `CONCURRENT_UPDATE`.
- Made interview-round and linked-schedule time synchronization bidirectional and transactional. Editing a linked schedule updates its round and sibling linked schedules; editing a round continues to update its schedules.
- Prevented duplicate automatic schedules for a round by reusing and rescheduling the existing linked event.
- Deletes linked schedules before deleting an interview round, rather than relying solely on database cascade behavior.
- Changed round display ordering to PRD order: dated rounds by start time, then undated rounds by sequence number.
- Added API/service regression tests for cross-user round access, job-type/status modification rejection, schedule version conflicts, linked synchronization, duplicate linked schedules, PRD event types, and urgency boundaries.

## Verification evidence

Run from `apps/api`:

```powershell
.\mvnw.cmd '-Dtest=ScheduleServiceTest,ScheduleEventTypeTest,UrgencyTest,ScheduleApiTest,InterviewRoundApiTest,CompanyApiTest,PositionApiTest,DashboardApiTest' test
```

Result: 38 tests run, 0 failures, 0 errors.

The new schedule-specific tests were first run red: 3 failures demonstrated the absent round-to-schedule synchronization, duplicate linked schedule protection, and missing `HR_COMMUNICATION` support. They passed after the minimal implementation.

`mvnw.cmd -DskipTests test-compile` was also attempted. It could not resolve the Spring Boot parent because the local sandbox denied an outbound Maven Central socket. The focused Maven test command above subsequently ran from the local Maven cache and compiled main and test code successfully.

## Remaining risks / follow-up

- Migration V5 has not been run against a dedicated real MySQL test schema in this workspace because Docker and `TEST_DB_*` credentials are unavailable. The existing MySQL migration test now asserts V5 behavior and should be run in the release environment.
- Job type and status are isolated by repository/service scope; their cross-user regression coverage was added to the MySQL integration suite and still needs the real-MySQL gate.
- Reminder persistence (Task 3) must own the next migration version (V6) and update schedule completion/cancellation paths.
- Existing V3/V4 domain payloads are uncommitted baseline work from the earlier implementation. This task commits the core corrections on top of that baseline without resetting or deleting user changes.
