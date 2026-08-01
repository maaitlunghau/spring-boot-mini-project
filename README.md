# Spring Boot Mini Project

[![CI](https://github.com/maaitlunghau/spring-boot-mini-project/actions/workflows/ci.yml/badge.svg)](https://github.com/maaitlunghau/spring-boot-mini-project/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=spring&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)

A Spring Boot backend built as a hands-on exercise in production-grade practices: externalized configuration, containerized local infrastructure, and a real CI pipeline — not just a "hello world" starter.

> **Status:** early stage. Application skeleton, infrastructure, and CI/CD are in place; domain features (auth, entities, endpoints) are actively being built. See [Roadmap](#roadmap).

---

## Table of Contents

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

```
spring-boot-mini-project/
├── src/
│   ├── main/
│   │   ├── java/com/maaitlunghau/spring_boot_mini_project/
│   │   │   └── SpringBootMiniProjectApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/maaitlunghau/spring_boot_mini_project/
│           └── SpringBootMiniProjectApplicationTests.java
├── .github/workflows/       # CI and CD pipelines
├── docs/superpowers/        # design specs & implementation plans for this project
├── Dockerfile                # multi-stage production image for the app
├── docker-compose.yml        # local dev infra: MySQL, Redis, phpMyAdmin
├── run-dev.sh                 # loads .env and starts the app in one command
├── .envrc                     # direnv config (auto-loads .env on cd)
├── .env.example               # template for required environment variables
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

- [ ] Spring Security configuration + JWT authentication filter
- [ ] User entity, repository, and registration/login endpoints
- [ ] Global exception handling (`@ControllerAdvice`)
- [ ] `spring-boot-starter-actuator` for health checks and metrics
- [ ] API documentation (OpenAPI/Swagger)
- [ ] Database migrations (Flyway/Liquibase) instead of `ddl-auto: update`
- [ ] Real CD pipeline once a deployment target is chosen
