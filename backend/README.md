# ProdWatch API

**Spring Boot 3 + Java 17** REST API for ProdWatch inventory and POS. Built on a config-driven template with JPA, Flyway, Supabase PostgreSQL, and JWT auth.

Implementation plan: [`docs/TASK_02_backend.md`](../docs/TASK_02_backend.md)

Base package: **`com.prodwatch.api`**. Gradle project: **`prodwatch-api`**.

## Quick start

```bash
cd backend
cp .env.example .env   # set DATABASE_URL, SUPABASE_*, CORS_ALLOWED_ORIGINS
./gradlew bootRun      # Windows: gradlew.bat bootRun
```

Or use `start.sh` / `start.bat` (loads `.env`, offers dev/prod/Docker modes).

| URL | Purpose |
| --- | --- |
| http://localhost:8080/ | Root health check |
| http://localhost:8080/docs | Swagger UI |
| http://localhost:8080/actuator/health | Actuator |

API routes are under `/api/v1/*` (see `config_file.json`).

## Environment variables

See [`.env.example`](.env.example). Required: `DATABASE_URL`, `SUPABASE_URL`, `SUPABASE_JWT_SECRET`, `SUPABASE_SERVICE_ROLE_KEY`, `CORS_ALLOWED_ORIGINS`.

After Flyway migrations, seed dev users:

```bash
# Windows
.\scripts\seed-auth-users.ps1
# macOS/Linux
bash scripts/seed-auth-users.sh
```

## Docker

```bash
docker compose up --build
# or
docker build -t prodwatch-api .
docker run -p 8080:8080 --env-file .env prodwatch-api
```

## Development

```bash
./gradlew test          # H2 in-memory (application-test.properties)
./gradlew clean bootJar # → build/libs/app.jar
```

## Requirements

- JDK 17+
- Supabase project with Postgres + Auth (`docs/TASK_01_database.md`)
