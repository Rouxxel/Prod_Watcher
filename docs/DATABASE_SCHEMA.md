# ProdWatch Database Schema

PostgreSQL schema on **Supabase** for the ProdWatch MVP (Phase 1: Inventory + POS schema stub).

Migrations live in `backend/src/main/resources/db/migration/` (Flyway). Frontend domain types: `frontend/src/types/index.ts`.

---

## ER diagram

```mermaid
erDiagram
    auth_users ||--|| profiles : "id"
    profiles ||--o{ user_roles : "user_id"
    profiles ||--o{ stock_movements : "user_id"
    profiles ||--o{ audit_entries : "user_id"
    profiles ||--o{ transactions : "cashier_id"

    warehouses ||--o{ products : "default_warehouse_id"
    warehouses ||--o{ stock_movements : "from_warehouse_id"
    warehouses ||--o{ stock_movements : "to_warehouse_id"

    products ||--o{ stock_movements : "product_id"

    auth_users {
        uuid id PK
        text email
    }

    profiles {
        uuid id PK_FK
        text name
        text email UK
        boolean active
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
        text name
        text location
        timestamptz created_at
        timestamptz updated_at
    }

    products {
        uuid id PK
        text name
        text sku UK
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
        uuid user_id FK
        text action
        text entity
        uuid entity_id
        text details
        timestamptz created_at
    }

    transactions {
        uuid id PK
        jsonb items
        numeric subtotal
        numeric tax
        numeric total
        uuid cashier_id FK
        transaction_status status
        timestamptz created_at
    }
```

**Views (computed, not stored):**

- `inventory_balances` — `(product_id, warehouse_id, quantity)` from `stock_movements`
- `product_stock_summary` — product stock at `default_warehouse_id` (feeds UI `Product.stock`)

**External (Supabase managed):**

- `auth.users` — login identities; trigger creates `profiles` on insert
- `storage.buckets` / `storage.objects` — `product-images` bucket (V12)

---

## Enums

| Postgres enum | Values | Frontend type |
| --- | --- | --- |
| `app_role` | `admin`, `warehouse_worker`, `warehouse_manager`, `inspector`, `cashier` | `Role` |
| `stock_movement_type` | `IN`, `OUT`, `TRANSFER`, `ADJUSTMENT` | `StockMovementType` |
| `transaction_status` | `completed`, `refunded`, `void` | `TransactionStatus` |

---

## Tables

### `profiles`

App user profile; 1:1 with `auth.users`.

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `uuid` | PK, FK → `auth.users(id)` ON DELETE CASCADE |
| `name` | `text` | NOT NULL |
| `email` | `text` | NOT NULL, UNIQUE |
| `active` | `boolean` | NOT NULL, DEFAULT `true` |
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
| `name` | `text` | NOT NULL |
| `location` | `text` | NOT NULL |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |
| `updated_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |

**Index:** `name`

### `products`

No `stock` column — quantity is derived from movements.

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `uuid` | PK, DEFAULT `gen_random_uuid()` |
| `name` | `text` | NOT NULL |
| `sku` | `text` | NOT NULL, UNIQUE |
| `category` | `text` | NOT NULL |
| `price` | `numeric(12,2)` | NOT NULL, CHECK `>= 0` |
| `default_warehouse_id` | `uuid` | NOT NULL, FK → `warehouses(id)` |
| `low_stock_threshold` | `integer` | NOT NULL, DEFAULT `0`, CHECK `>= 0` |
| `images` | `text[]` | NOT NULL, DEFAULT `'{}'` |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |
| `updated_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |

**Indexes:** `sku`, `category`, `default_warehouse_id`

### `stock_movements`

Source of truth for inventory. `qty` is always **positive**; direction is implied by `type` and warehouse columns.

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `uuid` | PK, DEFAULT `gen_random_uuid()` |
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
| `user_id` | `uuid` | NOT NULL, FK → `profiles(id)` |
| `action` | `text` | NOT NULL |
| `entity` | `text` | NOT NULL |
| `entity_id` | `uuid` | NOT NULL |
| `details` | `text` | nullable |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |

**Indexes:** `entity`, `entity_id`, `created_at`, `user_id`

### `transactions`

POS sales (schema only until TASK_02 Phase 2 wiring).

| Column | Type | Constraints |
| --- | --- | --- |
| `id` | `uuid` | PK, DEFAULT `gen_random_uuid()` |
| `items` | `jsonb` | NOT NULL, must be JSON array |
| `subtotal` | `numeric(12,2)` | NOT NULL, CHECK `>= 0` |
| `tax` | `numeric(12,2)` | NOT NULL, CHECK `>= 0` |
| `total` | `numeric(12,2)` | NOT NULL, CHECK `>= 0` |
| `cashier_id` | `uuid` | NOT NULL, FK → `profiles(id)` |
| `status` | `transaction_status` | NOT NULL, DEFAULT `'completed'` |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` |

**`items` element shape (CartItem):** `{ productId, name, sku, qty, unitPrice }`

**Indexes:** `cashier_id`, `created_at`, `status`

---

## Helper functions

| Function | Purpose |
| --- | --- |
| `has_role(user_id, role)` | SECURITY DEFINER — checks `user_roles` (used in RLS) |
| `current_user_role()` | Returns caller's primary role via `auth.uid()` |
| `is_active_user(user_id)` | Returns `profiles.active` (false if no profile) |
| `bootstrap_assign_admin(user_id)` | Inserts `admin` role if none exists (owner bootstrap) |
| `handle_new_user()` | Trigger fn: `auth.users` insert → `profiles` row |
| `seed_demo_activity()` | Idempotent dev seed for movements, audit, transactions |

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

RLS is enabled on all application tables. Policies apply to the `authenticated` role (Supabase JWT). The Java backend connects with a postgres/service role that **bypasses RLS** and enforces RBAC in controllers.

Inactive users (`profiles.active = false`) fail `is_active_user()` checks.

| Table | admin | warehouse_manager | warehouse_worker | inspector | cashier |
| --- | --- | --- | --- | --- | --- |
| **profiles** | SELECT all, UPDATE all | SELECT own | SELECT own | SELECT own | SELECT own |
| **user_roles** | SELECT/INSERT/UPDATE/DELETE | SELECT own | SELECT own | SELECT own | SELECT own |
| **warehouses** | SELECT, INSERT, UPDATE, DELETE | SELECT, INSERT, UPDATE, DELETE | SELECT | SELECT | SELECT |
| **products** | SELECT, INSERT, UPDATE, DELETE | SELECT, INSERT, UPDATE | SELECT, INSERT, UPDATE | SELECT | SELECT |
| **stock_movements** | SELECT, INSERT | SELECT, INSERT | SELECT, INSERT | SELECT | — |
| **audit_entries** | SELECT | SELECT | SELECT | SELECT | SELECT |
| **transactions** | SELECT, INSERT | SELECT | — | — | SELECT, INSERT |

**Write notes:**

- `audit_entries`: no client INSERT — backend/service role only
- `stock_movements`: no UPDATE/DELETE policies — append-only ledger
- **Storage** (`product-images`): public read; admin/manager/worker write (V12)

---

## Frontend ↔ Postgres mapping

| Frontend (`types/index.ts`) | Database |
| --- | --- |
| `User.id` | `profiles.id` (= `auth.users.id`) |
| `User.name` | `profiles.name` |
| `User.email` | `profiles.email` |
| `User.role` | `user_roles.role` (primary role) |
| `User.active` | `profiles.active` |
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
| V12 | `V12__seed_data.sql` | Reference seed + `seed_demo_activity()` (returns jsonb) |

---

## Related docs

- [`TASK_01_database.md`](TASK_01_database.md) — implementation plan
- [`SUPABASE_SETUP.md`](SUPABASE_SETUP.md) — Auth & Storage dashboard setup
- [`TASK_02_backend.md`](TASK_02_backend.md) — JPA entities, datasource, API
