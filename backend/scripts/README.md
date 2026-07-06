# Backend scripts

Dev utilities for seeding Supabase auth users and demo activity data.

**Prerequisites**

- Flyway migrations **V1–V12** applied (see root [`README.md`](../../README.md))
- `backend/.env` copied from [`backend/.env.example`](../.env.example) with real Supabase values

**Required env vars** (in `backend/.env`):

| Variable | Purpose |
| --- | --- |
| `SUPABASE_URL` | Project URL, e.g. `https://your-ref.supabase.co` |
| `SUPABASE_SERVICE_ROLE_KEY` | Service role key (Dashboard → Settings → API) |

Optional: `DEV_SEED_PASSWORD` — defaults to `ProdWatchDev2024!` if unset.

`psql` and `DATABASE_URL` are **not** required; scripts use the Supabase Auth Admin API and REST API.

---

## `seed-auth-users.ps1` (Windows)

Creates dev auth users, assigns roles, deactivates/bans the inactive seed user, and loads demo movements, audit entries, and transactions.

### Run from repo root

```powershell
cd C:\path\to\Prod_Watcher
.\backend\scripts\seed-auth-users.ps1
```

### If PowerShell blocks the script

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\backend\scripts\seed-auth-users.ps1
```

### Expected output

- `created` or `exists` for each of the 6 seed emails
- Role lines for `alex@acme.co` (admin) through `devon@acme.co` (cashier)
- `banned devon@acme.co`
- Activity data loaded (or a note to run `seed-activity-only.sql` if the RPC step fails)

### Seed accounts

| Email | Role |
| --- | --- |
| `alex@acme.co` | admin |
| `maya@acme.co` | warehouse_manager |
| `jordan@acme.co` | warehouse_worker |
| `sam@acme.co` | inspector |
| `riley@acme.co` | cashier |
| `devon@acme.co` | cashier (inactive + banned) |

Default password for all: `ProdWatchDev2024!` (unless `DEV_SEED_PASSWORD` is set in `.env`).

---

## `seed-auth-users.sh` (macOS / Linux / Git Bash)

Same behavior as the PowerShell script.

### Run from repo root

```bash
cd /path/to/Prod_Watcher
bash backend/scripts/seed-auth-users.sh
```

Make executable (optional):

```bash
chmod +x backend/scripts/seed-auth-users.sh
./backend/scripts/seed-auth-users.sh
```

Requires `curl` and `bash`. No `psql` needed.

---

## `seed-activity-only.sql` (fallback)

If users and roles are set but `stock_movements`, `audit_entries`, or `transactions` are still empty, run in **Supabase Dashboard → SQL Editor**:

```sql
SELECT public.seed_demo_activity();
```

Or open [`seed-activity-only.sql`](seed-activity-only.sql) and execute its contents.

Safe to re-run — inserts are idempotent (`ON CONFLICT DO NOTHING`).

---

## Troubleshooting

| Issue | Fix |
| --- | --- |
| `Missing backend/.env` | Copy `backend/.env.example` → `backend/.env` and fill in Supabase credentials |
| `422 Unprocessable Entity` on first user | User already exists — re-run is OK; script prints `exists` |
| `remote name could not be resolved` | `SUPABASE_URL` still has placeholder values |
| `stock_movements still empty` after script | Run `seed-activity-only.sql` in SQL Editor, or re-run the seed script |
| Re-run after partial success | Script is idempotent; safe to run again |

---

## Related docs

- [`docs/SUPABASE_SETUP.md`](../../docs/SUPABASE_SETUP.md) — Auth & Storage dashboard setup
- [`docs/DATABASE_SCHEMA.md`](../../docs/DATABASE_SCHEMA.md) — Schema reference
- [`docs/DATABASE_VERIFICATION.md`](../../docs/DATABASE_VERIFICATION.md) — Phase 8 verification

## `verify-database.sql`

Run in **Supabase SQL Editor** after auth + activity seed to validate counts, stock, RLS, and helpers.
