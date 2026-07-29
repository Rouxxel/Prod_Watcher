# Redis cache TTL catalog — ProdWatch

Redis caches high-value read paths so repeated requests do not hit Postgres on every call. It is **off by default** (`REDIS_ENABLED=false`). Connection failures are logged; the API still serves traffic with `"redis": "unavailable"` on `GET /`.

TTLs are set in [`config_file.json`](config_file.json) under `redis_cache.ttl_seconds`. Java code reads them via [`ConfigLoader.java`](../../../../java/com/prodwatch/api/core_specs/configuration/ConfigLoader.java).

**Architecture:** controllers → services → `RedisCacheService` → Redis (optional). Postgres remains the source of truth.

---

## Cached entities

| Config key | Default TTL | Redis key pattern | What it caches | API / UI usage | Invalidate on |
| --- | --- | --- | --- | --- | --- |
| `profile` | 600 (10m) | `profile:{userId}` | Profile + role for auth | Every authenticated request (`JwtAuthFilter`) | User admin mutations |
| `settings` | 1800 (30m) | `settings:{ecosystemId}` | Workspace settings row | Checkout tax, Settings page, receipts | `PATCH /settings` |
| `warehouses_list` | 3600 (60m) | `warehouses:{ecosystemId}` | Full warehouse list | Product/cashier warehouse filters | Warehouse create/update/delete |
| `warehouse` | 3600 (60m) | `warehouse:{ecosystemId}:{id}` | Single warehouse | Warehouse detail | Warehouse update/delete |
| `product_meta` | 1800 (30m) | `product:meta:{ecosystemId}:{id}` | Product fields **without** stock | `GET /products/{id}` metadata portion | Product create/update/delete |
| `ecosystem` | 21600 (6h) | `ecosystem:{ecosystemId}` | Ecosystem display name | Login, `/users/me` | Ecosystem rename (future) |
| `movement` | 86400 (24h) | `movement:{id}` | Immutable stock movement | Movement detail | Never |
| `audit` | 86400 (24h) | `audit:{id}` | Immutable audit entry | Audit detail | Never |
| `transaction` | 86400 (24h) | `txn:{id}` | Transaction / receipt body | Receipt reprint, txn detail | Refund / void |
| `stock_display` | 15 (15s) | `stock:{ecosystemId}:{productId}:{warehouseId}` | Display-only stock qty | Dashboard badges (optional) | Movement/checkout/refund touching product+warehouse |

---

## Explicitly not cached

| Data | Reason |
| --- | --- |
| Product / movement / audit / transaction **list** endpoints | Filter permutations + frequent writes |
| **Checkout oversell check** | Must be live and inside the DB transaction (`409 Insufficient stock`) |
| **Rate limiting** | Still in-memory per instance (`RateLimiter`); distributed limiter is a separate task |

---

## Environment

| Variable | Default | Purpose |
| --- | --- | --- |
| `REDIS_ENABLED` | `false` | Master switch |
| `REDIS_HOST` | `localhost` | Hostname (`redis` in Docker Compose) |
| `REDIS_PORT` | `6379` | Port (Redis Cloud may differ) |
| `REDIS_PASSWORD` | empty | Required for Redis Cloud |
| `REDIS_DB` | `0` | Database index |
| `REDIS_TLS` | `false` | Set `true` for Redis Cloud |

See [`backend/.env.example`](../../../../../../.env.example) and [`backend/README.md`](../../../../../../README.md).
