# Phase 8 — Database verification

End-to-end checklist after migrations **V1–V12** and auth seed on Supabase.

---

## Quick steps to finish verification

1. **Fix `DATABASE_URL`** in `backend/.env` (real Supabase connection string, not placeholders).
2. **Auth + activity seed** (if not done):
   ```powershell
   .\backend\scripts\seed-auth-users.ps1
   ```
   If `stock_movements` is still empty, run in **SQL Editor**:
   ```sql
   SELECT public.seed_demo_activity();
   ```
3. **Verify** — paste [`backend/scripts/verify-database.sql`](../backend/scripts/verify-database.sql) into Supabase SQL Editor and check results.
4. **Backend Flyway** (optional) — start backend with `.env` loaded; Flyway baselines at V12 if the schema already exists:
   ```powershell
   cd backend
   .\gradlew.bat bootRun
   ```

---

## Why some tables start empty

| Table / view | Populated when |
| --- | --- |
| `warehouses`, `products` | Migration `V12__seed_data.sql` |
| `profiles` | Auth user created → trigger |
| `user_roles` | Seed script or manual SQL |
| `stock_movements`, `audit_entries`, `transactions` | `seed_demo_activity()` |
| `inventory_balances` | View over `stock_movements` |

See [`backend/scripts/README.md`](../backend/scripts/README.md).

---

## Expected row counts (after full seed)

| Entity | Count |
| --- | --- |
| warehouses | 3 |
| products | 12 |
| profiles | 6 |
| user_roles | 6 |
| stock_movements | 19 |
| audit_entries | 6 |
| transactions | 2 |

---

## Expected stock (matches `frontend/src/mock/seed.ts`)

| SKU | Stock |
| --- | --- |
| KTL-001 | 42 |
| WCB-220 | 6 |
| APR-CHR | 0 |
| TMP-58 | 24 |
| MUG-S4 | 88 |
| CBC-1L | 4 |
| CIS-10 | 17 |
| TEA-BMB | 31 |
| VAS-HT1 | 9 |
| WTB-77 | 12 |
| CDL-BW3 | 60 |
| MLK-350 | 2 |

Query: section 7 in `verify-database.sql`.

---

## Phase 8 checklist

### Schema

| Item | How to verify |
| --- | --- |
| Migrations apply cleanly | V1–V12 applied in SQL Editor or via Flyway on `bootRun` |
| Enums match frontend types | `verify-database.sql` §3 |
| Foreign keys | `verify-database.sql` §9 |
| No `stock` on `products` | `verify-database.sql` §2 |

### Security

| Item | How to verify |
| --- | --- |
| RLS on all tables | `verify-database.sql` §4 |
| Roles in `user_roles` | `verify-database.sql` §5 |
| `has_role()` works | `verify-database.sql` §6 |
| Devon inactive | `is_active_user` = false; Auth ban in Dashboard |
| Auth redirect URLs | [`SUPABASE_SETUP.md`](SUPABASE_SETUP.md) |
| Bootstrap sign-up guard | TASK_02 backend |
| Admin-provisioned skip confirm | Seed script uses `email_confirm: true` |

### Data

| Item | How to verify |
| --- | --- |
| Warehouses + products | `verify-database.sql` §1 |
| Movements, audit, transactions | §1 counts = 19 / 6 / 2 |
| Stock matches mock | §7 all `matches_mock = true` |
| Audit append-only | §8 — only SELECT policy on `audit_entries` |

### Tooling

| Item | Status |
| --- | --- |
| Flyway on backend startup | Wired in `application.properties` + JDBC (needs valid `DATABASE_URL`) |
| Backend connects via `DATABASE_URL` | `spring.datasource.url=${DATABASE_URL}` |
| Frontend auth via Java API | TASK_03 (no Supabase client on frontend) |

---

## Flyway + manual SQL Editor

If you created the schema in the SQL Editor before starting the backend:

- `spring.flyway.baseline-on-migrate=true` and `spring.flyway.baseline-version=12` auto-baseline existing schemas.
- Schema at or below the baseline (V12) is not re-run on `bootRun`.
- Fresh empty databases run **V1–V12** from scratch.

---

## Troubleshooting

| Problem | Action |
| --- | --- |
| `stock_movements` empty | Run `SELECT seed_demo_activity();` in SQL Editor |
| Flyway fails on boot | Fix `DATABASE_URL`; use the Session Pooler host and ensure the password/project ref are correct |
| Counts wrong | Re-run `seed_demo_activity()` (idempotent) |

---

## Related

- [`DATABASE_SCHEMA.md`](DATABASE_SCHEMA.md)
- [`SUPABASE_SETUP.md`](SUPABASE_SETUP.md)
- [`TASK_01_database.md`](TASK_01_database.md)
