# Spring Boot Mini Project

[![CI](https://github.com/maaitlunghau/spring-boot-mini-project/actions/workflows/ci.yml/badge.svg)](https://github.com/maaitlunghau/spring-boot-mini-project/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)

A Spring Boot backend built as a hands-on exercise in production-grade practices: externalized configuration, containerized local infrastructure, and a real CI pipeline — not just a "hello world" starter.

> **Status:** infrastructure (Docker, CI, secrets) and the Auth + User modules are done, code-reviewed, and hardened via TDD. Blog, Like/Comment, and Notification are planned next, one at a time. See [Modules & Features](#modules--features) and [Roadmap](#roadmap).

---

## Table of Contents

- [Modules & Features](#modules--features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Running the App](#running-the-app)
- [Running Tests](#running-tests)
- [Docker](#docker)
- [CI/CD](#cicd)
- [Project Structure](#project-structure)
- [Commit Convention](#commit-convention)
- [Roadmap](#roadmap)

---

## Modules & Features

This project is a deliberately small, self-contained domain (auth + a tiny social-blog shape) built to practice **Spring Data JPA** and **Spring Security** in depth rather than breadth — every module is taken through real design decisions, code review, and TDD instead of being scaffolded and left alone. Modules are built one at a time, in the order listed below; each gets its own design spec (entities, ERD, business rules) under [`docs/superpowers/specs/`](docs/superpowers/specs/) when its turn comes, and known defects are tracked with root cause in [`docs/known-issues.md`](docs/known-issues.md) rather than duplicated in this table.

| # | Module | Status |
| - | ------ | :----: |
| 1 | [Authentication & Authorization](#1-authentication--authorization) | ✅ Done |
| 2 | [User Management](#2-user-management) | ✅ Done |
| 3 | [Blog](#3-blog) | 🚧 Planned |
| 4 | [Like & Comment](#4-like--comment) | 🚧 Planned |
| 5 | [Notification](#5-notification) | 🚧 Planned |

### 1. Authentication & Authorization

Stateless JWT auth: a short-lived **access token** (5 min) authorizes requests, a long-lived opaque **refresh token** (7-day sliding window, 30-day absolute cap) issues new access tokens without forcing a re-login. Both are delivered as `httpOnly` cookies by default (a `Bearer` header also works — see `JwtAuthenticationFilter`). Refresh tokens **rotate on every use**: the old token is invalidated the moment a new one is issued, and replaying an already-rotated token is treated as theft — it triggers `RefreshTokenReuseException` and revokes every active session for that user, not just the one that leaked. Login/register/refresh are additionally rate-limited (5 attempts / 60s per client) to blunt credential-stuffing and brute force.

| Feature | Endpoint | Notes |
| --- | --- | --- |
| Register | `POST /api/v1/auth/register` | Email uniqueness enforced at the DB level |
| Login | `POST /api/v1/auth/login` | Issues access + refresh token as `httpOnly` cookies |
| Refresh token | `POST /api/v1/auth/refresh` | Rotates the refresh token; detects and responds to reuse/theft |
| Logout | `POST /api/v1/auth/logout` | Blacklists the current access token (Redis) + revokes its session |
| List active sessions | `GET /api/v1/auth/sessions` | One entry per device/refresh-token currently valid |
| Revoke a session | `DELETE /api/v1/auth/sessions/{sessionId}` | Remote "sign out this device" |
| View / update profile | `GET /api/v1/users/me`, `PATCH /api/v1/users/{id}/profile` | Served from the User module's controller, not a separate `/auth/profile` route |
| Forgot / reset password | — | 🚧 planned |
| Social login (OAuth2 / Auth0) | — | 🚧 planned |

Authorization is role-based (`ADMIN` / `USER`) via Spring Security method security (`@PreAuthorize`), enforced per-endpoint in the User module below.

### 2. User Management

Full administrative CRUD over accounts, plus self-service profile editing, sitting behind role checks rather than a single blanket rule — a regular user can only ever read/update their own record, an admin can manage everyone's.

| Feature | Endpoint | Access |
| --- | --- | --- |
| Create user | `POST /api/v1/users` | Admin only |
| List / search users (paginated, filter by role or keyword) | `GET /api/v1/users` | Admin only |
| Get user by id | `GET /api/v1/users/{id}` | Admin only |
| Update profile | `PATCH /api/v1/users/{id}/profile` | Self or admin |
| Change role | `PUT /api/v1/users/{id}/role` | Admin only |
| Delete user | `DELETE /api/v1/users/{id}` | Admin only |

Search/filtering is built on Spring Data JPA Specifications (`UserSpecifications`) rather than a growing pile of derived-query methods, so new filter criteria compose instead of multiplying methods.

### 3. Blog

🚧 **Planned.** Standard content CRUD — create, list (paginated), get by id, update, delete — owned by the authenticated user who authored it. Intended to be the first module built on top of the JPA relationship patterns (`User` 1—N `Post`) that Like/Comment will then extend.

### 4. Like & Comment

🚧 **Planned.** CRUD scoped to a blog post: one like per user per post (uniqueness constraint, not just application-level checking), threaded or flat comments TBD at spec time. Exercises JPA associations and cascade/orphan-removal semantics beyond the single-entity modules built so far.

### 5. Notification

🚧 **Planned.** Fan-out notifications (e.g. "someone liked/commented on your post") processed asynchronously through a **queue** rather than inline in the request path, delivered to connected clients **in real time**. Concrete transport (Redis Streams/pub-sub vs. a dedicated broker, WebSocket vs. SSE) is a decision for that module's design spec, not fixed yet.

## Tech Stack

| Layer          | Technology                                             |
| -------------- | ------------------------------------------------------- |
| Language       | Java 21                                                  |
| Framework      | Spring Boot 4.1.0 (Web MVC, Data JPA, Security, Validation) |
| Database       | MySQL 8.4                                                |
| Cache          | Redis 7                                                  |
| ORM            | Hibernate                                                |
| Build Tool     | Maven (via `mvnw` wrapper)                               |
| Containers     | Docker & Docker Compose                                  |
| CI/CD          | GitHub Actions                                           |
| Admin UI       | phpMyAdmin                                               |
| Git Hooks      | Husky (conventional commit enforcement)                  |

## Prerequisites

- [Java 21](https://adoptium.net/) (JDK)
- [Docker](https://www.docker.com/) & Docker Compose (or [OrbStack](https://orbstack.dev/) on macOS)
- Node.js + npm (only needed for Husky git hooks — not required to run the app)
- *(Optional, recommended)* [direnv](https://direnv.net/) — auto-loads `.env` when you `cd` into the project directory

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/maaitlunghau/spring-boot-mini-project.git
cd spring-boot-mini-project
```

### 2. Create your local environment file

Secrets and per-environment config are never committed to the repository. Copy the template and fill in real values:

```bash
cp .env.example .env
```

Then edit `.env`:

```env
MYSQL_HOST=localhost
DB_USERNAME=root
DB_PASSWORD=your-local-password
REDIS_HOST=localhost
JWT_SECRET=generate-a-real-256-bit-base64-secret
FRONTEND_URL=http://localhost:5173
```

Generate a strong JWT secret with:

```bash
openssl rand -base64 64
```

### 3. Load the environment variables

Spring Boot does **not** read `.env` files natively — you must load these variables into your shell before running the app. Pick whichever fits your workflow:

<details>
<summary><strong>Option A — direnv (recommended, auto-loads on <code>cd</code>)</strong></summary>

```bash
brew install direnv                        # macOS
# or: scoop install direnv                 # Windows (via scoop, works in PowerShell)
# or: sudo apt install direnv              # Linux / WSL

# add the hook once to your shell config, then restart your shell:
echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc     # zsh
# echo 'eval "$(direnv hook bash)"' >> ~/.bashrc # bash

direnv allow .
```

From now on, every time you `cd` into this project, `.env` is loaded automatically. Not supported on plain `cmd.exe` — use PowerShell or WSL on Windows.

</details>

<details>
<summary><strong>Option B — <code>run-dev.sh</code> helper script (macOS/Linux/WSL/Git Bash)</strong></summary>

```bash
./run-dev.sh
```

This exports `.env` and runs `./mvnw spring-boot:run` in one step. No install required.

</details>

<details>
<summary><strong>Option C — manual export</strong></summary>

```bash
export $(grep -v '^#' .env | xargs)     # macOS/Linux/WSL
./mvnw spring-boot:run
```

```powershell
Get-Content .env | ForEach-Object {
  if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
    [System.Environment]::SetEnvironmentVariable($matches[1], $matches[2])
  }
}
./mvnw.cmd spring-boot:run
```

</details>

### 4. Start local infrastructure

```bash
docker compose up -d
```

This starts:

| Service     | Host Port | Purpose                          |
| ----------- | --------- | --------------------------------- |
| MySQL       | `3306`    | Primary database                  |
| Redis       | `6379`    | Caching / session store           |
| phpMyAdmin  | `8080`    | Web UI for inspecting the database |

### 5. Run the application

```bash
./mvnw spring-boot:run
```

The API is now available at **http://localhost:8081**.

## Environment Variables

| Variable       | Description                                    | Example                        |
| -------------- | ----------------------------------------------- | ------------------------------- |
| `MYSQL_HOST`   | MySQL hostname                                  | `localhost`                     |
| `DB_USERNAME`  | MySQL username                                  | `root`                          |
| `DB_PASSWORD`  | MySQL password                                  | `your-password`                 |
| `REDIS_HOST`   | Redis hostname                                  | `localhost`                     |
| `JWT_SECRET`   | Base64-encoded secret for signing JWTs (256-bit+) | `openssl rand -base64 64`       |
| `FRONTEND_URL` | Allowed origin for CORS                         | `http://localhost:5173`         |

`.env` is gitignored — every developer and environment maintains their own copy, sourced from `.env.example`.

## Running the App

```bash
docker compose up -d && ./mvnw spring-boot:run
```

Stop the app with `Ctrl+C`. Stop infrastructure (keeps data volumes) with:

```bash
docker compose down
```

## Running Tests

Tests run against a real MySQL/Redis instance (via docker-compose), not mocks:

```bash
docker compose up -d
./mvnw test
```

## Docker

The application itself ships with a standalone, production-shaped multi-stage `Dockerfile` (not wired into `docker-compose.yml` — that file is infrastructure-only for local dev):

```bash
docker build -t spring-boot-mini-project:local .
docker run --rm -p 8081:8081 --env-file .env spring-boot-mini-project:local
```

The image runs as a non-root user on `eclipse-temurin:21-jre-alpine`.

## CI/CD

- **CI** (`.github/workflows/ci.yml`) runs on every push/PR to `main`: builds the project, runs the full test suite against real MySQL/Redis service containers, and verifies the Docker image builds. No secrets leave GitHub Actions — CI uses throwaway, non-production credentials.
- **CD** (`.github/workflows/cd.yml`) is a documented placeholder. No deployment target (VPS/Kubernetes) exists yet; wiring up real deployment is tracked in the [Roadmap](#roadmap).

## Project Structure

Feature-first: each domain gets its own module under `module/`, with the usual Spring layers inside it. Cross-cutting code lives at the top level.

```
spring-boot-mini-project/
├── src/
│   ├── main/java/com/maaitlunghau/spring_boot_mini_project/
│   │   ├── module/
│   │   │   ├── auth/          # login/refresh/logout, session management
│   │   │   │   ├── controller/v1/  service/impl/  repository/  entity/  dto/{request,response}/
│   │   │   └── user/          # user CRUD, roles, profile
│   │   │       └── controller/v1/  service/impl/  repository/  entity/  dto/{request,response}/
│   │   ├── common/            # ApiResponse, PageResponse, BaseEntity
│   │   ├── config/            # SecurityConfig, JwtAuthenticationFilter, CORS/CSRF, OpenAPI
│   │   ├── exception/         # AppException hierarchy + GlobalExceptionHandler
│   │   ├── security/          # JwtService, UserDetailsServiceImpl
│   │   ├── filter/            # RateLimitFilter
│   │   ├── scheduler/         # CleanupScheduledTask (expired refresh tokens)
│   │   ├── util/              # CookieUtils, RequestUtils, DateUtils
│   │   └── SpringBootMiniProjectApplication.java
│   ├── main/resources/application.yml
│   └── test/java/...          # unit tests (Mockito) + integration tests (@SpringBootTest, real MySQL/Redis)
├── .github/workflows/         # CI and CD pipelines
├── docs/
│   ├── known-issues.md        # tracked bugs — fixed vs. open, with root cause
│   └── superpowers/           # design specs & implementation plans for past work
├── Dockerfile                  # multi-stage production image for the app
├── docker-compose.yml          # local dev infra: MySQL, Redis, phpMyAdmin
├── run-dev.sh                   # loads .env and starts the app in one command
├── .envrc                       # direnv config (auto-loads .env on cd)
├── .env.example                 # template for required environment variables
└── pom.xml
```

## Commit Convention

Commits are enforced by a Husky git hook (`commit-msg`) using [Conventional Commits](https://www.conventionalcommits.org/):

```
type(scope): subject
```

- **Types:** `feat` `fix` `docs` `style` `refactor` `perf` `test` `chore` `revert` `ci`
- Subject must be lowercase, a single line, and ≤ 60 characters — no body/footer.

```bash
git commit -m "feat(auth): add jwt refresh token endpoint"
```

## Roadmap

Feature work (see [Modules & Features](#modules--features) for detail):

- [ ] Forgot / reset password
- [ ] Social login (OAuth2 / Auth0)
- [ ] Blog module (CRUD)
- [ ] Like & Comment modules
- [ ] Notification module (queue + realtime)

Hardening / infra:

- [ ] Fix remaining known issues #5–#10 — see [`docs/known-issues.md`](docs/known-issues.md)
- [ ] `spring-boot-starter-actuator` for health checks and metrics
- [ ] Database migrations (Flyway/Liquibase) instead of `ddl-auto: update`
- [ ] Real CD pipeline once a deployment target is chosen

Already done: Spring Security + JWT auth, user CRUD/roles, global exception handling, OpenAPI/Swagger docs.
