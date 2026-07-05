#!/usr/bin/env bash
# Provision ProdWatch dev auth users via Supabase Admin API, assign roles, and load demo activity.
# Prerequisites: Flyway migrations applied (V1–V11), backend/.env configured, psql on PATH.
#
# Usage (from repo root):
#   bash backend/scripts/seed-auth-users.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$BACKEND_DIR/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE — copy backend/.env.example and fill in Supabase credentials."
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${SUPABASE_URL:?SUPABASE_URL is required in backend/.env}"
: "${SUPABASE_SERVICE_ROLE_KEY:?SUPABASE_SERVICE_ROLE_KEY is required in backend/.env}"
: "${DATABASE_URL:?DATABASE_URL is required in backend/.env}"

# psql expects postgresql:// not jdbc:postgresql://
PSQL_URL="${DATABASE_URL#jdbc:}"

DEV_SEED_PASSWORD="${DEV_SEED_PASSWORD:-ProdWatchDev2024!}"

create_user() {
  local email="$1"
  local name="$2"
  local payload
  payload=$(printf '{"email":"%s","password":"%s","email_confirm":true,"user_metadata":{"name":"%s"}}' \
    "$email" "$DEV_SEED_PASSWORD" "$name")

  local response http_code body
  response=$(curl -sS -w "\n%{http_code}" -X POST "$SUPABASE_URL/auth/v1/admin/users" \
    -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Content-Type: application/json" \
    -d "$payload")
  http_code=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')

  if [[ "$http_code" == "200" || "$http_code" == "201" ]]; then
    echo "  created $email"
    return 0
  fi

  if echo "$body" | grep -qi "already been registered\|already exists\|duplicate"; then
    echo "  exists  $email"
    return 0
  fi

  echo "  FAILED  $email (HTTP $http_code): $body" >&2
  return 1
}

ban_user() {
  local email="$1"
  local user_id
  user_id=$(psql "$PSQL_URL" -tA -c "SELECT id FROM public.profiles WHERE email = '$email' LIMIT 1;" 2>/dev/null || true)
  if [[ -z "$user_id" ]]; then
    echo "  skip ban — profile not found for $email"
    return 0
  fi

  curl -sS -X PUT "$SUPABASE_URL/auth/v1/admin/users/$user_id" \
    -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Content-Type: application/json" \
    -d '{"ban_duration":"876000h"}' >/dev/null
  echo "  banned  $email"
}

assign_roles() {
  psql "$PSQL_URL" -v ON_ERROR_STOP=1 <<'SQL'
INSERT INTO public.user_roles (user_id, role)
SELECT p.id, 'admin'::app_role FROM public.profiles p WHERE p.email = 'alex@acme.co'
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO public.user_roles (user_id, role)
SELECT p.id, 'warehouse_manager'::app_role FROM public.profiles p WHERE p.email = 'maya@acme.co'
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO public.user_roles (user_id, role)
SELECT p.id, 'warehouse_worker'::app_role FROM public.profiles p WHERE p.email = 'jordan@acme.co'
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO public.user_roles (user_id, role)
SELECT p.id, 'inspector'::app_role FROM public.profiles p WHERE p.email = 'sam@acme.co'
ON CONFLICT (user_id, role) DO NOTHING;

INSERT INTO public.user_roles (user_id, role)
SELECT p.id, 'cashier'::app_role FROM public.profiles p WHERE p.email IN ('riley@acme.co', 'devon@acme.co')
ON CONFLICT (user_id, role) DO NOTHING;

UPDATE public.profiles SET active = false WHERE email = 'devon@acme.co';
SQL
}

echo "Creating dev auth users (password: see DEV_SEED_PASSWORD in backend/.env.example)..."

create_user "alex@acme.co"   "Alex Reyes"
create_user "maya@acme.co"   "Maya Chen"
create_user "jordan@acme.co" "Jordan Park"
create_user "sam@acme.co"    "Sam Holt"
create_user "riley@acme.co"  "Riley Vega"
create_user "devon@acme.co"  "Devon Cruz"

echo "Assigning roles..."
assign_roles

echo "Deactivating inactive user..."
ban_user "devon@acme.co"

echo "Loading demo movements, audit entries, and transactions..."
psql "$PSQL_URL" -v ON_ERROR_STOP=1 -c "SELECT public.seed_demo_activity();"

echo "Done. Dev users ready (alex@acme.co = admin)."
