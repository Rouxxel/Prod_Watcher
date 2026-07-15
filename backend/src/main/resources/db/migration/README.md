# Flyway migrations — ProdWatch

SQL schema history for the Supabase PostgreSQL database. Applied automatically on backend startup (`spring.flyway.enabled=true`) unless disabled.

**Full multi-tenancy plan:** [`docs/TASK_05_ecosystems.md`](../../../../docs/TASK_05_ecosystems.md)  
**Schema reference:** [`docs/DATABASE_SCHEMA.md`](../../../../docs/DATABASE_SCHEMA.md)  
**Backend setup:** [`backend/README.md`](../../../../README.md)

---

## How migrations run

1. `./gradlew bootRun` (or Docker / Render) starts the API.
2. Flyway runs `flyway.repair()` then `migrate()` (see `application.properties` — repairs checksum drift from line-ending edits on already-applied files).
3. New versioned scripts in this folder run **once**, in order.
4. JPA uses `ddl-auto=validate` — **Flyway owns the schema**; do not hand-edit production tables without a new migration.

**Local Supabase:** same DATABASE_URL as `backend/.env`.  
**Tests:** H2 in-memory schema is **not** Flyway-driven — update `backend/src/test/resources/schema-h2.sql` when you add migrations.

---

## Naming convention

```
V{version}__{snake_case_description}.sql
```

| Rule | Example |
| --- | --- |
| Version is integer, zero-padded optional | `V18`, `V19` |
| Double underscore before description | `V18__ecosystems.sql` |
| Never change applied migrations in prod | Add `V19` instead |
| Idempotent manual re-runs | Prefer `IF NOT EXISTS` / `ON CONFLICT` in **new** scripts only |

---

## Migration catalog (V1–V18)

| Version | File | Purpose |
| --- | --- | --- |
| **V1** | `V1__extensions_and_enums.sql` | Extensions; enums `app_role`, `stock_movement_type`, `transaction_status` |
| **V2** | `V2__auth_helpers.sql` | Auth helper functions |
| **V3** | `V3__profiles_and_roles.sql` | `profiles`, `user_roles`, `handle_new_user` trigger, `bootstrap_assign_admin` |
| **V4** | `V4__warehouses.sql` | `warehouses` |
| **V5** | `V5__products.sql` | `products` (global SKU unique — **pre-ecosystem**) |
| **V6** | `V6__stock_movements.sql` | Immutable stock ledger |
| **V7** | `V7__inventory_balances_view.sql` | `inventory_balances`, `product_stock_summary` views |
| **V8** | `V8__audit_entries.sql` | Append-only audit log |
| **V9** | `V9__transactions.sql` | POS `transactions` |
| **V10** | `V10__rls_policies.sql` | RLS policies (role-based, **not tenant-scoped**) |
| **V11** | `V11__storage_product_images.sql` | Supabase Storage bucket policies |
| **V12** | `V12__seed_data.sql` | **Demo** warehouses/products + `seed_demo_activity()` — **global, single-tenant** |
| **V13** | `V13__rename_transaction_void_status.sql` | Enum label `void` → `void_` |
| **V14** | `V14__stock_movement_provider.sql` | `provider` on IN movements |
| **V15** | `V15__stock_movement_recipient.sql` | `recipient` on OUT movements |
| **V16** | `V16__user_delete_references.sql` | Nullable `user_id` on audit/movements; ON DELETE SET NULL |
| **V17** | `V17__audit_entity_label.sql` | `entity_label` on audit; backfill user names |
| **V18** | `V18__workspace_settings.sql` | Singleton workspace settings (tax, receipts, business mode) + RLS |

---

## Planned migrations (ecosystem isolation)

See [`docs/TASK_05_ecosystems.md`](../../../../docs/TASK_05_ecosystems.md).

| Version | Purpose |
| --- | --- |
| **V19** | `ecosystems` table |
| **V20** | Add nullable `ecosystem_id` to tenant tables (incl. `workspace_settings`) |
| **V21** | Backfill demo ecosystem for existing Alex/seed data |
| **V22** | NOT NULL + FKs + per-ecosystem unique indexes (e.g. SKU) |
| **V23** | RLS + view updates for ecosystem scope |

Until V19–V23 ship, **public sign-up on a shared database exposes all tenants' data** to every admin.

---

## Manual SQL editor (Supabase)

Use the dashboard when:

- Flyway has already applied a migration and you only need a **subset** (e.g. backfill `UPDATE`)
- You are repairing a dev project after a partial manual run

| Situation | What to do |
| --- | --- |
| `column already exists` | Column is applied — **skip** the `ALTER TABLE`; run remaining statements only |
| Migration failed mid-file | Fix data/conflict, then run `flyway repair` + restart backend, or complete remaining SQL manually and mark version in `flyway_schema_history` (dev only) |
| Re-run V17 backfill | Safe — only updates rows where `entity_label IS NULL` |

Example — V17 backfill only (column already present):

```sql
UPDATE public.audit_entries ae
SET entity_label = p.name
FROM public.profiles p
WHERE ae.entity = 'user'
  AND ae.entity_id = p.id
  AND ae.entity_label IS NULL
  AND p.name IS NOT NULL
  AND trim(p.name) <> '';
```

**Do not** paste full migration files into SQL Editor if Flyway already applied them.

---

## Demo seed vs production

| Source | What it inserts | Tenant scope today |
| --- | --- | --- |
| **V12** (Flyway) | Warehouses, products, `seed_demo_activity()` | **Global** — visible to all admins |
| **`scripts/seed-auth-users.ps1`** | Auth users, roles, demo activity | **Global** |
| **Owner sign-up** | Auth user + admin role | No inventory rows — but **can read** V12 global data |

After ecosystem migrations:

- Demo data → fixed **demo ecosystem id**
- New sign-ups → new ecosystem, empty inventory

Consider moving V12 business seed out of Flyway into scripts only for production deployments.

---

## Checksums and editing files

Flyway stores a checksum per applied migration in `flyway_schema_history`. Editing an **already applied** file causes startup validation errors. Options:

1. **Preferred:** add a new `V{n+1}__...sql` migration.
2. **Dev only:** `flyway.repair()` on startup (already configured) updates checksums after intentional edits — use sparingly.

---

## Related scripts

| Script | Purpose |
| --- | --- |
| [`backend/scripts/seed-auth-users.ps1`](../../../../scripts/seed-auth-users.ps1) | Dev users + roles (update for `ecosystem_id` when V18+ lands) |
| [`backend/scripts/seed-activity-only.sql`](../../../../scripts/seed-activity-only.sql) | Demo movements/audit/transactions |
| [`backend/scripts/verify-database.sql`](../../../../scripts/verify-database.sql) | Post-seed validation |

---

## Adding a new migration

1. Create `V{n}__description.sql` in this folder (next integer after latest).
2. Mirror structural changes in `backend/src/test/resources/schema-h2.sql`.
3. Run `./gradlew test` and local `bootRun` against Supabase.
4. Document the version in this README and [`docs/DATABASE_SCHEMA.md`](../../../../docs/DATABASE_SCHEMA.md).
5. If behavior affects API or sign-up, update [`backend/API.md`](../../../../API.md) and the relevant TASK doc.
