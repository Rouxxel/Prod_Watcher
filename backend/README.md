# ProdWatch API

**Spring Boot 3 + Java 17** REST API for ProdWatch inventory and POS. Uses JPA, Flyway, Supabase PostgreSQL, and Supabase JWT auth.

| Resource | Location |
| --- | --- |
| **API reference** | [`API.md`](API.md) |
| Database schema | [`docs/DATABASE_SCHEMA.md`](../docs/DATABASE_SCHEMA.md) |
| Supabase setup | [`docs/SUPABASE_SETUP.md`](../docs/SUPABASE_SETUP.md) |
| Frontend app | [`frontend/README.md`](../frontend/README.md) |

Base package: **`com.prodwatch.api`**. Gradle project: **`prodwatch-api`**.

---

## Quick start (local + Supabase)

### 1. Prerequisites

- JDK 17+
- Supabase project with Postgres + Auth ([`docs/SUPABASE_SETUP.md`](../docs/SUPABASE_SETUP.md))

### 2. Configure environment

```bash
cd backend
cp .env.example .env
```

Edit `.env` with your Supabase credentials:

| Variable | Purpose |
| --- | --- |
| `DATABASE_URL` | JDBC URL to Supabase Postgres (see `.env.example` for direct vs pooler) |
| `SUPABASE_URL` | Project URL (`https://<ref>.supabase.co`) |
| `SUPABASE_JWT_SECRET` | Settings → API → JWT Secret |
| `SUPABASE_SERVICE_ROLE_KEY` | Admin API (user provisioning) — **server only** |
| `SUPABASE_ANON_KEY` | Auth sign-up/login proxy |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins (no spaces) |

Optional:

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` (config) | HTTP port |
| `POS_TAX_RATE` | `0.16` | Checkout tax rate |
| `POS_WAREHOUSE_ID` | — | Fixed warehouse for POS stock deduction |

### 3. Run

```bash
./gradlew bootRun          # Windows: gradlew.bat bootRun
# or
./start.sh                 # Windows: start.bat
```

`bootRun` and the start scripts load `backend/.env` automatically.

On first run, Flyway applies migrations `V1`–`V12` against Supabase Postgres.

### 4. Seed dev users (optional)

After migrations:

```powershell
# Windows
.\scripts\seed-auth-users.ps1
```

```bash
# macOS/Linux
bash scripts/seed-auth-users.sh
```

Default password: `ProdWatchDev2024!` (override with `DEV_SEED_PASSWORD` in `.env`).

Alternatively, use **owner bootstrap**: `GET /api/v1/auth/bootstrap-status` → `POST /api/v1/auth/signup` when no admin exists.

### 5. Verify

| URL | Purpose |
| --- | --- |
| http://localhost:8080/ | Root health |
| http://localhost:8080/api/v1/health | DB connectivity |
| http://localhost:8080/docs | Swagger UI |
| http://localhost:8080/actuator/health | Spring Actuator |

Log in via `POST /api/v1/auth/login`, then call protected routes with `Authorization: Bearer <token>`. See [`API.md`](API.md).

---

## CORS checklist

`CORS_ALLOWED_ORIGINS` must list **every** browser origin that calls this API:

1. **Local dev** — `http://localhost:8000` (frontend Vite), plus legacy ports if needed
2. **Vercel production** — add `https://your-app.vercel.app` after first frontend deploy
3. **Cloudflare Workers** (optional edge target) — add the Workers URL only if deployed

After updating CORS on Render, redeploy the backend service. See [`frontend/README.md`](../frontend/README.md) for frontend deploy and CORS configuration.

---

## Docker

### Compose (local)

```bash
docker compose up --build
```

### Production image

```bash
docker build -t prodwatch-api .
docker run -p 8080:8080 --env-file .env prodwatch-api
```

The Dockerfile runs `./gradlew clean bootJar` and starts `java -jar app.jar` on port **8080**.

---

## Render deployment

1. Create a **Web Service** pointing at the `backend/` directory (or connect the repo and set root directory to `backend`).
2. **Build command:** `./gradlew clean bootJar -x test`
3. **Start command:** `java -jar build/libs/app.jar`
4. Set environment variables (same as `.env.example`). Use the **session pooler** JDBC URL for `DATABASE_URL` on Render.
5. Set `CORS_ALLOWED_ORIGINS` to your Vercel URL + local dev origins.
6. Post-deploy smoke test:
   - `GET /actuator/health`
   - `GET /docs`
   - Preflight `OPTIONS` from Vercel origin
   - Authenticated `GET /api/v1/products` from the hosted frontend

Alternatively, deploy via Docker using the included `Dockerfile`.

---

## Development

```bash
./gradlew test          # H2 in-memory + integration tests
./gradlew clean bootJar # → build/libs/app.jar
```

Test profile uses `src/test/resources/application-test.properties` and `schema-h2.sql`.

API routes and rate limits are config-driven: `src/main/resources/core_specs/configuration/config_file.json`.

Logs are written to `logs/` via `CustomLogger` (configured in the same JSON file).

---

## Project layout

```
backend/
├── API.md                 # REST contract (this doc's companion)
├── src/main/java/...      # com.prodwatch.api.*
├── src/main/resources/
│   ├── db/migration/      # Flyway V1–V12
│   └── core_specs/        # config_file.json, general_data.json
├── scripts/               # seed-auth-users.*
└── .env.example
```
