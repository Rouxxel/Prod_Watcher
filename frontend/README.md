# ProdWatch — Inventory & POS Frontend

ProdWatch is the **React frontend** for an inventory and point-of-sale system.
It talks to the **Java Spring Boot API** (`backend/`) for auth, data, and transactions.
Product image uploads use **Supabase Storage** directly from the browser (optional env vars).

---

## Features

### Auth & shell

- **Backend auth** — `/login` and owner `/signup` bootstrap via `POST /api/v1/auth/*`
- **Two operating modes** — `inventory` and `selling`, switchable from the topbar
- **Role-based sidebar** — menu items filter by the logged-in user's role
- **Global API errors** — toasts for 401/403/409/429/network; dev console logging via `error-capture.ts`

### Inventory mode

- **Dashboard** — KPIs, recent movements, low-stock highlights
- **Products** — CRUD, search/category/stock-level filters, **warehouse filter** (stock shown at selected warehouse)
- **Warehouses** — CRUD, per-warehouse product summary
- **Stock movements** — record IN / OUT / TRANSFER / ADJUSTMENT
  - **IN** — required **Provider** (external supplier); **To** = destination warehouse
  - **OUT** — **From** = source warehouse; required **Recipient** (customer / third party)
  - **ADJUSTMENT** — single warehouse + **Add stock** / **Remove stock** (not two warehouses)
  - **TRANSFER** — from / to internal warehouses
- **Audit** — immutable log with **user names** and **entity labels** (product name, warehouse, etc.)

### Selling mode (POS)

- **Cashier** — search/scan products with **warehouse filter** (stock at selected location)
- **Multi-warehouse cart** — switch warehouse, add items; each line stores `warehouseId` and warehouse name
- **Cart** — review lines before checkout
- **Transactions** — history with **cashier names** (not UUIDs)
- **Receipt** dialog after successful checkout

### Admin

- User provisioning, password reset, Settings (static MVP placeholders)

### Other

- **Product images** — URL list and optional file upload to Supabase `product-images` bucket
- **Dark-only theme** with burgundy/vinotinto tokens in `src/styles.css`

---

## Tech stack

| Concern        | Choice                                                |
| -------------- | ----------------------------------------------------- |
| Framework      | TanStack Start v1 (React 19, Vite 7)                  |
| Routing        | TanStack Router (file-based, `src/routes/`)           |
| Data layer     | TanStack Query + `src/services/*` → Java REST API     |
| UI             | shadcn/ui (Radix), Tailwind CSS v4                    |
| Toasts         | sonner (`src/lib/notify.ts`)                          |
| Deploy         | Vercel (primary, `vercel.json`); Cloudflare Workers optional |

---

## Running locally

**Ports:** frontend **8000**, backend **8080**.

```bash
# Terminal 1 — backend (from backend/)
./gradlew bootRun   # or start.bat / start.sh

# Terminal 2 — frontend (from frontend/)
npm install
npm run dev         # http://localhost:8000
```

### Environment (`frontend/.env`)

```bash
VITE_API_BASE_URL=http://localhost:8080/api/v1

# Optional — product image uploads to Supabase Storage
VITE_SUPABASE_URL=https://your-project.supabase.co
VITE_SUPABASE_ANON_KEY=your-anon-key
```

Copy Supabase values from `backend/.env`. Ensure backend `CORS_ALLOWED_ORIGINS`
includes `http://localhost:8000`.

### Scripts

```bash
npm run dev       # dev server (port 8000)
npm run build     # production build
npm run lint      # ESLint
```

---

## Project structure

```text
src/
  routes/              # File-based routes (_app.* layout, login, signup)
  components/          # Layout, products, movements, POS, users, ui
  hooks/
    use-current-user.tsx   # Auth context (login/logout, GET /users/me)
    queries.ts             # TanStack Query hooks (products accept warehouseId)
    use-cart.tsx           # POS cart — per-warehouse lines + checkout
  services/            # HTTP clients → Java API (api.ts, *.service.ts)
  lib/
    auth-token.ts      # sessionStorage JWT
    api-error.ts       # Toast mapping for API errors
    error-capture.ts   # Dev console logging
    storage.ts         # Supabase Storage uploads (optional)
  types/index.ts       # Shared domain types
```

---

## API integration

All data flows through `src/services/` using `apiGet` / `apiPost` / `apiPatch` /
`apiDelete` from `src/services/api.ts`:

- Attaches `Authorization: Bearer <token>` from `auth-token.ts`
- **401** → clears token, redirects to `/login`
- Errors surface via toasts (`toastApiError`) and dev console logs (`error-capture.ts`)

### Notable API usage

| Feature | Client | Notes |
| --- | --- | --- |
| Products list | `GET /products?warehouseId=` | When set, `stock` is quantity **at that warehouse** (default warehouse when omitted) |
| Stock movement create | `POST /stock-movements` | Send `provider` for IN, `recipient` for OUT |
| Checkout | `POST /transactions` | Each cart item includes `warehouseId` for stock deduction |
| Audit | `GET /audit` | Responses include `userName`, `entityLabel` |
| Transactions | `GET /transactions` | Responses include `cashierName` |

Endpoint reference: [`backend/API.md`](../backend/API.md)

---

## Stock display behavior

- **Products page (default)** — `stock` and status badges use each product's **default warehouse**
- **Products page (warehouse filter)** — `stock` reflects the **selected warehouse**; list shows products stocked there or assigned as default
- **Cashier** — catalog always scoped to the **selected warehouse**; cart can combine lines from multiple warehouses in one checkout

---

## Deployment

### Vercel (primary)

1. Connect repo; set **Root Directory** to `frontend/`
2. Env: `VITE_API_BASE_URL` → your Render API URL (`…/api/v1`)
3. Optional: `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY` for image uploads
4. Add Vercel URL to backend `CORS_ALLOWED_ORIGINS` and Supabase Auth redirect URLs

`frontend/vercel.json` configures build output (`dist/client`) and SPA rewrites.

### Cloudflare Workers (optional)

```bash
npm run build
npx wrangler deploy
```

Set `VITE_API_BASE_URL` in Workers vars; add Workers URL to backend CORS.

---

## License

Internal project. Add a license before publishing.
