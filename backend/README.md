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
| `POS_TAX_RATE` | `0.16` | Checkout tax rate |
| `POS_WAREHOUSE_ID` | — | Fixed warehouse for POS when cart items omit `warehouseId` |

### 3. Run

```bash
./gradlew bootRun          # Windows: gradlew.bat bootRun
# or
./start.sh                 # Windows: start.bat
```

`bootRun` and the start scripts load `backend/.env` automatically.

On first run, Flyway applies migrations `V1`–`V15` against Supabase Postgres.
Startup runs `flyway.repair()` then `migrate()` to heal checksum drift from line-ending edits on already-applied migrations.

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

## Domain behavior (integration notes)

### Authentication

- JWT verification supports **ES256/RS256** via Supabase JWKS (`{SUPABASE_URL}/auth/v1/.well-known/jwks.json`) and **HS256** via `SUPABASE_JWT_SECRET` when present.

### Products & stock

- `Product.stock` in API responses is computed from the `inventory_balances` view.
- **`GET /products?warehouseId=`** — when `warehouseId` is set, `stock` is the quantity at that warehouse; otherwise stock at the product's **default warehouse**.
- `product_stock_summary` view unchanged — still keyed to `default_warehouse_id`.

### Stock movements

| Type | Warehouses | Extra fields |
| --- | --- | --- |
| `IN` | `to_warehouse_id` required; `from_warehouse_id` null | **`provider`** (required) — external supplier |
| `OUT` | `from_warehouse_id` required; `to_warehouse_id` null | **`recipient`** (required) — customer / third party |
| `TRANSFER` | both warehouses, must differ | — |
| `ADJUSTMENT` | exactly one of from/to | — |

Migrations: `V14__stock_movement_provider.sql`, `V15__stock_movement_recipient.sql`.

POS checkout creates `OUT` movements with `recipient = "POS customer"`. Refunds/voids create matching `IN` rows.

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
│   ├── db/migration/      # Flyway V1–V15
│   └── core_specs/        # config_file.json, general_data.json
├── scripts/               # seed-auth-users.*
└── .env.example
```

### Flyway migrations (recent)

| Version | Purpose |
| --- | --- |
| V13 | Rename `transaction_status` enum label `void` → `void_` (Java keyword) |
| V14 | Add `provider` on `stock_movements` (required for IN) |
| V15 | Add `recipient` on `stock_movements` (required for OUT) |
