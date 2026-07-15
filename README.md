# ProdWatch

Inventory and point-of-sale web app for businesses with one or more warehouses.

| Part | Stack | Docs |
| --- | --- | --- |
| **Frontend** | React 19, TanStack Start, TanStack Query, Tailwind v4 | [`frontend/README.md`](frontend/README.md) |
| **Backend** | Spring Boot 3, Java 17, JPA, Flyway, PostgreSQL (Supabase) | [`backend/README.md`](backend/README.md) |
| **API contract** | REST `/api/v1` | [`backend/API.md`](backend/API.md) |

---

## What it does

- **Inventory** — products, warehouses, stock movements (IN / OUT / TRANSFER / ADJUSTMENT), audit log, dashboard
- **POS** — cashier with per-warehouse catalog, multi-warehouse cart, checkout, transaction history
- **Auth & roles** — Supabase Auth (JWT), RBAC; each user belongs to one **ecosystem** (tenant)
- **Stock ledger** — movements are immutable; balances computed from the ledger

---

## Multi-tenancy (ecosystems)

ProdWatch runs as **multi-tenant SaaS** on a shared Supabase Postgres project. Each owner sign-up creates an isolated **ecosystem** (business workspace). Inventory, users, POS, audit, and settings are scoped by `ecosystem_id` in the Java API.

| Scenario | Behavior |
| --- | --- |
| **New owner sign-up** | New ecosystem + admin role + **empty** catalog |
| **Demo seed** (`seed-auth-users.ps1`) | Fixed **Acme Demo** ecosystem only (`alex@acme.co` + staff) |
| **Staff provision** | Inherits the admin's ecosystem |
| **Cross-tenant UUID** | Returns **404** (no data leakage) |

One Supabase project can host many businesses. Migrations **V19–V26** add ecosystems, RLS, and storage path isolation — see [`backend/src/main/resources/db/migration/README.md`](backend/src/main/resources/db/migration/README.md) and [`docs/DATABASE_SCHEMA.md`](docs/DATABASE_SCHEMA.md).

**Production:** apply Flyway through **V26**, deploy backend + frontend, then optionally run the dev seed script for demos only.

---

## Run locally

**Ports:** frontend `http://localhost:8000`, backend `http://localhost:8080`.

```bash
# Terminal 1 — backend
cd backend
cp .env.example .env   # fill Supabase credentials
./gradlew bootRun        # Windows: gradlew.bat bootRun

# Terminal 2 — frontend
cd frontend
cp .env.example .env     # VITE_API_BASE_URL=http://localhost:8080/api/v1
npm install
npm run dev
```

Seed dev users (optional): `backend/scripts/seed-auth-users.ps1` (see `backend/scripts/README.md`).

Default dev password is set in `backend/.env` as `DEV_SEED_PASSWORD` (see `.env.example`).

---

## Recent integration highlights

- Live backend API (no mock data); global API error toasts and dev logging
- Product images via optional Supabase Storage (`VITE_SUPABASE_*`)
- **Stock movements** — `provider` on IN (supplier), `recipient` on OUT (customer); ADJUSTMENT uses one warehouse + add/remove direction
- **Products** — optional `warehouseId` query shows stock at that warehouse; warehouse filter on the products page
- **Cashier** — warehouse filter; cart lines track `warehouseId` (mix warehouses in one sale)
- **Audit & transactions** — human-readable `userName` / `entityLabel` / `cashierName` instead of raw UUIDs
- **Backend** — ecosystem-scoped queries; Flyway `V19`–`V26` (multi-tenancy + storage paths); Supabase ES256 JWT (JWKS)

---

## Deploy

- **Backend** — Render or Docker ([`backend/README.md`](backend/README.md))
- **Frontend** — Vercel, root `frontend/` ([`frontend/README.md`](frontend/README.md))
- Set `CORS_ALLOWED_ORIGINS` on the backend to include your frontend URL(s)

---

## More docs

- [`docs/DATABASE_SCHEMA.md`](docs/DATABASE_SCHEMA.md) — schema, views, and multi-tenancy
- [`docs/SUPABASE_SETUP.md`](docs/SUPABASE_SETUP.md) — Supabase project setup
- [`backend/src/main/resources/db/migration/README.md`](backend/src/main/resources/db/migration/README.md) — Flyway catalog (V1–V26)
- [`backend/scripts/README.md`](backend/scripts/README.md) — dev seed scripts
