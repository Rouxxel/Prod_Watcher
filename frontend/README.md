# ProdWatch — Inventory & POS Frontend Shell

ProdWatch is a **frontend-only shell** for an inventory and point-of-sale
management system. It ships a fully navigable, production-feel SaaS dashboard
built on **React 19 + TypeScript + TanStack Start + Tailwind v4 + shadcn/ui**,
backed entirely by **mock data**. There is no real backend, no real auth, no
database — every list, form, login, and transaction round-trips through an
in-memory service layer designed to be swapped for a real API later without
touching pages or components.

The goal of this repo is to nail UX, information architecture, role-based
navigation, and visual design first, so wiring it to a real backend becomes a
mechanical refactor of `src/services/*` and `src/hooks/*`.

---

## Features

- **Mock authentication** — `/login` with role selector (admin, warehouse
  manager, warehouse worker, inspector, cashier), email, and password
  (`demo1234` for all seed users). Persists the session in `localStorage`.
- **Two operating modes** — `inventory` and `selling`, picked on a landing
  screen after login and switchable from the topbar dropdown. Routes are
  guarded per mode.
- **Role-based sidebar** — menu items filter by the logged-in user's role.
- **Inventory side** — Dashboard, Products (CRUD + filters), Warehouses,
  Stock Movements, Inventory Audit.
- **POS side** — Cashier (search + cart), Cart page, Transactions, Receipt
  dialog with tax breakdown.
- **Admin** — Users & Roles, Settings.
- **Dark-only theme** with a burgundy/vinotinto accent system defined via
  semantic `oklch` tokens in `src/styles.css`.
- **TanStack Query** wraps every mock service so React Query call sites stay
  identical when a real backend lands.

---

## Tech stack

| Concern        | Choice                                                |
| -------------- | ----------------------------------------------------- |
| Framework      | TanStack Start v1 (React 19, SSR-capable, Vite 7)     |
| Routing        | TanStack Router (file-based, in `src/routes/`)        |
| Data layer     | TanStack Query + mock services in `src/services/`     |
| UI primitives  | shadcn/ui (Radix) in `src/components/ui/`             |
| Styling        | Tailwind CSS v4 via `src/styles.css` (no `tailwind.config.js`) |
| Forms          | react-hook-form + zod                                 |
| Icons          | lucide-react                                          |
| Toasts         | sonner                                                |
| Runtime target | Vercel (primary); Cloudflare Workers optional (`@cloudflare/vite-plugin`) |

> **No `index.html` / `main.tsx` / `App.tsx`.** TanStack Start owns the HTML
> shell via `src/routes/__root.tsx` (`shellComponent`), and `src/router.tsx`
> bootstraps the router. Global styles live in `src/styles.css`.

---

## Project structure

```text
src/
  routes/                       # File-based routes (auto-registered in routeTree.gen.ts)
    __root.tsx                  # HTML shell, providers, favicon, meta
    login.tsx                   # Mock login (role + email + password)
    _app.tsx                    # Authenticated layout (sidebar + topbar + mode guard)
    _app.index.tsx              # Dashboard (/)
    _app.products.tsx
    _app.warehouses.tsx
    _app.stock-movements.tsx
    _app.audit.tsx
    _app.cashier.tsx
    _app.cart.tsx
    _app.transactions.tsx
    _app.users.tsx
    _app.settings.tsx

  components/
    layout/                     # AppSidebar, Topbar, ModeSelectScreen, RoleBadge, CRTOverlay
    common/                     # PageHeader, StatCard, EmptyState, TableSkeleton
    products/                   # ProductFormDialog, ProductImageCarousel
    pos/                        # Cart, receipt, search panels
    ui/                         # shadcn primitives (do not edit casually)

  hooks/
    use-current-user.tsx        # Mock auth context (login/logout, localStorage)
    use-app-mode.tsx            # Inventory vs Selling mode
    use-business-mode.tsx
    use-cart.tsx                # POS cart context
    queries.ts                  # TanStack Query hooks wrapping services

  services/                     # Mock API layer — the seam to a real backend
    api.ts                      # fakeDelay(), newId()
    products.service.ts
    warehouses.service.ts
    movements.service.ts
    audit.service.ts
    transactions.service.ts
    users.service.ts

  mock/seed.ts                  # In-memory seed data
  types/index.ts                # Shared domain types
  lib/                          # format, notify, utils
  assets/                       # Logo, images
  styles.css                    # Tailwind v4 + design tokens

  router.tsx                    # createRouter(...) — query client + route tree
  start.ts                      # createStart(...) — server middleware
  server.ts                     # Worker fetch entry (SSR + error wrapping)
```

### Routing conventions

TanStack Router uses **dot-separated filenames**:

- `_app.tsx` → pathless layout that renders `<Outlet />` for everything below
- `_app.index.tsx` → `/`
- `_app.products.tsx` → `/products`
- `login.tsx` → `/login` (outside the `_app` layout, so no sidebar)

`src/routeTree.gen.ts` is **auto-generated** — never edit it by hand.

---

## Running locally

```bash
bun install
bun run dev          # http://localhost:8080
bun run build        # production build (Worker target)
bun run lint
```

### Mock credentials

Any seed user from `src/mock/seed.ts` works. Password is always:

```
demo1234
```

Pick the matching role in the login dropdown.

---

## Design system

All colors, gradients, shadows, and radii are declared as `oklch` tokens in
`src/styles.css`. **Never hardcode hex/rgb in components** — always reach for
semantic Tailwind classes that map to those tokens (`bg-card`, `text-primary`,
`border-border`, `bg-[--gradient-primary]`, etc.). The app is dark-only;
`<html class="dark">` is set permanently in `__root.tsx`.

---

## Wiring this to a real backend

The whole point of the shell is that pages, hooks, and components are
**agnostic to where data comes from**. To go live, replace the mock service
layer — that's it.

See `docs/TASK_03_frontend.md` for the full integration checklist.

### 1. Stack overview

| Layer | Technology |
| --- | --- |
| Frontend (this repo) | TanStack Start — deploy to **Vercel** (primary) or **Cloudflare Workers** (optional) |
| API | **Java Spring Boot 3** on Render (`backend/`) |
| Database & Auth | **Supabase** (PostgreSQL + Auth + Storage) |

Recommended order:

1. Provision Supabase (Postgres, Auth, Storage) — see `docs/TASK_01_database.md`.
2. Deploy the Java API — see `docs/TASK_02_backend.md`.
3. Wire this frontend — see `docs/TASK_03_frontend.md`.

### 2. Replace each service module

Each file in `src/services/` exposes the same shape:

```ts
export const productsService = {
  list:   () => Promise<Product[]>,
  get:    (id) => Promise<Product | undefined>,
  create: (input: ProductInput) => Promise<Product>,
  update: (id, input) => Promise<Product>,
  remove: (id) => Promise<void>,
};
```

Swap the mock body for HTTP calls to the Java API:

```ts
// src/services/products.service.ts
import { apiGet, apiPost, apiPatch, apiDelete } from "./api";
import type { Product, ProductInput } from "@/types";

export const productsService = {
  async list() {
    return apiGet<Product[]>("/products");
  },
  async create(input: ProductInput) {
    return apiPost<Product>("/products", input);
  },
  // ...etc
};
```

Set `VITE_API_BASE_URL` to your Render API (e.g. `https://prodwatch-api.onrender.com/api/v1`).

Hooks in `src/hooks/queries.ts` and every page component **stay unchanged** —
TanStack Query keys (`['products']`, `['products', id]`) and `invalidateQueries`
calls already match.

For server-only logic (webhooks, privileged operations), use **TanStack Start
server functions** (`createServerFn`) in `*.functions.ts` files — keep
service-role keys on the server only.

### 3. Replace mock auth

All auth goes through the **Java API** — no Supabase client in the frontend:

- **Owner:** `/signup` → `POST /api/v1/auth/signup` (bootstrap only)
- **Staff:** admin provisions via `/users`; employees use `/login`
- **Login:** `POST /api/v1/auth/login` → store `accessToken` from response; send `Authorization: Bearer …` on API calls
- **Logout:** `POST /api/v1/auth/logout` + clear stored token
- Profile + role from `GET /api/v1/users/me`
- Keep the existing `login/logout` context shape in `use-current-user.tsx`

### 4. Replace seed data

Delete `src/mock/seed.ts` once real data flows. Keep `src/types/index.ts` —
it's still the source of truth for shared domain types.

### 5. File storage

Product images currently come from `src/assets/`. Use a **Supabase Storage**
bucket (e.g. `product-images`) with public read + authenticated write policies,
and store the resulting URLs in `products.images`.

### 6. Deployment

- **Vercel (primary):** connect the `frontend/` directory, set `VITE_*` env vars.
- **Cloudflare Workers (optional):** `bun run build && wrangler deploy` using `wrangler.jsonc`.
- Add production URL(s) to Supabase Auth redirect allowlist and backend CORS.

---

## What's intentionally NOT here

- No real auth, no JWTs, no password hashing.
- No backend, database, or external integrations.
- No tests yet — add Vitest once business logic moves out of mocks.
- No theme toggle — dark only by design.

---

## License

Internal project shell. Add a license before publishing.
