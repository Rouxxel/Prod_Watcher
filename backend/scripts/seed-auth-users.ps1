# Provision ProdWatch dev auth users via Supabase Admin API, assign roles, and load demo activity.
# Prerequisites: Flyway migrations applied (V1-V11), backend/.env configured, psql on PATH.
#
# Usage (from repo root):
#   .\backend\scripts\seed-auth-users.ps1

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Split-Path -Parent $ScriptDir
$EnvFile = Join-Path $BackendDir ".env"

if (-not (Test-Path $EnvFile)) {
    Write-Error "Missing $EnvFile — copy backend/.env.example and fill in Supabase credentials."
}

Get-Content $EnvFile | ForEach-Object {
    if ($_ -match '^\s*([^#=]+?)\s*=\s*(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim().Trim('"').Trim("'")
        Set-Item -Path "Env:$name" -Value $value
    }
}

foreach ($var in @("SUPABASE_URL", "SUPABASE_SERVICE_ROLE_KEY", "DATABASE_URL")) {
    if (-not (Get-Item "Env:$var" -ErrorAction SilentlyContinue)) {
        Write-Error "$var is required in backend/.env"
    }
}

$DevSeedPassword = if ($env:DEV_SEED_PASSWORD) { $env:DEV_SEED_PASSWORD } else { "ProdWatchDev2024!" }
$PsqlUrl = $env:DATABASE_URL -replace '^jdbc:', ''

function New-SeedUser {
    param(
        [string]$Email,
        [string]$Name
    )

    $body = @{
        email         = $Email
        password      = $DevSeedPassword
        email_confirm = $true
        user_metadata = @{ name = $Name }
    } | ConvertTo-Json -Compress

    $headers = @{
        apikey        = $env:SUPABASE_SERVICE_ROLE_KEY
        Authorization = "Bearer $($env:SUPABASE_SERVICE_ROLE_KEY)"
    }

    try {
        Invoke-RestMethod -Method Post -Uri "$($env:SUPABASE_URL)/auth/v1/admin/users" `
            -Headers $headers -ContentType "application/json" -Body $body | Out-Null
        Write-Host "  created $Email"
    }
    catch {
        $msg = $_.Exception.Message
        if ($msg -match "already|duplicate|registered") {
            Write-Host "  exists  $Email"
        }
        else {
            Write-Error "  FAILED  $Email : $msg"
        }
    }
}

function Set-UserBanned {
    param([string]$Email)

    $userId = & psql $PsqlUrl -tA -c "SELECT id FROM public.profiles WHERE email = '$Email' LIMIT 1;" 2>$null
    $userId = ($userId | Out-String).Trim()
    if (-not $userId) {
        Write-Host "  skip ban — profile not found for $Email"
        return
    }

    $headers = @{
        apikey        = $env:SUPABASE_SERVICE_ROLE_KEY
        Authorization = "Bearer $($env:SUPABASE_SERVICE_ROLE_KEY)"
    }
    $body = '{"ban_duration":"876000h"}'
    Invoke-RestMethod -Method Put -Uri "$($env:SUPABASE_URL)/auth/v1/admin/users/$userId" `
        -Headers $headers -ContentType "application/json" -Body $body | Out-Null
    Write-Host "  banned  $Email"
}

function Set-SeedRoles {
    $sql = @"
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
"@
    & psql $PsqlUrl -v ON_ERROR_STOP=1 -c $sql
}

Write-Host "Creating dev auth users (password: see DEV_SEED_PASSWORD in backend/.env.example)..."

New-SeedUser "alex@acme.co"   "Alex Reyes"
New-SeedUser "maya@acme.co"   "Maya Chen"
New-SeedUser "jordan@acme.co" "Jordan Park"
New-SeedUser "sam@acme.co"    "Sam Holt"
New-SeedUser "riley@acme.co"  "Riley Vega"
New-SeedUser "devon@acme.co"  "Devon Cruz"

Write-Host "Assigning roles..."
Set-SeedRoles

Write-Host "Deactivating inactive user..."
Set-UserBanned "devon@acme.co"

Write-Host "Loading demo movements, audit entries, and transactions..."
& psql $PsqlUrl -v ON_ERROR_STOP=1 -c "SELECT public.seed_demo_activity();"

Write-Host "Done. Dev users ready (alex@acme.co = admin)."
