# Secrets, Docker Infra, and CI Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Externalize DB/JWT secrets from `application.yml` into `.env`, stand up a
local dev infra (MySQL + Redis + phpMyAdmin) via docker-compose, add a
production-shaped multi-stage Dockerfile for the app, and implement a real CI
pipeline. CD stays a documented placeholder — no deployment target exists yet.

**Architecture:** `application.yml` reads secrets via `${VAR}` placeholders. A
gitignored `.env` holds real local values; a committed `.env.example`
documents the shape. `docker-compose.yml` runs only infra containers (mysql,
redis, phpmyadmin) — the app itself keeps running via `./mvnw spring-boot:run`
against those containers over `localhost`. The `Dockerfile` builds the app
image independently (used by CI to prove it builds, ready for a future real
deployment). CI runs tests against real MySQL/Redis service containers in
GitHub Actions and does a `docker build` (no push).

**Tech Stack:** Spring Boot 4.1.0 / Java 21, Docker & docker-compose, GitHub
Actions, MySQL 8.4, Redis 7.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-01-secrets-docker-ci-design.md`
- Never commit real secret values. `.env` is gitignored; `.env.example` holds
  placeholders only.
- The JWT secret currently in `application.yml`'s git history is considered
  leaked — do not reuse it anywhere, including in `.env.example` or CI.
- docker-compose in this plan is infra-only (mysql, redis, phpmyadmin). Do not
  containerize the Spring Boot app in `docker-compose.yml` — out of scope per
  spec decision.
- CD (`cd.yml`) must remain a documented no-op placeholder — do not add real
  deploy logic; no deployment target exists yet.
- No new dependencies (no Testcontainers, no dotenv library, no Docker
  secrets/Vault) — spec explicitly rejected these as out of scope.
- MySQL image: `mysql:8.4`. Redis image: `redis:7-alpine`. Runtime base image:
  `eclipse-temurin:21-jre-alpine`. Build image: `maven:3.9-eclipse-temurin-21`.

---

### Task 1: Externalize secrets into `.env`

**Files:**
- Modify: `src/main/resources/application.yml`
- Create: `.env` (gitignored, real values)
- Create: `.env.example` (committed, placeholder values)
- Modify: `.gitignore`

**Interfaces:**
- Produces: env var names `MYSQL_HOST`, `DB_USERNAME`, `DB_PASSWORD`,
  `REDIS_HOST`, `JWT_SECRET`, `FRONTEND_URL` — Task 3 (docker-compose) and
  Task 4 (CI) consume these same names.

- [ ] **Step 1: Update `application.yml` to use placeholders for secrets**

Replace the `datasource` and `jwt` blocks so the full file reads:

```yaml
spring:
  application:
    name: spring-boot-mini-project

  datasource:
    url: "jdbc:mysql://${MYSQL_HOST:localhost}:3306/spring-boot-mini-project?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true"
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    open-in-view: false
    properties:
      hibernate:
        format_sql: true

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: 6379

server:
  port: 8081

app:
  frontend-url: ${FRONTEND_URL:http://localhost:5173}
  jwt:
    secret: ${JWT_SECRET}
    access-token-expiration: 300000
    refresh-token-expiration: 604800000
    refresh-token-absolute-expiration: 2592000000
```

- [ ] **Step 2: Create `.env.example` (committed template, no real values)**

```
MYSQL_HOST=localhost
DB_USERNAME=root
DB_PASSWORD=changeme
REDIS_HOST=localhost
JWT_SECRET=changeme-generate-a-256-bit-base64-secret
FRONTEND_URL=http://localhost:5173
```

- [ ] **Step 3: Create `.env` (real local values, will be gitignored)**

```
MYSQL_HOST=localhost
DB_USERNAME=root
DB_PASSWORD=112233
REDIS_HOST=localhost
JWT_SECRET=/ltcEXFlwcqcQSl+Kc8P4DOW3N+nDknXdDEH7ZJ2GZ2SYgCAGDeuDXbFz5oZ1cejfpDV+tlESA0XYgCBh7FEQQ==
FRONTEND_URL=http://localhost:5173
```

- [ ] **Step 4: Add `.env` to `.gitignore`**

Append a line `.env` to the existing `.gitignore` (keep `.env.example`
untouched — it has a different name so it's tracked normally).

- [ ] **Step 5: Verify `.env` is actually ignored**

Run: `git check-ignore -v .env`
Expected: prints `.gitignore:<line>:.env	.env` (confirms it matches).

Run: `git status`
Expected: `.env` does NOT appear in untracked files; `.env.example`,
`application.yml`, `.gitignore` DO appear as modified/untracked.

- [ ] **Step 6: Verify the app still boots with the new placeholders**

MySQL must be reachable at `localhost:3306` for this (either the developer's
existing local MySQL, or wait until Task 3's docker-compose is up — if MySQL
isn't up yet, come back to this step after Task 3).

Run:
```bash
export $(grep -v '^#' .env | xargs)
./mvnw -q -o test
```
Expected: `BUILD SUCCESS`, `contextLoads` test passes — this confirms Spring
successfully resolved `${DB_USERNAME}`, `${DB_PASSWORD}`, `${JWT_SECRET}` from
the exported env vars (an unresolved placeholder would fail context startup
with `PlaceholderResolutionException`).

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/application.yml .env.example .gitignore
git commit -m "chore: externalize db and jwt secrets to env vars"
```

Do NOT `git add .env` — verify with `git status` after committing that `.env`
is still untracked.

---

### Task 2: Multi-stage `Dockerfile`

**Files:**
- Modify: `pom.xml:91` (add `<finalName>` inside `<build>`)
- Modify: `Dockerfile` (currently 0 bytes)

**Interfaces:**
- Consumes: nothing from other tasks.
- Produces: image built from this Dockerfile is referenced by Task 4 (CI
  `docker build` step) and is the artifact for any future real CD.

- [ ] **Step 1: Pin the jar's final name in `pom.xml`**

In `pom.xml`, change:
```xml
	<build>
		<plugins>
```
to:
```xml
	<build>
		<finalName>app</finalName>
		<plugins>
```

This decouples the Dockerfile from the version string in
`spring-boot-mini-project-0.0.1-SNAPSHOT.jar`, so version bumps don't break
the build.

- [ ] **Step 2: Verify the jar builds with the fixed name**

Run: `./mvnw -q -o -DskipTests package`
Expected: `target/app.jar` exists (`ls target/app.jar` succeeds).

- [ ] **Step 3: Write the Dockerfile**

```dockerfile
# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder /build/target/app.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 4: Verify the image builds**

Run: `docker build -t spring-boot-mini-project:local .`
Expected: `docker build` exits 0, final line `naming to
docker.io/library/spring-boot-mini-project:local`.

Run: `docker run --rm spring-boot-mini-project:local id`
Expected: output shows `uid=` for `spring`, not `root` (confirms non-root
user).

- [ ] **Step 5: Commit**

```bash
git add pom.xml Dockerfile
git commit -m "feat: add multi-stage production dockerfile"
```

---

### Task 3: `docker-compose.yml` — MySQL + Redis + phpMyAdmin

**Files:**
- Modify: `docker-compose.yml` (currently 0 bytes)

**Interfaces:**
- Consumes: env vars `DB_USERNAME`, `DB_PASSWORD` from `.env` (Task 1) via
  docker-compose's automatic `.env` interpolation (a file literally named
  `.env` next to `docker-compose.yml` is auto-loaded by Compose — no extra
  config needed for `${VAR}` substitution inside this file).
- Produces: `mysql` reachable at `localhost:3306`, `redis` at
  `localhost:6379`, `phpmyadmin` UI at `localhost:8080` — matches the
  `MYSQL_HOST=localhost` / `REDIS_HOST=localhost` values the app already
  expects from Task 1.

- [ ] **Step 1: Write `docker-compose.yml`**

```yaml
services:
  mysql:
    image: mysql:8.4
    container_name: spring-boot-mini-project-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: spring-boot-mini-project
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-p${DB_PASSWORD}"]
      interval: 5s
      timeout: 5s
      retries: 10
    networks:
      - backend

  redis:
    image: redis:7-alpine
    container_name: spring-boot-mini-project-redis
    restart: unless-stopped
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - backend

  phpmyadmin:
    image: phpmyadmin:latest
    container_name: spring-boot-mini-project-phpmyadmin
    restart: unless-stopped
    environment:
      PMA_HOST: mysql
      PMA_USER: ${DB_USERNAME}
      PMA_PASSWORD: ${DB_PASSWORD}
    ports:
      - "8080:80"
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - backend

networks:
  backend:
    driver: bridge

volumes:
  mysql_data:
  redis_data:
```

Note: `MYSQL_USER`/`MYSQL_PASSWORD` env vars are intentionally omitted — the
official MySQL image refuses to start if `MYSQL_USER=root` is set (root
already exists via `MYSQL_ROOT_PASSWORD`), and `DB_USERNAME` in `.env` is
`root`.

- [ ] **Step 2: Validate the compose file resolves variables correctly**

Run: `docker compose config`
Expected: prints the fully-resolved YAML with `MYSQL_ROOT_PASSWORD: "112233"`
(or whatever `.env` currently has) substituted in — not a literal
`${DB_PASSWORD}` string, and not a "variable is not set" warning.

- [ ] **Step 3: Bring the stack up and verify health**

Run: `docker compose up -d`

Run: `docker compose ps`
Expected: all three services show `running`/`healthy` (mysql may take ~10-20s
to become healthy — re-run `docker compose ps` if it's still `starting`).

- [ ] **Step 4: Verify each service is actually reachable**

Run: `docker exec spring-boot-mini-project-redis redis-cli ping`
Expected: `PONG`

Run: `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080`
Expected: `200`

- [ ] **Step 5: Go back and finish Task 1 Step 6 now that MySQL is up**

If Task 1 Step 6 (`./mvnw test`) wasn't verified yet because MySQL wasn't
running, run it now:
```bash
export $(grep -v '^#' .env | xargs)
./mvnw -q -o test
```
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Tear down (keep data volume) and commit**

Run: `docker compose down`

```bash
git add docker-compose.yml
git commit -m "feat: add docker-compose infra for mysql redis phpmyadmin"
```

---

### Task 4: CI pipeline (`ci.yml`)

**Files:**
- Modify: `.github/workflows/ci.yml` (currently 0 bytes)

**Interfaces:**
- Consumes: the same env var names as Task 1/3 (`DB_USERNAME`, `DB_PASSWORD`,
  `JWT_SECRET`, etc.) but with CI-only throwaway values — never the real
  `.env`.
- Consumes: `Dockerfile` from Task 2 (the `docker build` step builds it).

- [ ] **Step 1: Write `.github/workflows/ci.yml`**

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    services:
      mysql:
        image: mysql:8.4
        env:
          MYSQL_ROOT_PASSWORD: ci_test_password
          MYSQL_DATABASE: spring-boot-mini-project
        ports:
          - 3306:3306
        options: >-
          --health-cmd="mysqladmin ping -uroot -pci_test_password"
          --health-interval=5s
          --health-timeout=5s
          --health-retries=10

      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379
        options: >-
          --health-cmd="redis-cli ping"
          --health-interval=5s
          --health-timeout=5s
          --health-retries=10

    env:
      MYSQL_HOST: localhost
      DB_USERNAME: root
      DB_PASSWORD: ci_test_password
      REDIS_HOST: localhost
      JWT_SECRET: gMfIEwP2VOBiMJSxg16nbnS4sarzaXCr8DnDonKX1nBQwDXrvg05tuU4cdh6SIUGX/xFfzR4loUkiUbJIrzd9A==
      FRONTEND_URL: http://localhost:5173

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven

      - name: Run tests
        run: ./mvnw -B test

      - name: Build Docker image
        run: docker build -t spring-boot-mini-project:ci .
```

The `JWT_SECRET` here is a CI-only value (different from the real `.env` one
generated for local dev) — it exists only to satisfy Spring's placeholder
resolution during context startup in tests, never used against anything real.

- [ ] **Step 2: Validate the YAML is well-formed before pushing**

Run: `python3 -c "import yaml, sys; yaml.safe_load(open('.github/workflows/ci.yml'))" && echo OK`
Expected: `OK` (catches indentation errors before burning an Actions run).

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "feat: add ci workflow with mysql redis and docker build"
```

- [ ] **Step 4: Push and confirm the workflow runs green**

This step pushes to the remote — confirm with the user before running it if
executing this plan autonomously.

Run: `git push` (or push a branch and open a PR, per the user's preferred
workflow at execution time).

Then check the run:
```bash
gh run list --workflow=ci.yml --limit=1
gh run view --log $(gh run list --workflow=ci.yml --limit=1 --json databaseId -q '.[0].databaseId')
```
Expected: conclusion `success` for the `build-and-test` job.

---

### Task 5: CD placeholder (`cd.yml`)

**Files:**
- Modify: `.github/workflows/cd.yml` (currently 0 bytes)

**Interfaces:**
- Consumes: nothing (intentionally standalone, manual-trigger only).

- [ ] **Step 1: Write `.github/workflows/cd.yml`**

```yaml
name: CD

# Placeholder — no deployment target (VPS/K8s) exists yet for this project.
# When one exists, wire up:
#   1. Build & tag the Docker image (see ../../Dockerfile)
#   2. Push to a registry (Docker Hub / GHCR)
#   3. Deploy to the target (e.g. SSH + `docker compose pull && up -d`, or `kubectl apply`)
# Until then this workflow intentionally does nothing and only runs manually.

on:
  workflow_dispatch:

jobs:
  placeholder:
    runs-on: ubuntu-latest
    steps:
      - name: No-op
        run: echo "CD not implemented yet - see comment header in this file."
```

`workflow_dispatch`-only trigger means it never runs automatically on push —
it stays inert until manually invoked from the Actions tab.

- [ ] **Step 2: Validate the YAML is well-formed**

Run: `python3 -c "import yaml, sys; yaml.safe_load(open('.github/workflows/cd.yml'))" && echo OK`
Expected: `OK`

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/cd.yml
git commit -m "docs: document cd workflow as pending deployment target"
```

---

## Final check

- [ ] `git log --oneline -6` shows the 5 commits from this plan in order.
- [ ] `git status` is clean except for local runtime state (no `.env` leak,
  no stray `target/` changes).
- [ ] Re-run `docker compose up -d` one more time and `./mvnw test` (with
  `.env` exported) to confirm everything still works together end-to-end
  after all 5 tasks.
