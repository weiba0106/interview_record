# Task 5 Insights Core Report

## Scope

Implemented the minimum server-side statistics core required by the existing
`InsightsServiceTest`. Export functionality is intentionally outside this focused change.

## Behaviour

- All source reads are constrained by the authenticated caller's `userId`.
- The optional recruitment-type and applied-date filters apply to every metric.
- Results include ordered status distribution, recruitment-type breakdown, application trend,
  interview reach rate, offer conversion rate, and interview-round pass rate.
- A conversion with a zero denominator returns `available=false` and `percentage=null`, so the
  client can render PRD-required "暂无数据" rather than `0%`.

## Validation

Executed from `apps/api` on 2026-08-13:

```powershell
.\mvnw.cmd -Dtest=InsightsServiceTest test
.\mvnw.cmd -DskipTests test-compile
```

Both commands completed successfully. The focused test suite ran 3 tests with 0 failures and
0 errors. Maven emitted existing deprecation/unchecked and dynamic Mockito agent warnings;
they do not fail the build.
