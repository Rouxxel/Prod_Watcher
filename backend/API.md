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

Obtain a token via `POST /api/v1/auth/login` or `POST /api/v1/auth/confirm-email`. The backend validates the JWT with `SUPABASE_JWT_SECRET`, loads the user from `profiles` + `user_roles`, and rejects inactive accounts.

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
| 401 | Missing/invalid JWT (Spring Security) |
| 403 | Wrong role or inactive user |
| 404 | Entity not found |
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
| Transactions | CRUD + refund/void | R | — | — | checkout + R |

Legend: **C** create, **R** read, **U** update, **D** delete.

---

## Auth

### GET `/api/v1/auth/bootstrap-status`

Public. Returns whether owner sign-up is allowed (no admin exists yet).

**Response 200**

```json
{ "signupAllowed": true }
```

---

### POST `/api/v1/auth/signup`

Public. Owner bootstrap only (when `signupAllowed` is true).

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
    "emailConfirmed": true
  }
}
```

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

**Query:** `category`, `search`, `lowStock` (optional).

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

`stock` is computed from stock movements at the product's default warehouse.

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

| Type | Required warehouses | Effect |
| --- | --- | --- |
| `IN` | `toWarehouseId` | Increases stock |
| `OUT` | `fromWarehouseId` | Decreases stock (409 if insufficient) |
| `TRANSFER` | both (must differ) | Moves qty between warehouses |
| `ADJUSTMENT` | exactly one of from/to | Increase or decrease |

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
    "userId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3",
    "userName": "Jordan Lee",
    "timestamp": "2026-07-08T10:00:00Z",
    "note": "Initial receipt"
  }
]
```

---

### GET `/api/v1/stock-movements/{id}`

**Roles:** any authenticated user.

---

### POST `/api/v1/stock-movements`

**Roles:** admin, warehouse_manager, warehouse_worker.

**Body**

```json
{
  "type": "IN",
  "productId": "22222222-2222-4222-8222-222222222201",
  "qty": 10,
  "fromWarehouseId": null,
  "toWarehouseId": "11111111-1111-4111-8111-111111111101",
  "note": "Restock"
}
```

**Response 201** — `StockMovementResponse`.

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
    "action": "PRODUCT_CREATED",
    "entity": "product",
    "entityId": "22222222-2222-4222-8222-222222222201",
    "timestamp": "2026-07-08T10:05:00Z",
    "details": "KTL-001"
  }
]
```

---

### GET `/api/v1/audit/{id}`

**Roles:** any authenticated user.

---

## Users

Admin-only except `GET /users/me`.

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

**Roles:** admin. Cannot deactivate the last admin.

---

### POST `/api/v1/users/{id}/reactivate`

**Roles:** admin.

---

## Transactions (POS)

Tax rate defaults to **16%** (`POS_TAX_RATE=0.16`). Server validates line prices against product catalog and recalculates tax/total.

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
      "unitPrice": 54.50
    }
  ],
  "subtotal": 109.00,
  "tax": 17.44,
  "total": 126.44,
  "cashierId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa4",
  "status": "completed",
  "timestamp": "2026-07-08T12:00:00Z"
}
```

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
      "unitPrice": 54.50
    }
  ],
  "subtotal": 109.00,
  "tax": 17.44,
  "total": 126.44
}
```

**Response 201** — `TransactionResponse`. Returns **409** on oversell.

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
