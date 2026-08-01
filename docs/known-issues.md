# Known Issues — Auth & User Module

Findings from a full code review of the auth/user module (2026-08-01). The
three most critical ones (transaction/rollback bugs that broke core auth
flows) are already fixed with tests. The rest are noted here, unfixed,
ordered by priority.

## Fixed already

- ~~`AuthServiceImpl` login/refreshToken/logout inherited a read-only
  transaction and broke every DB write~~ — fixed, see
  `AuthServiceImplIntegrationTest`.
- ~~`RefreshTokenServiceImpl.rotate()` rolled back the "revoke all sessions"
  write when reuse was detected~~ — fixed via self-injected
  `REQUIRES_NEW`, see `RefreshTokenServiceImplIntegrationTest`.
- ~~`rotate()` checked expired before revoked, skipping reuse detection~~ —
  fixed (order swapped), see `RefreshTokenServiceImplTest`.
- ~~`RequestUtils.clientIp()` trusted `X-Forwarded-For` blindly, letting
  rate limiting be bypassed~~ — fixed (header ignored entirely; no reverse
  proxy exists in this deployment), see `RequestUtilsTest`.

## Open — deferred

### #5 — Last-admin removal race condition (investigation in progress, paused)

`UserServiceImpl.updateRole()`/`delete()` guard the "at least one ADMIN
must remain" invariant with a check-then-act `countByRole(Role.ADMIN) <= 1`.
Two concurrent admin-removal requests can both pass the guard and leave zero
admins. Reproduced reliably (~4/5 runs) with a real two-thread
`@SpringBootTest`.

A first fix attempt — `@Lock(PESSIMISTIC_WRITE)` on a
`SELECT ... WHERE role = :role` repository query — did not work empirically:
the generated SQL included the lock clause, but the race still reproduced.
Root cause not yet confirmed (unclear whether MySQL isn't blocking as
expected here, or the Spring/Hibernate/HikariCP transaction wiring is losing
the lock). See the disabled test's Javadoc for the full trail:
`src/test/java/.../module/user/service/impl/UserServiceImplConcurrencyTest.java`
(currently `@Disabled`, kept as the RED spec to re-enable once fixed).

### #6 — Duplicate email race falls through to a raw 500

`AuthServiceImpl.register()` / `UserServiceImpl.create()` do a check-then-act
`existsByEmail()` before `save()`. Two concurrent registrations with the same
email can both pass the check; the second hits the DB's unique constraint
and throws `DataIntegrityViolationException`, which has no
`@ExceptionHandler` in `GlobalExceptionHandler` — falls through to the
generic 500 instead of the intended 409 `DuplicateResourceException`
response.

**Fix:** add an `@ExceptionHandler(DataIntegrityViolationException.class)`
mapping to 409 in `GlobalExceptionHandler`, as defense in depth (this alone
doesn't remove the race, just makes its outcome correct).

### #7 — Auth cookies hardcode `secure(false)`

`CookieUtils.setAuthCookies()` sets `.secure(false)` unconditionally on both
`access_token` and `refresh_token` cookies, with no env/profile gating.
`clearAuthCookies()` doesn't set `secure` at all either.

**Fix:** make it configurable via `app.cookie.secure` (default `true`),
threaded through as a parameter from `AuthController` (`@Value` injected)
rather than converting `CookieUtils` into a Spring bean.

### #8 — `UsernameNotFoundException` uncaught in `JwtAuthenticationFilter`

If a valid, unexpired JWT refers to a user deleted (or renamed) after the
token was issued, `userDetailsService.loadUserByUsername()` throws
`UsernameNotFoundException`, which the filter's
`catch (JwtException | IllegalArgumentException)` doesn't cover. It
propagates uncaught, running before Spring Security's
`ExceptionTranslationFilter` gets a chance to intercept it — producing an
unhandled 500 instead of a clean 401.

**Fix:** broaden the catch clause (or catch `AuthenticationException`, the
common parent) and treat it the same as an invalid token (clear context,
continue the chain unauthenticated).

### #9 — NPE on null refresh token

`RefreshTokenServiceImpl.hash()` calls `rawToken.getBytes(...)` with no null
check. `AuthController.refresh()` uses `@RequestBody(required = false)`
with no `@Valid`, so calling `/api/v1/auth/refresh` with no body and no
`refresh_token` cookie leaves the raw token `null`, and `hash(null)` NPEs
instead of throwing a clean `InvalidRefreshTokenException`.

**Fix:** add a null/blank check at the top of `rotate()`, before `hash()` is
called.

### #10 — Silent exception handler + logout token-source mismatch

Two related issues:
- `GlobalExceptionHandler`'s catch-all `@ExceptionHandler(Exception.class)`
  never logs anything (`ex` is unused, no `Logger` in the file) — every
  unmapped failure vanishes with zero trace in application logs.
- `AuthController.logout()` resolves the access token header-first,
  cookie-fallback, while `JwtAuthenticationFilter.resolveToken()` (used to
  authenticate the very same request) checks cookie-first,
  header-fallback. A request with both a valid cookie and a stale header
  authenticates via the cookie but logs out using the stale header token,
  which can throw uncaught into the unlogged handler above.

**Fix:** add a logger to `GlobalExceptionHandler`'s catch-all; extract a
single shared "resolve token from cookie-or-header" helper (e.g. in
`CookieUtils`) used by both `JwtAuthenticationFilter` and
`AuthController.logout()`, so precedence can't drift between them again.

## Noted but not prioritized

- CSRF is disabled (`ignoringRequestMatchers`) for `/api/v1/auth/logout` and
  `/api/v1/auth/refresh`. Real gap, but largely mitigated in practice by
  `SameSite=Lax` on the auth cookies.
- `/swagger-ui/**` and `/v3/api-docs/**` are `permitAll()`. `springdoc-openapi`
  was added and the docs are now live and public at `/swagger-ui/index.html` —
  this is an intentional choice (confirmed with the project owner), not an
  oversight. Revisit `SecurityConfig` if the docs should ever be restricted.
- JWT is parsed/verified 2–4 times per request in `JwtAuthenticationFilter`
  and `AuthServiceImpl.logout()` instead of once and reused.
- `AuthService`/`RefreshTokenService`/`UserService`/`TokenBlacklistService`
  each have exactly one implementation but are still split into
  interface + impl.
- The JSON-error-response-writing boilerplate is duplicated identically
  across `CustomAccessDeniedHandler`, `CustomAuthenticationEntryPoint`, and
  `RateLimitFilter`.
