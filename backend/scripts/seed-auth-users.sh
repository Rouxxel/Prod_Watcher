#!/usr/bin/env bash
# Provision ProdWatch dev auth users via Supabase Admin API + REST (no psql required).
# Prerequisites: greenfield Flyway migrations applied (V1-V14), backend/.env configured.
#
# Usage (from repo root):
#   bash backend/scripts/seed-auth-users.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$BACKEND_DIR/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE - copy backend/.env.example and fill in Supabase credentials."
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${SUPABASE_URL:?SUPABASE_URL is required in backend/.env}"
: "${SUPABASE_SERVICE_ROLE_KEY:?SUPABASE_SERVICE_ROLE_KEY is required in backend/.env}"

DEV_SEED_PASSWORD="${DEV_SEED_PASSWORD:-ProdWatchDev2024!}"

# Demo ecosystem id — must match V14__seed_data.sql
DEMO_ECOSYSTEM_ID="33333333-3333-4333-8333-333333333301"

ensure_demo_ecosystem() {
  curl -sS -X POST "$SUPABASE_URL/rest/v1/ecosystems" \
    -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Content-Type: application/json" \
    -H "Prefer: resolution=ignore-duplicates" \
    -d "{\"id\":\"$DEMO_ECOSYSTEM_ID\",\"name\":\"Acme Demo\"}" >/dev/null
  echo "  demo ecosystem ready ($DEMO_ECOSYSTEM_ID)"
}

assign_seed_ecosystem() {
  curl -sS -X PATCH "$SUPABASE_URL/rest/v1/profiles?email=in.(alex@acme.co,maya@acme.co,jordan@acme.co,sam@acme.co,riley@acme.co,devon@acme.co)" \
    -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Content-Type: application/json" \
    -d "{\"ecosystem_id\":\"$DEMO_ECOSYSTEM_ID\"}" >/dev/null
  echo "  assigned demo ecosystem to seed profiles"
}

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

  if [[ "$http_code" == "422" ]] || echo "$body" | grep -qi "already been registered\|already exists\|duplicate"; then
    echo "  exists  $email"
    return 0
  fi

  echo "  FAILED  $email (HTTP $http_code): $body" >&2
  return 1
}

profile_id() {
  local email="$1"
  local encoded="${email/@/%40}"
  curl -sS "$SUPABASE_URL/rest/v1/profiles?email=eq.${encoded}&select=id" \
    -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" | sed -n 's/.*"id"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n1
}

add_role() {
  local email="$1"
  local role="$2"
  local user_id
  user_id=$(profile_id "$email")
  if [[ -z "$user_id" ]]; then
    echo "  skip role $role - no profile for $email" >&2
    return 0
  fi

  curl -sS -X POST "$SUPABASE_URL/rest/v1/user_roles?on_conflict=user_id,role" \
    -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Content-Type: application/json" \
    -H "Prefer: resolution=ignore-duplicates" \
    -d "{\"user_id\":\"$user_id\",\"role\":\"$role\"}" >/dev/null
  echo "  role $role -> $email"
}

assign_roles() {
  add_role "alex@acme.co"   "admin"
  add_role "maya@acme.co"   "warehouse_manager"
  add_role "jordan@acme.co" "warehouse_worker"
  add_role "sam@acme.co"    "inspector"
  add_role "riley@acme.co"  "cashier"
  add_role "devon@acme.co"  "cashier"

  curl -sS -X PATCH "$SUPABASE_URL/rest/v1/profiles?email=eq.devon@acme.co" \
    -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Content-Type: application/json" \
    -d '{"active":false}' >/dev/null
  echo "  deactivated devon@acme.co"
}

ban_user() {
  local email="$1"
  local user_id
  user_id=$(profile_id "$email")
  if [[ -z "$user_id" ]]; then
    echo "  skip ban - profile not found for $email"
    return 0
  fi

  curl -sS -X PUT "$SUPABASE_URL/auth/v1/admin/users/$user_id" \
    -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Content-Type: application/json" \
    -d '{"ban_duration":"876000h"}' >/dev/null
  echo "  banned  $email"
}

seed_activity() {
  curl -sS -X POST "$SUPABASE_URL/rest/v1/rpc/seed_demo_activity" \
    -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" \
    -H "Content-Type: application/json" \
    -d '{}' >/dev/null
}

echo "Ensuring demo ecosystem..."
ensure_demo_ecosystem

echo "Creating dev auth users (password: see DEV_SEED_PASSWORD in backend/.env.example)..."

create_user "alex@acme.co"   "Alex Reyes"
create_user "maya@acme.co"   "Maya Chen"
create_user "jordan@acme.co" "Jordan Park"
create_user "sam@acme.co"    "Sam Holt"
create_user "riley@acme.co"  "Riley Vega"
create_user "devon@acme.co"  "Devon Cruz"

echo "Assigning roles..."
assign_roles

echo "Assigning demo ecosystem to seed profiles..."
assign_seed_ecosystem

echo "Deactivating inactive user..."
ban_user "devon@acme.co"

echo "Loading demo movements, audit entries, and transactions..."
seed_activity

echo "Done. Dev users ready (alex@acme.co = admin)."
