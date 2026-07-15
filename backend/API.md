# ProdWatch API Reference

Base URL: `http://localhost:8080` (local) or your Render service URL.

All business routes live under **`/api/v1`**. JSON field names are **camelCase** and match `frontend/src/types/index.ts`.

Interactive docs: **[Swagger UI](/docs)** (OpenAPI at `/api-docs`).

---

## Authentication

Protected routes require a Supabase access token:

```http
Authorization: Bearer <accessToken>
```

Obtain a token via `POST /api/v1/auth/login` or `POST /api/v1/auth/confirm-email`. The backend validates the JWT using Supabase **ES256/RS256** keys from `{SUPABASE_URL}/auth/v1/.well-known/jwks.json`, with optional **HS256** fallback via `SUPABASE_JWT_SECRET`. It loads the user from `profiles` + `user_roles`, requires a non-null `ecosystem_id`, and rejects inactive accounts.

### Multi-tenancy (ecosystems)

Each business owner gets an isolated **ecosystem** (tenant). All inventory, POS, audit, user-admin, and settings data is scoped by `ecosystem_id` on the server.

| Concept | Notes |
| --- | --- |
| **Ecosystem** | One row in `ecosystems` per business / workspace |
| **Membership** | `profiles.ecosystem_id` — one ecosystem per user (MVP) |
| **Sign-up** | Creates a **new** ecosystem (empty inventory) + `admin` role |
| **Staff provision** | Admin creates users in **their** ecosystem only |
| **List endpoints** | Return only the caller's ecosystem |
| **GET by UUID** | Cross-ecosystem id → **404** (not 403) |

Demo seed data (`seed-auth-users.ps1`) lives on the fixed **Acme Demo** ecosystem (`33333333-3333-4333-8333-333333333301`). New sign-ups never see it.

`UserResponse` includes `ecosystemId` and `ecosystemName` for display/debug. There is no tenant-switching API in MVP.

### Public routes (no JWT)

| Method | Path |
| --- | --- |
| GET | `/` |
| GET | `/api/v1/health` |
| GET | `/api/v1/auth/bootstrap-status` |
| POST | `/api/v1/auth/signup` |
| POST | `/api/v1/auth/login` |
| POST | `/api/v1/auth/confirm-email` |
| GET | `/docs`, `/actuator/**` |

All other `/api/v1/**` routes require authentication.

---

## Error responses

Errors use a uniform shape:

```json
{
  "status": 404,
  "error": "Not Found",
  "detail": "Product not found"
}
```

| HTTP | When |
| --- | --- |
| 400 | Bean validation failed (`field: message` in `detail`) |
| 401 | Missing/invalid JWT, or profile has no `ecosystem_id` yet |
| 403 | Wrong role |
| 404 | Entity not found, or resource exists in another ecosystem |
| 409 | Insufficient stock (`InsufficientStockException`) |
| 422 | Business rule violation (e.g. duplicate SKU, invalid movement) |
| 429 | Rate limit exceeded |
| 500 | Unexpected server error |

---

## Roles

| Role | Description |
| --- | --- |
| `admin` | Full access |
| `warehouse_manager` | Inventory CRUD (no product delete); read transactions |
| `warehouse_worker` | Products create/update; stock movements create |
| `inspector` | Read-only inventory + audit |
| `cashier` | Read products; checkout (POS) |

### RBAC matrix

| Group | admin | warehouse_manager | warehouse_worker | inspector | cashier |
| --- | --- | --- | --- | --- | --- |
| Products | CRUD | CRU | CU | R | R |
| Warehouses | CRUD | CRUD | R | R | R |
| Stock movements | CRU | CRU | CRU | R | R |
| Audit | R | R | R | R | R |
| Users | full | — | — | — | — |
| Settings | RU | R | R | R | R |
| Transactions | CRUD + refund/void | R | — | R | checkout + R |

Legend: **C** create, **R** read, **U** update, **D** delete.

Settings **U** (PATCH) is admin-only; all roles may **R** (GET) when authenticated. Unauthenticated clients receive **401** and never see `contactEmail` or other fields.

---

## Auth

### GET `/api/v1/auth/bootstrap-status`

Public. Legacy compatibility — always returns `signupAllowed: true` (public owner sign-up is enabled; each sign-up creates a new ecosystem).

**Response 200**

```json
{ "signupAllowed": true }
```

Staff accounts are provisioned by an admin on `/users` (cannot self-sign-up as cashier/worker).

---

### GET `/api/v1/auth/signup-email-available`

Public. Checks whether an email is globally available (auth identity is unique across the platform).

**Query:** `email` (required)

**Response 200**

```json
{ "available": true }
```

---

### POST `/api/v1/auth/signup`

Public. Creates a Supabase auth user and profile, then assigns a **new ecosystem** (empty workspace) and **admin** role. Does not copy demo seed data.

**Body**

```json
{
  "name": "Alex Owner",
  "email": "alex@acme.co",
  "password": "SecurePass123!"
}
```

**Response 200**

```json
{ "message": "Check your email to confirm your account" }
```

---

### POST `/api/v1/auth/login`

Public.

**Body**

```json
{
  "email": "jordan@acme.co",
  "password": "ProdWatchDev2024!"
}
```

**Response 200**

```json
{
  "accessToken": "eyJhbG...",
  "refreshToken": "v1.MR...",
  "expiresIn": 3600,
  "user": {
    "id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
    "name": "Jordan Lee",
    "email": "jordan@acme.co",
    "role": "warehouse_worker",
    "active": true,
    "emailConfirmed": true,
    "ecosystemId": "33333333-3333-4333-8333-333333333301",
    "ecosystemName": "Acme Demo"
  }
}
```

`ecosystemId` / `ecosystemName` identify the user's tenant. Omitted or null only before bootstrap completes (login/confirm-email runs bootstrap first).

---

### POST `/api/v1/auth/logout`

Optional JWT. Revokes session when `Authorization` header is present.

**Response 200** — empty body.

---

### POST `/api/v1/auth/confirm-email`

Public. Completes sign-up after email link; assigns admin if first user.

**Body**

```json
{ "token": "<signup_confirmation_token>" }
```

**Response 200** — same shape as login.

---

### GET `/api/v1/auth/me`

**Roles:** any authenticated user.

**Response 200** — `UserResponse` (see Users).

---

## Health

### GET `/api/v1/health`

Public.

**Response 200**

```json
{
  "status": "ok",
  "database": "connected"
}
```

---

## Products

### GET `/api/v1/products`

**Roles:** any authenticated user.

**Query:** `category`, `search`, `lowStock` (optional), `warehouseId` (optional UUID).

When **`warehouseId`** is omitted, `stock` is the on-hand quantity at the product's **default warehouse** (`warehouseId` field). When **`warehouseId`** is set, `stock` is the quantity **at that warehouse** (any location in the network). The `warehouseId` on each product row is always the catalog default, not the query filter.

**Response 200** — array of:

```json
{
  "id": "22222222-2222-4222-8222-222222222201",
  "name": "Ceramic Pour-Over Kettle",
  "sku": "KTL-001",
  "category": "Kitchen",
  "price": 78.00,
  "stock": 42,
  "warehouseId": "11111111-1111-4111-8111-111111111101",
  "lowStockThreshold": 10,
  "images": ["https://example.com/kettle.jpg"]
}
```

`stock` is computed from the `inventory_balances` view (ledger-derived, not a stored column).

---

### GET `/api/v1/products/{id}`

**Roles:** any authenticated user.

---

### POST `/api/v1/products`

**Roles:** admin, warehouse_manager, warehouse_worker.

**Body**

```json
{
  "name": "New Widget",
  "sku": "WDG-001",
  "category": "General",
  "price": 19.99,
  "warehouseId": "11111111-1111-4111-8111-111111111101",
  "lowStockThreshold": 5,
  "images": []
}
```

**Response 201** — `ProductResponse`.

Opening stock is **not** set on create; record an `IN` stock movement separately.

---

### PATCH `/api/v1/products/{id}`

**Roles:** admin, warehouse_manager, warehouse_worker.

Partial update — all fields optional in body (same fields as create).

---

### DELETE `/api/v1/products/{id}`

**Roles:** admin only.

**Response 204** — no body.

---

## Warehouses

### GET `/api/v1/warehouses`

**Roles:** any authenticated user.

**Response 200**

```json
[
  { "id": "11111111-1111-4111-8111-111111111101", "name": "Central Depot", "location": "Caracas, VE" }
]
```

---

### GET `/api/v1/warehouses/{id}`

**Roles:** any authenticated user.

---

### POST `/api/v1/warehouses`

**Roles:** admin, warehouse_manager.

**Body**

```json
{ "name": "South Annex", "location": "Building B" }
```

**Response 201** — `WarehouseResponse`.

---

### PATCH `/api/v1/warehouses/{id}`

**Roles:** admin, warehouse_manager.

---

### DELETE `/api/v1/warehouses/{id}`

**Roles:** admin, warehouse_manager.

Blocked if warehouse has stock, movements, or is a product default warehouse.

**Response 204**.

---

## Stock movements

Movements are **immutable** (no update/delete). Corrections use a new `ADJUSTMENT`.

| Type | Warehouses | Extra fields | Effect |
| --- | --- | --- | --- |
| `IN` | `toWarehouseId` required; `fromWarehouseId` null | **`provider`** required (non-empty string) — external supplier | Increases stock at destination |
| `OUT` | `fromWarehouseId` required; `toWarehouseId` null | **`recipient`** required (non-empty string) — customer / third party | Decreases stock (409 if insufficient) |
| `TRANSFER` | both (must differ) | `provider` and `recipient` must be null | Moves qty between warehouses |
| `ADJUSTMENT` | exactly one of from/to | `provider` and `recipient` must be null | Increase (to) or decrease (from) |

`provider` and `recipient` are free-text columns on `stock_movements` (not warehouse FKs). POS checkout creates `OUT` rows with `recipient = "POS customer"`.

### GET `/api/v1/stock-movements`

**Roles:** any authenticated user.

**Query:** `productId`, `warehouseId`, `type`, `from`, `to` (ISO-8601 datetimes).

**Response 200**

```json
[
  {
    "id": "33333333-3333-4333-8333-333333333301",
    "type": "IN",
    "productId": "22222222-2222-4222-8222-222222222201",
    "productName": "Ceramic Pour-Over Kettle",
    "qty": 50,
    "fromWarehouseId": null,
    "toWarehouseId": "11111111-1111-4111-8111-111111111101",
    "provider": "Acme Steel Co.",
    "recipient": null,
    "userId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3",
    "userName": "Jordan Lee",
    "timestamp": "2026-07-08T10:00:00Z",
    "note": "Initial receipt"
  },
  {
    "id": "33333333-3333-4333-8333-333333333302",
    "type": "OUT",
    "productId": "22222222-2222-4222-8222-222222222202",
    "productName": "Walnut Cutting Board",
    "qty": 2,
    "fromWarehouseId": "11111111-1111-4111-8111-111111111101",
    "toWarehouseId": null,
    "provider": null,
    "recipient": "BuildRight Contractors",
    "userId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3",
    "userName": "Jordan Lee",
    "timestamp": "2026-07-09T14:00:00Z",
    "note": null
  }
]
```

**UI mapping:** for `IN`, display `provider` in the "From" column; for `OUT`, display `recipient` in the "To" column; warehouse columns show internal warehouse names for the other side.

---

### GET `/api/v1/stock-movements/{id}`

**Roles:** any authenticated user.

---

### POST `/api/v1/stock-movements`

**Roles:** admin, warehouse_manager, warehouse_worker.

**Body (IN — receive from supplier)**

```json
{
  "type": "IN",
  "productId": "22222222-2222-4222-8222-222222222201",
  "qty": 10,
  "fromWarehouseId": null,
  "toWarehouseId": "11111111-1111-4111-8111-111111111101",
  "provider": "Acme Steel Co.",
  "recipient": null,
  "note": "Restock"
}
```

**Body (OUT — issue to customer)**

```json
{
  "type": "OUT",
  "productId": "22222222-2222-4222-8222-222222222202",
  "qty": 2,
  "fromWarehouseId": "11111111-1111-4111-8111-111111111101",
  "toWarehouseId": null,
  "provider": null,
  "recipient": "BuildRight Contractors",
  "note": null
}
```

**Body (ADJUSTMENT — add stock at one warehouse)**

```json
{
  "type": "ADJUSTMENT",
  "productId": "22222222-2222-4222-8222-222222222201",
  "qty": 5,
  "fromWarehouseId": null,
  "toWarehouseId": "11111111-1111-4111-8111-111111111101",
  "provider": null,
  "recipient": null,
  "note": "Cycle count found extra"
}
```

Returns **422** if `provider`/`recipient` are sent on the wrong type, missing when required, or if warehouse rules are violated.

**Response 201** — `StockMovementResponse` (same fields as list item above).

---

## Audit

Read-only append-only log.

### GET `/api/v1/audit`

**Roles:** any authenticated user.

**Query:** `entity`, `userId`, `from`, `to`.

**Response 200**

```json
[
  {
    "id": "44444444-4444-4444-8444-444444444401",
    "userId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
    "userName": "Alex Owner",
    "action": "PRODUCT_CREATED",
    "entity": "product",
    "entityId": "22222222-2222-4222-8222-222222222201",
    "entityLabel": "Ceramic Pour-Over Kettle",
    "timestamp": "2026-07-08T10:05:00Z",
    "details": "KTL-001"
  }
]
```

| Field | Description |
| --- | --- |
| `userName` | Profile name of the actor (from `profiles.name`) |
| `entityLabel` | Human-readable target resolved by entity type: product name, warehouse name, user name, movement's product name, transaction total (`$124.50`), etc. Falls back to `details` or a short id if the record was deleted |
| `details` | Free-text context stored at write time (often SKU, email, or change summary) |

---

### GET `/api/v1/audit/{id}`

**Roles:** any authenticated user.

---

## Users

Admin-only except `GET /users/me`.

### `UserResponse`

```json
{
  "id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
  "name": "Alex Reyes",
  "email": "alex@acme.co",
  "role": "admin",
  "active": true,
  "emailConfirmed": null,
  "ecosystemId": "33333333-3333-4333-8333-333333333301",
  "ecosystemName": "Acme Demo"
}
```

| Field | Notes |
| --- | --- |
| `emailConfirmed` | Set on login/confirm-email responses; usually `null` on `/users` routes |
| `ecosystemId` | Tenant UUID — all list/detail queries are scoped to this id |
| `ecosystemName` | Display label from `ecosystems.name` (e.g. `"{ownerName}'s workspace"`) |

Provisioning staff sets `ecosystemId` to the admin's ecosystem automatically.

### GET `/api/v1/users`

**Roles:** admin.

---

### GET `/api/v1/users/me`

**Roles:** any authenticated user.

---

### GET `/api/v1/users/{id}`

**Roles:** admin.

---

### POST `/api/v1/users`

**Roles:** admin. Provisions a confirmed user (no email flow).

**Body**

```json
{
  "email": "new.worker@acme.co",
  "name": "New Worker",
  "password": "TempPass123!",
  "role": "warehouse_worker",
  "active": true
}
```

`role` cannot be `admin`. User can log in immediately.

**Response 201** — `UserResponse`.

---

### PATCH `/api/v1/users/{id}`

**Roles:** admin. Update `active` and/or `role` (not to `admin`).

---

### POST `/api/v1/users/me/step-down-admin`

**Roles:** admin (self only). Demote your own account to a non-admin role when at least two admins are active.

**Body**

```json
{ "role": "warehouse_manager" }
```

**Response 200** — `UserResponse` with the new role.

---

### POST `/api/v1/users/{id}/promote-admin`

**Roles:** admin.

---

### POST `/api/v1/users/{id}/reset-password`

**Roles:** admin.

**Body**

```json
{ "newPassword": "NewSecurePass123!" }
```

**Response 204**.

---

### POST `/api/v1/users/{id}/deactivate`

**Roles:** admin. Cannot deactivate the last active admin.

---

### POST `/api/v1/users/{id}/reactivate`

**Roles:** admin.

---

### DELETE `/api/v1/users/{id}`

**Roles:** admin. Permanently removes a non-admin user (Supabase auth + profile + role). Cannot delete admins, yourself, or cashiers with POS transaction history.

**Response 204**.

---

## Settings

Workspace configuration **per ecosystem** (tax, receipts, business mode). Persisted in `workspace_settings` (one row per `ecosystem_id`).

| Method | Path | Roles | Description |
| --- | --- | --- | --- |
| GET | `/api/v1/settings` | authenticated | Workspace settings |
| PATCH | `/api/v1/settings` | admin | Partial update |

### GET `/api/v1/settings`

**Roles:** any authenticated user (cashiers need tax rate for cart/receipts). Unauthenticated requests → **401** (no settings payload, including `contactEmail`).

**Response 200**

```json
{
  "businessName": "ProdWatch Demo Co.",
  "contactEmail": "ops@prodwatch.app",
  "taxRate": 0.16,
  "taxLabel": "VAT",
  "receiptFooter": "Thank you for your purchase!",
  "receiptLogoUrl": null,
  "businessMode": "auto",
  "updatedAt": "2026-07-15T12:00:00Z"
}
```

| Field | Description |
| --- | --- |
| `taxRate` | Decimal fraction `0`–`1` (e.g. `0.16` = 16%) |
| `businessMode` | `auto` \| `single` \| `multi` — inventory/POS location UX preference |

### PATCH `/api/v1/settings`

**Roles:** admin.

**Body:** partial update — omitted/`null` fields are unchanged.

```json
{
  "taxRate": 0.10,
  "taxLabel": "GST"
}
```

**Validation:** `taxRate` 0–1; `contactEmail` valid email when set; `receiptLogoUrl` http/https URL when set; `businessMode` one of `auto`, `single`, `multi`.

**Response 200** — full `SettingsResponse` after save. Writes `SETTINGS_UPDATED` audit entry.

On first access per ecosystem, if no row exists the server bootstraps defaults using `POS_TAX_RATE` (env fallback, default `0.16`).

New sign-ups get a fresh settings row when they first open Settings or when POS reads tax rate.

---

## Transactions (POS)

Tax rate comes from **workspace settings** (`GET /api/v1/settings`). `POS_TAX_RATE` seeds the row on first bootstrap only. Server validates line prices against the product catalog and recalculates tax/total.

Checkout creates one **`OUT`** stock movement per line. Stock is deducted from **`warehouseId` on each cart item** when present; otherwise from `POS_WAREHOUSE_ID` (if configured) or the product's default warehouse. Each persisted line item stores `warehouseId` in transaction JSON so refunds/voids restore stock to the correct warehouse.

### GET `/api/v1/transactions`

**Roles:** admin, warehouse_manager, cashier.

**Query:** `cashierId`, `status` (`completed` \| `refunded` \| `void`), `from`, `to`.

---

### GET `/api/v1/transactions/{id}`

**Roles:** admin, warehouse_manager, cashier.

**Response 200**

```json
{
  "id": "55555555-5555-4555-8555-555555555501",
  "items": [
    {
      "productId": "22222222-2222-4222-8222-222222222202",
      "name": "Walnut Cutting Board",
      "sku": "WCB-220",
      "qty": 2,
      "unitPrice": 54.50,
      "warehouseId": "11111111-1111-4111-8111-111111111101"
    }
  ],
  "subtotal": 109.00,
  "tax": 17.44,
  "total": 126.44,
  "cashierId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa4",
  "cashierName": "Sam Rivera",
  "status": "completed",
  "timestamp": "2026-07-08T12:00:00Z"
}
```

| Field | Description |
| --- | --- |
| `cashierName` | Cashier profile name (display); `cashierId` remains the UUID FK |
| `items[].warehouseId` | Warehouse stock was drawn from; `null` on legacy rows (refund uses default/POS warehouse) |

---

### POST `/api/v1/transactions`

**Roles:** admin, cashier. Checkout — creates transaction + OUT movements per line.

**Body**

```json
{
  "items": [
    {
      "productId": "22222222-2222-4222-8222-222222222202",
      "name": "Walnut Cutting Board",
      "sku": "WCB-220",
      "qty": 2,
      "unitPrice": 54.50,
      "warehouseId": "11111111-1111-4111-8111-111111111101"
    },
    {
      "productId": "22222222-2222-4222-8222-222222222201",
      "name": "Ceramic Pour-Over Kettle",
      "sku": "KTL-001",
      "qty": 1,
      "unitPrice": 78.00,
      "warehouseId": "11111111-1111-4111-8111-111111111102"
    }
  ],
  "subtotal": 187.00,
  "tax": 29.92,
  "total": 216.92
}
```

A single checkout may include lines from **multiple warehouses**. `warehouseId` per line is optional; omit only when `POS_WAREHOUSE_ID` or default-warehouse fallback is intended.

**Response 201** — `TransactionResponse`. Returns **409** on oversell at the resolved warehouse for any line.

---

### POST `/api/v1/transactions/{id}/refund`

**Roles:** admin. Restores stock; sets status `refunded`.

---

### POST `/api/v1/transactions/{id}/void`

**Roles:** admin. Restores stock; sets status `void`.

---

## Rate limiting

Per-endpoint limits are defined in `src/main/resources/core_specs/configuration/config_file.json` (e.g. products: 60/min). Exceeded limits return **429**.

---

## Related docs

- [`README.md`](README.md) — setup, env vars, Docker, Render
- [`docs/DATABASE_SCHEMA.md`](../docs/DATABASE_SCHEMA.md) — Postgres schema reference
- [`docs/SUPABASE_SETUP.md`](../docs/SUPABASE_SETUP.md) — Supabase project configuration
- [`frontend/README.md`](../frontend/README.md) — frontend deploy and CORS
