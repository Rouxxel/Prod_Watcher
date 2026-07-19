# Flyway migrations — ProdWatch

This directory contains the **V1–V14 squashed migration chain** for a new, empty (greenfield) database. It reproduces the post-V26 schema: multi-tenant ecosystems, final RLS, storage path isolation, and demo seed data.

**Schema reference:** [`docs/DATABASE_SCHEMA.md`](../../../../docs/DATABASE_SCHEMA.md)  
**Backend setup:** [`backend/README.md`](../../../../README.md)  
**Seed scripts:** [`backend/scripts/README.md`](../../../../scripts/README.md)

## Critical legacy-database boundary

Do **not** point this build at a database whose `flyway_schema_history` contains the former V1–V26 chain. The V1–V14 scripts reuse version numbers with different descriptions and checksums, and backend startup runs `flyway.repair()` before migration.

Existing local, staging, and production databases on the legacy chain must remain on a pre-squash release or branch. Do not run `bootRun`, deploy this build, run repair/baseline, or manually alter Flyway history against them. Moving existing data to a squashed environment is a separate, explicitly authorized export/import and cutover project.

The historical files are retained at [`../legacy_sqls/`](../legacy_sqls/). That archive is forensic reference only and is not a Flyway location.

## How migrations run

On a new empty database, backend startup runs Flyway `repair()` then `migrate()`. The scripts in this folder execute once, in numeric order; JPA uses `ddl-auto=validate`, so Flyway owns the schema.

Tests use the H2 schema in `backend/src/test/resources/schema-h2.sql`, not Flyway. Keep it aligned with structural migration changes.

## Active migration catalog (V1–V14)

| Version | File | Purpose |
| --- | --- | --- |
| V1 | `V1__extensions_and_enums.sql` | Extensions and final enum values (`transaction_status` includes `void_`) |
| V2 | `V2__auth_helpers.sql` | Authentication/RLS helper functions |
| V3 | `V3__ecosystems.sql` | `ecosystems` table and update trigger |
| V4 | `V4__profiles_and_roles.sql` | Profiles, roles, auth trigger, bootstrap helper; nullable profile ecosystem |
| V5 | `V5__warehouses.sql` | Ecosystem-scoped warehouses |
| V6 | `V6__products.sql` | Ecosystem-scoped products and per-ecosystem SKU uniqueness |
| V7 | `V7__stock_movements.sql` | Immutable ecosystem stock ledger with `provider` and `recipient` rules |
| V8 | `V8__inventory_balances_view.sql` | Ecosystem-aware inventory views |
| V9 | `V9__audit_entries.sql` | Ecosystem audit trail with `entity_label` |
| V10 | `V10__transactions.sql` | Ecosystem-scoped POS transactions |
| V11 | `V11__workspace_settings.sql` | Per-ecosystem workspace settings |
| V12 | `V12__rls_policies.sql` | Ecosystem helper, final table RLS, and view security |
| V13 | `V13__storage_product_images.sql` | Product-images bucket and ecosystem-prefixed storage policies |
| V14 | `V14__seed_data.sql` | Acme Demo ecosystem, catalog/settings, and demo activity seed |

**Demo ecosystem id:** `33333333-3333-4333-8333-333333333301` (`Acme Demo`). `V14` is part of the greenfield chain; production deployments may skip the separate auth-user seed script if demo users are not wanted.

## Adding a future migration

1. Add the next integer versioned script in this directory.
2. Mirror structural changes in `backend/src/test/resources/schema-h2.sql`.
3. Update this catalog and [`docs/DATABASE_SCHEMA.md`](../../../../docs/DATABASE_SCHEMA.md).
4. Test only against a new/disposable environment when validating this squashed chain.

Never edit a migration already applied to an environment. Add a new migration instead.
