# ProdWatch Database Schema

PostgreSQL schema on **Supabase** for ProdWatch inventory + POS. **Multi-tenant:** business data is scoped by `ecosystem_id`.

Migrations live in `backend/src/main/resources/db/migration/` (Flyway V1–V26). Frontend domain types: `frontend/src/types/index.ts`.

**Demo ecosystem id:** `33333333-3333-4333-8333-333333333301` (`Acme Demo`).

---

## ER diagram

```mermaid
erDiagram
    ecosystems ||--o{ profiles : "ecosystem_id"
    ecosystems ||--o{ warehouses : "ecosystem_id"
    ecosystems ||--o{ products : "ecosystem_id"
    ecosystems ||--o{ stock_movements : "ecosystem_id"
    ecosystems ||--o{ audit_entries : "ecosystem_id"
    ecosystems ||--o{ transactions : "ecosystem_id"
    ecosystems ||--o| workspace_settings : "ecosystem_id"

    auth_users ||--|| profiles : "id"
    profiles ||--o{ user_roles : "user_id"
    profiles ||--o{ stock_movements : "user_id"
    profiles ||--o{ audit_entries : "user_id"
    profiles ||--o{ transactions : "cashier_id"
    profiles ||--o| workspace_settings : "updated_by"

    warehouses ||--o{ products : "default_warehouse_id"
    warehouses ||--o{ stock_movements : "from_warehouse_id"
    warehouses ||--o{ stock_movements : "to_warehouse_id"

    products ||--o{ stock_movements : "product_id"

    ecosystems {
        uuid id PK
        text name
        timestamptz created_at
        timestamptz updated_at
    }

    auth_users {
        uuid id PK
        text email
    }

    profiles {
        uuid id PK_FK
        text name
        text email UK
        boolean active
        uuid ecosystem_id FK
        timestamptz created_at
        timestamptz updated_at
    }

    user_roles {
        uuid id PK
        uuid user_id FK
        app_role role
    }

    warehouses {
        uuid id PK
        uuid ecosystem_id FK
        text name
        text location
        timestamptz created_at
        timestamptz updated_at
    }

    products {
        uuid id PK
        uuid ecosystem_id FK
        text name
        text sku
        text category
        numeric price
        uuid default_warehouse_id FK
        integer low_stock_threshold
        text_array images
        timestamptz created_at
        timestamptz updated_at
    }

    stock_movements {
        uuid id PK
        uuid ecosystem_id FK
        stock_movement_type type
        uuid product_id FK
        integer qty
        uuid from_warehouse_id FK
        uuid to_warehouse_id FK
        uuid user_id FK
        text note
        timestamptz created_at
    }

    audit_entries {
        uuid id PK
        uuid ecosystem_id FK
        uuid user_id FK
        text action
        text entity
        uuid entity_id
        text details
        timestamptz created_at
    }

    transactions {
        uuid id PK
        uuid ecosystem_id FK
        jsonb items
        numeric subtotal
        numeric tax
        numeric total
        uuid cashier_id FK
        transaction_status status
        timestamptz created_at
    }

    workspace_settings {
        uuid id PK
        uuid ecosystem_id FK UK
        text business_name
        text contact_email
        numeric tax_rate
        text tax_label
        text receipt_footer
        text receipt_logo_url
        text business_mode
        timestamptz updated_at
        uuid updated_by FK
    }
```

**Uniqueness (per ecosystem):** `UNIQUE (ecosystem_id, sku)` on `products`; `UNIQUE (ecosystem_id)` on `workspace_settings`. Email remains globally unique on `profiles`.

**Views (computed, ecosystem-aware after V23):**

- `inventory_balances` — `(ecosystem_id, product_id, warehouse_id, quantity)` from `stock_movements`
- `product_stock_summary` — product stock at `default_warehouse_id` within matching ecosystem

**External (Supabase managed):**

- `auth.users` — login identities; trigger creates `profiles` on insert (`ecosystem_id` null until bootstrap)
- `storage.buckets` / `storage.objects` — `product-images` bucket (`V11`); writes scoped by path prefix (`V26`)

---

## Enums

| Postgres enum | Values | Frontend type |
| --- | --- | --- |
| `app_role` | `admin`, `warehouse_worker`, `warehouse_manager`, `inspector`, `cashier` | `Role` |
| `stock_movement_type` | `IN`, `OUT`, `TRANSFER`, `ADJUSTMENT` | `StockMovementType` |
| `transaction_status` | `completed`, `refunded`, `void` | `TransactionStatus` |

---

## Tables

### `ecosystems`

One row per business / workspace (tenant).

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `uuid` | PK, DEFAULT `gen_random_uuid()` |
| `name` | `text` | NOT NULL, DEFAULT `''` |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |
| `updated_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |

### `profiles`

App user profile; 1:1 with `auth.users`.

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `uuid` | PK, FK → `auth.users(id)` ON DELETE CASCADE |
| `name` | `text` | NOT NULL |
| `email` | `text` | NOT NULL, UNIQUE (global) |
| `active` | `boolean` | NOT NULL, DEFAULT `true` |
| `ecosystem_id` | `uuid` | FK → `ecosystems(id)` — nullable until bootstrap (`V24`) |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |
| `updated_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |

**Note:** Role is **not** on this table — see `user_roles`.

### `user_roles`

Single primary role per user (MVP).

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `uuid` | PK, DEFAULT `gen_random_uuid()` |
| `user_id` | `uuid` | NOT NULL, FK → `profiles(id)` ON DELETE CASCADE |
| `role` | `app_role` | NOT NULL |

**Unique:** `(user_id, role)`

### `warehouses`

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `uuid` | PK, DEFAULT `gen_random_uuid()` |
| `ecosystem_id` | `uuid` | NOT NULL, FK → `ecosystems(id)` |
| `name` | `text` | NOT NULL |
| `location` | `text` | NOT NULL |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |
| `updated_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |

**Index:** `ecosystem_id`, `name`

### `products`

No `stock` column — quantity is derived from movements.

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `uuid` | PK, DEFAULT `gen_random_uuid()` |
| `ecosystem_id` | `uuid` | NOT NULL, FK → `ecosystems(id)` |
| `name` | `text` | NOT NULL |
| `sku` | `text` | NOT NULL |
| `category` | `text` | NOT NULL |
| `price` | `numeric(12,2)` | NOT NULL, CHECK `>= 0` |
| `default_warehouse_id` | `uuid` | NOT NULL, FK → `warehouses(id)` |
| `low_stock_threshold` | `integer` | NOT NULL, DEFAULT `0`, CHECK `>= 0` |
| `images` | `text[]` | NOT NULL, DEFAULT `'{}'` |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |
| `updated_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |

**Unique:** `(ecosystem_id, sku)` — SKU unique per tenant, not globally.

**Indexes:** `ecosystem_id`, `sku`, `category`, `default_warehouse_id`

### `stock_movements`

Source of truth for inventory. `qty` is always **positive**; direction is implied by `type` and warehouse columns.

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `uuid` | PK, DEFAULT `gen_random_uuid()` |
| `ecosystem_id` | `uuid` | NOT NULL, FK → `ecosystems(id)` |
| `type` | `stock_movement_type` | NOT NULL |
| `product_id` | `uuid` | NOT NULL, FK → `products(id)` |
| `qty` | `integer` | NOT NULL, CHECK `> 0` |
| `from_warehouse_id` | `uuid` | FK → `warehouses(id)`, nullable |
| `to_warehouse_id` | `uuid` | FK → `warehouses(id)`, nullable |
| `user_id` | `uuid` | NOT NULL, FK → `profiles(id)` |
| `note` | `text` | nullable |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |

**Type rules (`stock_movements_type_warehouses_check`):**

| Type | `from_warehouse_id` | `to_warehouse_id` |
| --- | --- | --- |
| `IN` | NULL | required |
| `OUT` | required | NULL |
| `TRANSFER` | required | required (must differ) |
| `ADJUSTMENT` (decrease) | required | NULL |
| `ADJUSTMENT` (increase) | NULL | required |

**Indexes:** `product_id`, `from_warehouse_id`, `to_warehouse_id`, `created_at`, `user_id`

### `audit_entries`

Append-only audit trail.

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `uuid` | PK, DEFAULT `gen_random_uuid()` |
| `ecosystem_id` | `uuid` | NOT NULL, FK → `ecosystems(id)` |
| `user_id` | `uuid` | NOT NULL, FK → `profiles(id)` |
| `action` | `text` | NOT NULL |
| `entity` | `text` | NOT NULL |
| `entity_id` | `uuid` | NOT NULL |
| `details` | `text` | nullable |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |

**Indexes:** `entity`, `entity_id`, `created_at`, `user_id`

### `transactions`

POS sales (schema only).

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `uuid` | PK, DEFAULT `gen_random_uuid()` |
| `ecosystem_id` | `uuid` | NOT NULL, FK → `ecosystems(id)` |
| `items` | `jsonb` | NOT NULL, must be JSON array |
| `subtotal` | `numeric(12,2)` | NOT NULL, CHECK `>= 0` |
| `tax` | `numeric(12,2)` | NOT NULL, CHECK `>= 0` |
| `total` | `numeric(12,2)` | NOT NULL, CHECK `>= 0` |
| `cashier_id` | `uuid` | NOT NULL, FK → `profiles(id)` |
| `status` | `transaction_status` | NOT NULL, DEFAULT `'completed'` |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |

**`items` element shape (CartItem):** `{ productId, name, sku, qty, unitPrice }`

**Indexes:** `cashier_id`, `created_at`, `status`

### `workspace_settings`

Per-ecosystem workspace configuration.

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `uuid` | PK |
| `ecosystem_id` | `uuid` | NOT NULL, FK → `ecosystems(id)`, UNIQUE |
| `business_name` | `text` | NOT NULL, DEFAULT `''` |
| `contact_email` | `text` | NOT NULL, DEFAULT `''` |
| `tax_rate` | `numeric(6,4)` | NOT NULL, DEFAULT `0.16`, CHECK `0`–`1` |
| `tax_label` | `text` | NOT NULL, DEFAULT `'Tax'` |
| `receipt_footer` | `text` | nullable |
| `receipt_logo_url` | `text` | nullable |
| `business_mode` | `text` | NOT NULL, DEFAULT `'auto'`, CHECK `auto` \| `single` \| `multi` |
| `updated_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |
| `updated_by` | `uuid` | FK → `profiles(id)` ON DELETE SET NULL |

Demo row id `00000000-0000-4000-8000-000000000001` is tagged to Acme Demo in `V21`.

---

## Helper functions

| Function | Purpose |
| --- | --- |
| `has_role(user_id, role)` | SECURITY DEFINER — checks `user_roles` (used in RLS) |
| `current_user_role()` | Returns caller's primary role via `auth.uid()` |
| `is_active_user(user_id)` | Returns `profiles.active` (false if no profile) |
| `bootstrap_assign_admin(user_id)` | Inserts `admin` role if none exists (owner bootstrap) |
| `handle_new_user()` | Trigger fn: `auth.users` insert → `profiles` row |
| `current_user_ecosystem_id()` | Returns caller's `profiles.ecosystem_id` (V23; used in RLS + storage) |
| `seed_demo_activity()` | Idempotent dev seed for movements, audit, transactions (ecosystem-aware, V25) |

---

## How stock is computed

Stock is **never stored** on `products`. Current quantity per `(product_id, warehouse_id)` comes from the `inventory_balances` view:

```
quantity = SUM(deltas)
```

| Movement type | Warehouse credited | Delta |
| --- | --- | --- |
| `IN` | `to_warehouse_id` | `+qty` |
| `OUT` | `from_warehouse_id` | `-qty` |
| `TRANSFER` | `from_warehouse_id` | `-qty` |
| `TRANSFER` | `to_warehouse_id` | `+qty` |
| `ADJUSTMENT` | `from_warehouse_id` (decrease) | `-qty` |
| `ADJUSTMENT` | `to_warehouse_id` (increase) | `+qty` |

The UI field `Product.stock` maps to `product_stock_summary.quantity` — balance at the product's `default_warehouse_id` only.

Corrections use new `ADJUSTMENT` rows; movements are not deleted.

---

## RLS policy summary

RLS is enabled on application tables. Policies apply to the `authenticated` role (Supabase JWT). The Java backend connects with postgres/service role (**bypasses RLS**) and enforces **ecosystem_id + RBAC** in services.

Since **V23**, tenant tables use `ecosystem_id = current_user_ecosystem_id()` (plus role checks). Inactive users fail `is_active_user()`.

| Table | Scope | Notes |
| --- | --- | --- |
| **ecosystems** | Own ecosystem only | SELECT where `id = current_user_ecosystem_id()` |
| **profiles** | Own ecosystem | SELECT/UPDATE within tenant; email globally unique |
| **user_roles** | Own profile | Unchanged shape; implicit tenant via profile |
| **warehouses** | Per ecosystem | CRUD scoped by `ecosystem_id` |
| **products** | Per ecosystem | CRUD scoped; SKU unique per ecosystem |
| **stock_movements** | Per ecosystem | Append-only ledger |
| **audit_entries** | Per ecosystem | SELECT only for clients |
| **transactions** | Per ecosystem | POS scoped |
| **workspace_settings** | Per ecosystem | One row per `ecosystem_id` |

**Storage** (`product-images`, V26): public read; inventory-role **writes** require path prefix `split_part(name,'/',1) = current_user_ecosystem_id()::text`.

---

## Frontend ↔ Postgres mapping

| Frontend (`types/index.ts`) | Database |
| --- | --- |
| `User.id` | `profiles.id` (= `auth.users.id`) |
| `User.name` | `profiles.name` |
| `User.email` | `profiles.email` |
| `User.role` | `user_roles.role` (primary role) |
| `User.active` | `profiles.active` |
| `User.ecosystemId` | `profiles.ecosystem_id` / `ecosystems.id` |
| `User.ecosystemName` | `ecosystems.name` |
| `Warehouse.id` | `warehouses.id` |
| `Warehouse.name` | `warehouses.name` |
| `Warehouse.location` | `warehouses.location` |
| `Product.id` | `products.id` |
| `Product.name` | `products.name` |
| `Product.sku` | `products.sku` |
| `Product.category` | `products.category` |
| `Product.price` | `products.price` |
| `Product.warehouseId` | `products.default_warehouse_id` |
| `Product.stock` | `product_stock_summary.quantity` |
| `Product.lowStockThreshold` | `products.low_stock_threshold` |
| `Product.images` | `products.images` |
| `StockMovement.id` | `stock_movements.id` |
| `StockMovement.type` | `stock_movements.type` |
| `StockMovement.productId` | `stock_movements.product_id` |
| `StockMovement.qty` | `stock_movements.qty` (always positive in DB; decrease via type/warehouse) |
| `StockMovement.fromWarehouseId` | `stock_movements.from_warehouse_id` |
| `StockMovement.toWarehouseId` | `stock_movements.to_warehouse_id` |
| `StockMovement.userId` | `stock_movements.user_id` |
| `StockMovement.timestamp` | `stock_movements.created_at` |
| `StockMovement.note` | `stock_movements.note` |
| `AuditEntry.id` | `audit_entries.id` |
| `AuditEntry.userId` | `audit_entries.user_id` |
| `AuditEntry.action` | `audit_entries.action` |
| `AuditEntry.entity` | `audit_entries.entity` |
| `AuditEntry.entityId` | `audit_entries.entity_id` |
| `AuditEntry.timestamp` | `audit_entries.created_at` |
| `AuditEntry.details` | `audit_entries.details` |
| `Transaction.id` | `transactions.id` |
| `Transaction.items` | `transactions.items` (jsonb) |
| `Transaction.subtotal` | `transactions.subtotal` |
| `Transaction.tax` | `transactions.tax` |
| `Transaction.total` | `transactions.total` |
| `Transaction.cashierId` | `transactions.cashier_id` |
| `Transaction.status` | `transactions.status` |
| `Transaction.timestamp` | `transactions.created_at` |
| `CartItem.productId` | `items[].productId` in jsonb |
| `CartItem.name` | `items[].name` |
| `CartItem.sku` | `items[].sku` |
| `CartItem.qty` | `items[].qty` |
| `CartItem.unitPrice` | `items[].unitPrice` |
| `WorkspaceSettings.businessName` | `workspace_settings.business_name` |
| `WorkspaceSettings.contactEmail` | `workspace_settings.contact_email` |
| `WorkspaceSettings.taxRate` | `workspace_settings.tax_rate` |
| `WorkspaceSettings.taxLabel` | `workspace_settings.tax_label` |
| `WorkspaceSettings.receiptFooter` | `workspace_settings.receipt_footer` |
| `WorkspaceSettings.receiptLogoUrl` | `workspace_settings.receipt_logo_url` |
| `WorkspaceSettings.businessMode` | `workspace_settings.business_mode` |
| `WorkspaceSettings.updatedAt` | `workspace_settings.updated_at` |

---

## Migration index

| Version | File | Contents |
| --- | --- | --- |
| V1 | `V1__extensions_and_enums.sql` | Enum types |
| V2 | `V2__auth_helpers.sql` | `has_role`, `current_user_role`, `is_active_user` |
| V3 | `V3__profiles_and_roles.sql` | `profiles`, `user_roles`, auth trigger, bootstrap admin |
| V4 | `V4__warehouses.sql` | `warehouses` |
| V5 | `V5__products.sql` | `products` |
| V6 | `V6__stock_movements.sql` | `stock_movements` |
| V7 | `V7__inventory_balances_view.sql` | `inventory_balances`, `product_stock_summary` |
| V8 | `V8__audit_entries.sql` | `audit_entries` |
| V9 | `V9__transactions.sql` | `transactions` |
| V10 | `V10__rls_policies.sql` | RLS enable + policies |
| V11 | `V11__storage_product_images.sql` | `product-images` bucket + storage RLS |
| V12 | `V12__seed_data.sql` | Reference seed + `seed_demo_activity()` (demo ecosystem via V21) |
| V18 | `V18__workspace_settings.sql` | Workspace settings + RLS |
| V19 | `V19__ecosystems.sql` | `ecosystems` table |
| V20 | `V20__ecosystem_id_columns.sql` | Nullable `ecosystem_id` on tenant tables |
| V21 | `V21__ecosystem_backfill.sql` | Acme Demo ecosystem + backfill |
| V22 | `V22__ecosystem_constraints.sql` | NOT NULL, per-ecosystem unique indexes |
| V23 | `V23__ecosystem_rls_and_views.sql` | `current_user_ecosystem_id()`, RLS, views |
| V24 | `V24__profiles_ecosystem_nullable.sql` | Nullable profile ecosystem until bootstrap |
| V25 | `V25__seed_demo_activity_ecosystem.sql` | Ecosystem-aware activity seed |
| V26 | `V26__storage_ecosystem_paths.sql` | Storage write policies by path prefix |

Full catalog: `backend/src/main/resources/db/migration/README.md`.
