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

## Ecosystem isolation (multi-tenant)

Each business is an **ecosystem**. The API enforces `ecosystem_id` on every tenant query (warehouses, products, movements, transactions, audit, users, settings). JWT auth requires a non-null `profiles.ecosystem_id` (assigned on sign-up or login bootstrap).

| Flow | Behavior |
| --- | --- |
| `POST /auth/signup` | Supabase user + profile → **new ecosystem** + `admin` role |
| `POST /auth/login` / `confirm-email` | Ensures ecosystem + role if missing (orphan profiles) |
| `POST /users` (provision) | Staff in **admin's** ecosystem |
| List / get by UUID | Scoped to caller's ecosystem; other tenant's id → **404** |

**Demo data:** `scripts/seed-auth-users.*` attaches seed users to the fixed Acme Demo ecosystem (`33333333-3333-4333-8333-333333333301`). New sign-ups do not see it.

**Defense in depth:** Postgres RLS and storage policies (greenfield V12 and V13) scope direct Supabase access; Java API uses service role and enforces tenancy in services.

See [`src/main/resources/db/migration/README.md`](src/main/resources/db/migration/README.md), [`docs/DATABASE_SCHEMA.md`](../docs/DATABASE_SCHEMA.md), and [`API.md`](API.md) (`UserResponse.ecosystemId`).

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
| `SUPABASE_URL` | Project URL (`https://<ref>.supabase.co`) — also used for JWKS (ES256 JWT) |
| `SUPABASE_JWT_SECRET` | Legacy HS256 fallback (optional if project uses asymmetric signing) |
| `SUPABASE_SERVICE_ROLE_KEY` | Admin API (user provisioning) — **server only** |
| `SUPABASE_ANON_KEY` | Auth sign-up/login proxy |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins (no spaces) |
| `DEV_SEED_PASSWORD` | Password applied when seeding auth users |

Optional:

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` (config) | HTTP port |
| `POS_TAX_RATE` | `0.16` | **Bootstrap only** — seeds `workspace_settings.tax_rate` on first API access when no row exists; checkout reads tax from Settings after that |
| `POS_WAREHOUSE_ID` | — | Fixed warehouse for POS when cart items omit `warehouseId` |
| `SWAGGER_ENABLED` | `true` | Enable/disable Swagger UI and API docs (set to `false` in production) |

### 3. Run

```bash
./gradlew bootRun          # Windows: gradlew.bat bootRun
# or
./start.sh                 # Windows: start.bat
```

`bootRun` and the start scripts load `backend/.env` automatically.

On a **new, empty database**, Flyway applies migrations `V1`–`V14` against Supabase Postgres.
Startup runs `flyway.repair()` then `migrate()` to heal checksum drift from line-ending edits on already-applied migrations. **Never start this build against a database with legacy V1–V26 Flyway history:** reused version numbers have different descriptions and checksums. Keep existing databases on the pre-squash release or branch.

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

Password is controlled by `DEV_SEED_PASSWORD` in `.env` (see `.env.example`).
Existing Supabase users are **not** updated automatically — reset via Admin API if needed.

Alternatively, use **owner sign-up**: `POST /api/v1/auth/signup` → confirm email → login (creates a **new** ecosystem with empty inventory).

### 5. Verify

| URL | Purpose |
| --- | --- |
| http://localhost:8080/ | Root health |
| http://localhost:8080/api/v1/health | DB connectivity |
| http://localhost:8080/docs | Swagger UI |
| http://localhost:8080/actuator/health | Spring Actuator |

Log in via `POST /api/v1/auth/login`, then call protected routes with `Authorization: Bearer <token>`. See [`API.md`](API.md).

---

## Domain behavior (integration notes)

### Authentication

- JWT verification supports **ES256/RS256** via Supabase JWKS (`{SUPABASE_URL}/auth/v1/.well-known/jwks.json`) and **HS256** via `SUPABASE_JWT_SECRET` when present.

### Products & stock

- `Product.stock` in API responses is computed from the `inventory_balances` view.
- **`GET /products?warehouseId=`** — when `warehouseId` is set, `stock` is the quantity at that warehouse; otherwise stock at the product's **default warehouse**.
- `product_stock_summary` and `inventory_balances` are ecosystem-scoped (greenfield V8); stock is per default warehouse within the tenant.

### Stock movements

| Type | Warehouses | Extra fields |
| --- | --- | --- |
| `IN` | `to_warehouse_id` required; `from_warehouse_id` null | **`provider`** (required) — external supplier |
| `OUT` | `from_warehouse_id` required; `to_warehouse_id` null | **`recipient`** (required) — customer / third party |
| `TRANSFER` | both warehouses, must differ | — |
| `ADJUSTMENT` | exactly one of from/to | — |

The movement schema is defined in `V7__stock_movements.sql`.

POS checkout creates `OUT` movements with `recipient = "POS customer"`. Refunds/voids create matching `IN` rows.

### Workspace settings

- **One row per ecosystem** in `workspace_settings` (greenfield `V11__workspace_settings.sql`, unique on `ecosystem_id`).
- **Tax at checkout** comes from the settings row for the caller's ecosystem (`SettingsService.getTaxRate(ecosystemId)`).
- **`POS_TAX_RATE`** (env → `prodwatch.pos.tax-rate`) seeds `tax_rate` when bootstrap creates the row for an ecosystem.
- Admins change tax, receipts, and business mode via **`PATCH /api/v1/settings`** or the frontend Settings page.

### POS / transactions

- Cart items may include **`warehouseId`**; checkout deducts stock at that warehouse per line.
- `TransactionResponse` includes **`cashierName`** (profile name) alongside `cashierId`.
- Transaction line items persist **`warehouseId`** in JSON for correct refund routing.

### Audit

- `AuditEntryResponse` includes **`userName`** and **`entityLabel`** (resolved product/warehouse/user names, movement product name, transaction total, etc.).

### PostgreSQL / JPA

- Native enum binding (`PostgreSQLEnumJdbcType`) for `app_role`, `stock_movement_type`, `transaction_status`.
- Filter queries on movements, transactions, and audit use `COALESCE` for optional date bounds (avoids Postgres parameter type inference errors).

---

## CORS checklist

`CORS_ALLOWED_ORIGINS` must list **every** browser origin that calls this API:

1. **Local dev** — `http://localhost:8000` (frontend Vite), plus legacy ports if needed
2. **Vercel production** — add `https://your-app.vercel.app` after first frontend deploy
3. **Cloudflare Workers** (optional edge target) — add the Workers URL only if deployed

After updating CORS on Render, redeploy the backend service. See [`frontend/README.md`](../frontend/README.md) for frontend deploy and CORS configuration.

---

## Optional Redis caching

Redis is **off by default** (`REDIS_ENABLED=false`). When disabled, cache calls no-op and Postgres remains the source of truth. Connection failures are logged; the API still serves traffic with `"redis": "unavailable"` on `GET /`.

TTLs live in [`src/main/resources/core_specs/configuration/config_file.json`](src/main/resources/core_specs/configuration/config_file.json) (`redis_cache.ttl_seconds`). Full mapping: [`REDIS_CACHE_TTL.md`](src/main/resources/core_specs/configuration/REDIS_CACHE_TTL.md).

| Mode | `REDIS_ENABLED` | `REDIS_HOST` | Notes |
| --- | --- | --- | --- |
| Off (default) | `false` | — | No Redis required |
| Local Docker | `true` | `localhost` | `docker run -p 6379:6379 redis:7-alpine` |
| Docker Compose | `true` | `redis` | Uncomment `redis` service in `docker-compose.yml` |
| Redis Cloud | `true` | `<endpoint>` | Set `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_TLS=true` |

Verify:

```bash
curl http://localhost:8080/
# {"message":"...","build":"...","redis":"disabled"|"connected"|"unavailable"}
```

Rate limiting remains **in-memory** per instance (`RateLimiter`) until a separate distributed limiter is added.

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
6. **Production security hardening:**
   - Set `SPRING_PROFILES_ACTIVE=prod` to activate production profile
   - Set `SWAGGER_ENABLED=false` to disable Swagger UI in production
7. Post-deploy smoke test:
   - `GET /actuator/health`
   - `GET /docs` (should return 404 if `SWAGGER_ENABLED=false`)
   - Preflight `OPTIONS` from Vercel origin
   - Authenticated `GET /api/v1/products` from the hosted frontend

Alternatively, deploy via Docker using the included `Dockerfile`.

---

## Development

```bash
./gradlew test          # H2 in-memory + integration tests
./gradlew clean bootJar # → build/libs/app.jar
```

Test profile uses `src/test/resources/application-test.properties` and `schema-h2.sql` (includes `provider` / `recipient` on `stock_movements`).

API routes and rate limits are config-driven: `src/main/resources/core_specs/configuration/config_file.json`.

Logs are written to `logs/` via `CustomLogger` (configured in the same JSON file).

---

## Project layout

```
backend/
├── API.md                 # REST contract
├── src/main/java/...      # com.prodwatch.api.*
├── src/main/resources/
│   ├── db/migration/      # Greenfield Flyway V1–V14
│   └── core_specs/        # config_file.json, general_data.json
├── scripts/               # seed-auth-users.*
└── .env.example
```

### Greenfield Flyway migrations

| Version | Purpose |
| --- | --- |
| V1 | Extensions and final enums, including `void_` |
| V2 | Auth helper functions |
| V3–V4 | Ecosystems; profiles, roles, and auth triggers |
| V5–V7 | Warehouses, products, and stock movements (including `provider` / `recipient`) |
| V8–V10 | Ecosystem-scoped views, audit entries (including `entity_label`), and transactions |
| V11–V12 | Per-ecosystem workspace settings and RLS |
| V13 | Storage bucket with ecosystem-prefixed write policies |
| V14 | Acme Demo ecosystem/catalog and ecosystem-aware demo activity seed |

This catalog is **greenfield-only**. Existing V1–V26 databases must remain on the pre-squash release or branch. Full catalog: [`src/main/resources/db/migration/README.md`](src/main/resources/db/migration/README.md).
