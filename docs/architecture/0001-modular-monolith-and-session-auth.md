# ADR 0001: Modular monolith and JDBC-backed sessions

**Status:** Accepted — Phase 1

## Context and decision

V1 is a single product with a small, evolving domain and a strict user-isolation requirement. We use a Spring Boot modular monolith rather than microservices: account, preference, and future business capabilities stay independently packaged, while one deployment and one transactional boundary avoid network coordination, duplicated authorization, and operational overhead.

Authentication uses Spring Security sessions persisted by Spring Session JDBC. Session cookies are `HttpOnly` and hold no bearer credential in browser storage. This avoids local-storage token exposure and makes logout, password reset, and account deletion able to revoke server-side sessions immediately.

MySQL is the only V1 shared state store. It holds product data, sessions, one-time-token digests, and rate-limit buckets, so security and consistency rules are observable and recoverable in one system. Packages follow `feature.domain` → `feature.application` → `feature.infrastructure` / `feature.api`; APIs and jobs call application services rather than reaching into another feature's tables.

## Consequences

The monolith has a deliberately clear extraction boundary without paying a distributed-system cost now. Redis is not part of V1. Reconsider it only if measured reminder-claim or rate-limit contention shows that MySQL cannot meet the PRD latency targets; any change must retain durable correctness and user scoping.
