# Secrets externalization, Docker infra, and CI pipeline

## Context

The project currently hardcodes DB credentials and the JWT signing secret directly
in `src/main/resources/application.yml`, which is committed to git. `Dockerfile`
and `docker-compose.yml` exist but are empty (0 bytes). `.github/workflows/ci.yml`
and `cd.yml` exist but are empty placeholders. There is no local infra story for
MySQL/Redis beyond "have them running on localhost somehow."

Goal: externalize secrets, stand up a local dev infra (MySQL + Redis + phpMyAdmin)
via docker-compose, add a production-shaped Dockerfile for the app itself, and
implement a real CI pipeline. CD is explicitly deferred (see Decisions).

## Decisions

- **Secrets mechanism**: `.env` file (gitignored) + `.env.example` (committed
  template) + `application.yml` reads values via `${VAR_NAME}` placeholders,
  consistent with the existing `${MYSQL_HOST:localhost}` pattern already in the
  file. No Docker secrets / Vault / Spring Cloud Config — out of scope for a
  mini project, no infra to support it.
- **docker-compose scope**: infra-only — `mysql`, `redis`, `phpmyadmin`. The
  Spring Boot app is NOT containerized in this compose file; it keeps running
  via `./mvnw spring-boot:run` or the IDE during development, connecting to the
  containers over `localhost`. This preserves hot-reload/debugging.
- **Dockerfile**: still built, as a multi-stage production image for the app.
  Not wired into docker-compose (per above) — its purpose is (a) prove the app
  is containerizable, (b) get exercised by CI's `docker build` step, (c) be
  ready to reuse whenever a real CD/deployment target exists.
- **CI scope**: build + test (against real MySQL/Redis service containers in
  GitHub Actions, matching production-like behavior instead of mocking) +
  `docker build` (no push, no registry credentials needed).
- **CD**: deferred. No deployment target (VPS/K8s) exists yet. `cd.yml` is left
  as a documented placeholder, not implemented, to avoid building deployment
  logic against an unknown target.
- **Testcontainers for integration tests**: considered, rejected for now — adds
  a new dependency and test-infra rework beyond what was asked. Can be a future
  improvement once real integration tests exist (currently only `contextLoads`).

## Changes

### 1. `src/main/resources/application.yml`

Replace hardcoded values with env placeholders:

```yaml
spring:
  datasource:
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
app:
  jwt:
    secret: ${JWT_SECRET}
```

`MYSQL_HOST`/`REDIS_HOST` placeholders already exist and are kept as-is.

### 2. `.env` (new, gitignored) and `.env.example` (new, committed)

`.env` holds real local dev values (new JWT secret — the current one is
considered leaked via git history and must not be reused).

`.env.example` documents every variable with a placeholder/description, no real
values:

```
MYSQL_HOST=localhost
DB_USERNAME=root
DB_PASSWORD=changeme
REDIS_HOST=localhost
JWT_SECRET=changeme-generate-a-256-bit-base64-secret
FRONTEND_URL=http://localhost:5173
```

### 3. `.gitignore`

Add `.env` (keep `.env.example` tracked).

### 4. `Dockerfile`

Multi-stage:
- Stage 1 (`builder`): `maven:3.9-eclipse-temurin-21` (or equivalent), copy
  `pom.xml` first + `mvn dependency:go-offline` for layer caching, then copy
  `src/` and run `mvn package -DskipTests`.
- Stage 2 (`runtime`): `eclipse-temurin:21-jre-alpine`, non-root user, copy the
  built jar from stage 1, `EXPOSE 8081`, `ENTRYPOINT ["java", "-jar", "app.jar"]`.

### 5. `docker-compose.yml`

Three services, one bridge network, named volumes for persistence:

- `mysql`: official `mysql:8.4` image, env from `.env`
  (`MYSQL_ROOT_PASSWORD`/`MYSQL_DATABASE` derived from the same `DB_*` vars),
  volume `mysql_data:/var/lib/mysql`, healthcheck via `mysqladmin ping`.
- `redis`: `redis:7-alpine`, volume `redis_data:/data`.
- `phpmyadmin`: `phpmyadmin:latest`, `PMA_HOST=mysql`, depends_on mysql
  (healthy), exposed on a host port (e.g. 8082) to avoid clashing with the app's
  8081.

`env_file: .env` on the services that need credentials.

### 6. `.github/workflows/ci.yml`

Trigger: `push`/`pull_request` targeting `main`.

Steps:
1. Checkout
2. Setup JDK 21 (temurin)
3. Cache Maven `~/.m2`
4. `mvn -B test` — with GitHub Actions `services:` block running `mysql:8.4`
   and `redis:7-alpine` as sidecar containers, using CI-only throwaway
   credentials injected as env vars (not the real `.env`, no repo secrets
   needed for this stage).
5. `docker build -t spring-boot-mini-project:ci .` — validates the Dockerfile
   builds; image is not pushed anywhere.

### 7. `.github/workflows/cd.yml`

Left empty except a header comment explaining it's a placeholder pending a
real deployment target (VPS/K8s), so it's not mistaken for an oversight.

## Out of scope

- Containerizing the app in docker-compose for local dev.
- Any real deployment/CD logic.
- Testcontainers or other integration-test infra changes.
- SecurityConfig/JWT filter implementation (separate future task).
- Actuator, OpenAPI, global exception handling (separate future task).

## Testing

- `./mvnw test` passes locally against the new docker-compose MySQL/Redis
  (manually verified: start compose, run tests, stop compose).
- `docker build .` succeeds locally.
- CI workflow runs green on a throwaway branch/PR before merging to main.
