# Task 4: Sharing backend core

## Delivered

- Added Flyway V7 with `share_links` and `share_rounds`, user/position foreign keys, token hash uniqueness, owner-position index, and cascade invalidation when the source position is deleted.
- Added share entities/repositories and the application service.
- Tokens are 32-byte random URL-safe values; only their SHA-256 digest is stored.
- Added authenticated create/list/revoke APIs under a position and anonymous `GET /api/v1/shares/{token}`.
- Public output is constructed solely from persisted position and round allowlists. It excludes apply links, notes, schedules and account data by construction.
- Public invalid/expired/revoked/deleted-source cases all return the same not-found response. Anonymous reads are rate limited and include `X-Robots-Tag: noindex, nofollow` plus response `robots` metadata.

## Verification

- RED observed: `SharingServiceTest` initially failed test compilation because the sharing production package was intentionally absent.
- GREEN: `apps/api/.mvnw.cmd -Dtest=SharingServiceTest test` passed: 3 tests, 0 failures, 0 errors.
- Remaining integration/API tests are not yet added; the existing test covers token hash storage, foreign-round rejection, whitelist-only output, expiry, revocation, and source deletion invalidation.
