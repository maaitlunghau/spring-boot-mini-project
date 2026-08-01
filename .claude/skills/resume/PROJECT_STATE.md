# Project State — spring-boot-mini-project

**Last synced commit:** e58885b

## What this is

A Spring Boot 4.1.0 / Java 21 backend, single standalone project (not a sub-project mono-repo) — hands-on practice for production-grade patterns: proper secrets handling, Docker/docker-compose, CI, and a real reviewed auth module rather than a hello-world. Full stack/setup: `README.md`.

## Current focus

Auth + User module (JWT login/refresh/logout, refresh-token rotation with theft/reuse detection, role-based user management, rate limiting) — user-authored, then code-reviewed and partially hardened via TDD.

Infra (Docker, docker-compose, CI, secrets via `.env` + direnv) is done and stable — see `docs/superpowers/specs/` and `docs/superpowers/plans/` for how it was built, `README.md` for how to run it day to day.

## Known issues / TODO

Full list with root-cause detail: `docs/known-issues.md`. Summary:
- **Fixed (with regression tests):** readonly-transaction write bug, reuse-detection rollback bug, expired/revoked check-order bug in token rotation, X-Forwarded-For spoofing bypassing rate limiting.
- **Open, unfixed:** #5 last-admin race condition (investigated, no working fix found yet — memory `project_last-admin-race-deferred` has the full technical trail, read it before retrying), #6 duplicate-email race falling through to a raw 500, #7 cookies hardcode `secure(false)`, #8 `UsernameNotFoundException` uncaught in the JWT filter, #9 NPE on a null refresh token, #10 `GlobalExceptionHandler` never logs anything + a logout token-resolution precedence mismatch vs the filter.

## Decisions to respect (don't silently change)

- Swagger UI (`/swagger-ui/index.html`) is deliberately public — explicit user choice, not an oversight.
- CD pipeline is deliberately a no-op placeholder — no deployment target chosen yet.
- Commits are small and conventional (`type(scope): subject`, enforced by a Husky `commit-msg` hook: lowercase, single line, ≤60 chars, no body) — see `git log --oneline` for the established pattern.

## Next step

Ask the user which of the open bugs (#5–#10) to tackle next; follow the same TDD pattern already used for the first four (real MySQL/Redis via docker-compose, failing test first, minimal fix, one commit per fix with its test).
