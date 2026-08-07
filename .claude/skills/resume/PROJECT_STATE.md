# Project State — spring-boot-mini-project

**Last synced commit:** e33d6c6

## What this is

A Spring Boot 4.1.0 / Java 21 backend, single standalone project (not a sub-project mono-repo) — hands-on practice for production-grade patterns: proper secrets handling, Docker/docker-compose, CI, and a real reviewed auth module rather than a hello-world. Full stack/setup: `README.md`.

## Current focus

Auth + User module (JWT login/refresh/logout, refresh-token rotation with theft/reuse detection, role-based user management, rate limiting) — user-authored, then code-reviewed and partially hardened via TDD.

Infra (Docker, docker-compose, CI, secrets via `.env` + direnv) is done and stable — see `docs/superpowers/specs/` and `docs/superpowers/plans/` for how it was built, `README.md` for how to run it day to day.

`README.md`'s "Modules & Features" section is now a fully written, professional roadmap: Auth + User documented in depth (design rationale, not just endpoint lists), and Blog / Like & Comment / Notification / forgot-reset-password / OAuth2 social login listed as explicitly planned, one module at a time, each getting its own brainstorm → spec → plan cycle when picked up — don't design more than one ahead.

`.claude/CLAUDE.md` and `.claude/rules/*.md` were rewritten from a copy-pasted multi-sub-project template into content that actually matches this repo (real module layout, real stack, real gotchas like the `@Transactional`/`REQUIRES_NEW` bug). Read those before making architecture/convention decisions — don't re-derive them from scratch.

A GitHub ruleset on `main` now requires the `build-and-test` CI status check to pass before merging (configured and verified by the user directly on GitHub — both the "CI green → merge allowed" and "CI red → merge blocked, fix, merge" flows were tested end to end on the `test/ruleset-check` branch, PRs #2 and #3).

## Known issues / TODO

Full list with root-cause detail: `docs/known-issues.md`. Summary:
- **Fixed (with regression tests):** readonly-transaction write bug, reuse-detection rollback bug, expired/revoked check-order bug in token rotation, X-Forwarded-For spoofing bypassing rate limiting.
- **Open, unfixed:** #5 last-admin race condition (investigated, no working fix found yet — memory `project_last-admin-race-deferred` has the full technical trail, read it before retrying), #6 duplicate-email race falling through to a raw 500, #7 cookies hardcode `secure(false)`, #8 `UsernameNotFoundException` uncaught in the JWT filter, #9 NPE on a null refresh token, #10 `GlobalExceptionHandler` never logs anything + a logout token-resolution precedence mismatch vs the filter.

## Decisions to respect (don't silently change)

- Swagger UI (`/swagger-ui/index.html`) is deliberately public — explicit user choice, not an oversight.
- CD pipeline is deliberately a no-op placeholder — no deployment target chosen yet.
- Commits are small and conventional (`type(scope): subject`, enforced by a Husky `commit-msg` hook: lowercase, single line, ≤60 chars, no body) — see `git log --oneline` for the established pattern.

## Next step

Two open threads, ask the user which to pick up:
1. One of the open bugs (#5–#10) — follow the same TDD pattern already used for the first four (real MySQL/Redis via docker-compose, failing test first, minimal fix, one commit per fix with its test).
2. The first planned module (Blog is next in the README's stated order) — start with `superpowers:brainstorming`, don't jump to code.

Minor, low-priority loose end: `docs/notes/problems.txt` has the user's own note that `ci.yml`'s `JWT_SECRET` (a CI-only throwaway value) should move to a GitHub Actions Secret on principle, even though the value itself is harmless. Not acted on yet.

## Known meta-issue (not a repo problem)

This session's terminal tab could not load project-local `.claude/skills/` (confirmed via `/skills` showing only plugin skills) — every documented cause was ruled out (YAML validity, `skillOverrides`, directory structure, trust dialog, version). A different tab on the same project loads them fine. If `/resume` isn't available either, this is that same issue recurring — fall back to asking the assistant to read this file directly.
