# Supabase Setup — Storage & Auth

Runbook for **Phase 5.3** (Storage) and **Phase 6** (Auth) from `docs/TASK_01_database.md`.

SQL migrations automate the storage bucket (`V12__storage_product_images.sql`) and auth triggers (`V3__profiles_and_roles.sql`). Dashboard steps below must be applied once per Supabase project.

---

## Storage — `product-images` bucket (Phase 5.3)

### Applied by Flyway (`V12`)

| Setting | Value |
| --- | --- |
| Bucket ID | `product-images` |
| Public read | Yes |
| Max file size | 5 MB |
| Allowed MIME types | `image/jpeg`, `image/png`, `image/webp`, `image/gif` |
| Write access | `admin`, `warehouse_manager`, `warehouse_worker` (active users only) |

### Public URL pattern

```
{SUPABASE_URL}/storage/v1/object/public/product-images/{path}
```

Example: `https://your-project.supabase.co/storage/v1/object/public/product-images/kettle-1.jpg`

### Seed product images

`V11__seed_data.sql` still uses external [picsum.photos](https://picsum.photos) URLs so demo data works without uploaded files. When ready:

1. Upload images to the `product-images` bucket (Dashboard → Storage, or API from TASK_03).
2. Update `products.images` to Storage URLs (migration or admin script).
3. Optional path convention: `{sku}/{index}.jpg` (e.g. `KTL-001/1.jpg`).

---

## Auth configuration (Phase 6.1)

Open **Supabase Dashboard → Authentication → Providers → Email**.

| Setting | Value | Notes |
| --- | --- | --- |
| Enable Email provider | **ON** | Required for login |
| Confirm email | **ON** | Applies to public `/signup` (bootstrap owner) |
| Secure email change | ON (recommended) | |
| Double confirm email changes | Optional | |

**Provisioned staff** (Admin API `createUser` with `email_confirm: true`) skip the confirmation email regardless of the above — handled by the backend seed script and production admin API.

### Disable public sign-up (after bootstrap)

Choose **one**:

1. **Dashboard:** Authentication → Providers → Email → disable **Enable sign ups** after the first admin exists, **or**
2. **Backend (preferred, TASK_02):** Reject `POST /auth/signup` when `user_roles` already has an `admin` row.

Keep the dashboard option as a safety net in production.

### Site URL & redirect URLs

**Authentication → URL Configuration**

| Field | Value |
| --- | --- |
| **Site URL** | Vercel production URL (e.g. `https://prodwatch.vercel.app`) |

**Redirect URLs** — add every origin that handles auth callbacks:

| Environment | URL(s) |
| --- | --- |
| Local dev | `http://localhost:8080/**`, `http://localhost:3000/**` |
| Production (primary) | `https://prodwatch.vercel.app/**` (replace with your Vercel URL) |
| Production (optional) | `https://prodwatch.your-subdomain.workers.dev/**` — only if Cloudflare Workers is deployed |

Local frontend dev server runs on **port 8080** (`frontend/vite.config.ts`). Include both 8080 and 3000 for flexibility.

Both Vercel and Workers URLs can coexist in the allowlist.

### JWT & session settings

**Authentication → Settings** (recommended starting points; tighten for production):

| Setting | Suggested value |
| --- | --- |
| JWT expiry | `3600` (1 hour) |
| Refresh token rotation | **Enabled** |
| Reuse interval | `10` seconds |

Store `SUPABASE_JWT_SECRET` (or JWT signing secret from Project Settings → API) in `backend/.env` for token validation in the Java API.

### Backend CORS alignment

Ensure `CORS_ALLOWED_ORIGINS` in `backend/.env` lists the same frontend origins:

```
CORS_ALLOWED_ORIGINS=http://localhost:8080,http://localhost:3000,https://prodwatch.vercel.app
```

Add the Workers URL when that deployment is active.

---

## Auth hooks & triggers (Phase 6.2)

Already implemented in migrations — no extra dashboard hooks required for MVP.

| Item | Location |
| --- | --- |
| `on_auth_user_created` → `profiles` row | `V3__profiles_and_roles.sql` (`handle_new_user` trigger on `auth.users`) |
| `bootstrap_assign_admin(user_id)` | `V3__profiles_and_roles.sql` — called by backend after owner email confirmation |
| Block inactive users | RLS policies use `is_active_user()`; **backend JWT filter (TASK_02)** must reject login/API access when `profiles.active = false` |
| Devon Cruz (inactive) | Seed script sets `profiles.active = false` + Auth ban via Admin API |

### Optional: Custom Access Token hook

If you want Supabase to embed `app_role` in the JWT, add a **Custom Access Token** hook later (Dashboard → Authentication → Hooks). Not required for MVP — the Java API loads role from `user_roles` after JWT validation.

---

## Auth lifecycle verification (Phase 6.3)

Use this checklist after migrations and `seed-auth-users` script (or real signup flow).

### Bootstrap owner (`/signup`)

- [ ] Unconfirmed user exists in `auth.users` but **cannot** access protected API routes
- [ ] After email confirmation, backend calls `bootstrap_assign_admin` → `user_roles` contains `admin`
- [ ] Second public sign-up is rejected (backend or dashboard sign-ups disabled)

### Provisioned staff (Admin API)

- [ ] `email_confirmed_at` is set immediately (`email_confirm: true`)
- [ ] User can log in at `/login` with admin-set password (no confirmation email)
- [ ] Role row exists in `user_roles` (not on `profiles`)

### Promote to admin

- [ ] Existing user gets new/updated `user_roles` row with `admin` — no new `auth.users` row
- [ ] Audit entry `ROLE_PROMOTED_TO_ADMIN` (backend, TASK_02)

### Quick SQL checks

```sql
-- Roles live in user_roles, not profiles
SELECT p.email, ur.role
FROM profiles p
LEFT JOIN user_roles ur ON ur.user_id = p.id
ORDER BY p.email;

-- Inactive user
SELECT email, active FROM profiles WHERE email = 'devon@acme.co';
```

### Quick auth API checks (local)

```bash
# Provisioned user login (after seed script)
curl -s -X POST "$SUPABASE_URL/auth/v1/token?grant_type=password" \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Content-Type: application/json" \
  -d '{"email":"jordan@acme.co","password":"ProdWatchDev2024!"}'

# Inactive user should fail or be rejected by backend after token issued
curl -s -X POST "$SUPABASE_URL/auth/v1/token?grant_type=password" \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Content-Type: application/json" \
  -d '{"email":"devon@acme.co","password":"ProdWatchDev2024!"}'
```

---

## Dashboard checklist (copy/paste)

Apply once per environment (project):

- [ ] Email provider enabled
- [ ] Confirm email ON (bootstrap sign-up)
- [ ] Site URL = Vercel production URL
- [ ] Redirect URLs: localhost:8080, localhost:3000, Vercel URL, Workers URL (if used)
- [ ] JWT expiry + refresh rotation configured
- [ ] Sign-ups disabled after bootstrap **or** backend guard in place
- [ ] Flyway V12 applied (`product-images` bucket visible under Storage)
- [ ] `seed-auth-users` script run for local dev (optional)

---

## Related files

| File | Purpose |
| --- | --- |
| `backend/src/main/resources/db/migration/V3__profiles_and_roles.sql` | Auth user → profile trigger, bootstrap admin function |
| `backend/src/main/resources/db/migration/V12__storage_product_images.sql` | Storage bucket + RLS |
| `backend/scripts/seed-auth-users.ps1` / `.sh` | Dev auth users + roles |
| `backend/.env.example` | Env vars + dev seed password comments |
| `docs/TASK_02_backend.md` | JWT filter, signup guard, Auth Admin API |
