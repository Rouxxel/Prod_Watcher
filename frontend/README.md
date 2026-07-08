# ProdWatch — Inventory & POS Frontend

ProdWatch is the **React frontend** for an inventory and point-of-sale system.
It talks to the **Java Spring Boot API** on Render (`backend/`) for auth, data,
and transactions. Product image uploads use **Supabase Storage** directly from
the browser (optional env vars).

---

## Features

- **Backend auth** — `/login` and owner `/signup` bootstrap via `POST /api/v1/auth/*`
- **Two operating modes** — `inventory` and `selling`, switchable from the topbar
- **Role-based sidebar** — menu items filter by the logged-in user's role
- **Inventory** — Dashboard, Products (CRUD + filters), Warehouses, Stock Movements, Audit
- **POS** — Cashier (search + cart), Cart, Transactions, Receipt dialog
- **Admin** — User provisioning, password reset, Settings (static MVP placeholders)
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

**Ports:** frontend **8000**, backend **8080** (avoids conflict).

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
  components/          # Layout, products, POS, users, ui
  hooks/
    use-current-user.tsx   # Auth context (login/logout, GET /users/me)
    queries.ts             # TanStack Query hooks
    use-cart.tsx           # POS cart + checkout
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

Endpoint reference: [`backend/API.md`](../backend/API.md)

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
