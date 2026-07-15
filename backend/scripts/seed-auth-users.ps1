# Provision ProdWatch dev auth users via Supabase Admin API + REST (no psql required).
# Prerequisites: Flyway migrations applied (V1-V25), backend/.env configured.
#
# Usage (from repo root):
#   .\backend\scripts\seed-auth-users.ps1

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Split-Path -Parent $ScriptDir
$EnvFile = Join-Path $BackendDir ".env"

if (-not (Test-Path $EnvFile)) {
    Write-Error "Missing $EnvFile - copy backend/.env.example and fill in Supabase credentials."
}

Get-Content $EnvFile | ForEach-Object {
    if ($_ -match '^\s*([^#=]+?)\s*=\s*(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim().Trim('"').Trim("'")
        Set-Item -Path "Env:$name" -Value $value
    }
}

foreach ($var in @("SUPABASE_URL", "SUPABASE_SERVICE_ROLE_KEY")) {
    if (-not (Get-Item "Env:$var" -ErrorAction SilentlyContinue)) {
        Write-Error "$var is required in backend/.env"
    }
}

$DevSeedPassword = if ($env:DEV_SEED_PASSWORD) { $env:DEV_SEED_PASSWORD } else { "ProdWatchDev2024!" }

# Demo ecosystem id — must match V21__ecosystem_backfill.sql
$DemoEcosystemId = "33333333-3333-4333-8333-333333333301"

$SeedEmails = @(
    "alex@acme.co",
    "maya@acme.co",
    "jordan@acme.co",
    "sam@acme.co",
    "riley@acme.co",
    "devon@acme.co"
)

$AuthHeaders = @{
    apikey        = $env:SUPABASE_SERVICE_ROLE_KEY
    Authorization = "Bearer $($env:SUPABASE_SERVICE_ROLE_KEY)"
}

$RestHeaders = @{
    apikey        = $env:SUPABASE_SERVICE_ROLE_KEY
    Authorization = "Bearer $($env:SUPABASE_SERVICE_ROLE_KEY)"
    'Content-Type' = 'application/json'
}

function Ensure-DemoEcosystem {
    $body = @{ id = $DemoEcosystemId; name = "Acme Demo" } | ConvertTo-Json -Compress
    $headers = $RestHeaders.Clone()
    $headers["Prefer"] = "resolution=ignore-duplicates"

    try {
        Invoke-RestMethod -Method Post -Uri "$($env:SUPABASE_URL)/rest/v1/ecosystems" `
            -Headers $headers -Body $body | Out-Null
        Write-Host "  demo ecosystem ready ($DemoEcosystemId)"
    }
    catch {
        Write-Warning "  demo ecosystem: $($_.Exception.Message)"
    }
}

function Set-SeedEcosystem {
    $encoded = ($SeedEmails | ForEach-Object { [uri]::EscapeDataString($_) }) -join ","
    $uri = "$($env:SUPABASE_URL)/rest/v1/profiles?email=in.($encoded)"
    $body = @{ ecosystem_id = $DemoEcosystemId } | ConvertTo-Json -Compress

    try {
        Invoke-RestMethod -Method Patch -Uri $uri -Headers $RestHeaders -Body $body | Out-Null
        Write-Host "  assigned demo ecosystem to seed profiles"
    }
    catch {
        Write-Warning "  assign ecosystem: $($_.Exception.Message)"
    }
}

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

    try {
        Invoke-RestMethod -Method Post -Uri "$($env:SUPABASE_URL)/auth/v1/admin/users" `
            -Headers $AuthHeaders -ContentType "application/json" -Body $body | Out-Null
        Write-Host "  created $Email"
    }
    catch {
        $statusCode = $null
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        $msg = $_.Exception.Message
        if ($statusCode -eq 422 -or $msg -match "422|already|duplicate|registered|exists") {
            Write-Host "  exists  $Email"
            return
        }
        Write-Error "  FAILED  $Email : $msg"
    }
}

function Get-ProfileId {
    param([string]$Email)

    $uri = "$($env:SUPABASE_URL)/rest/v1/profiles?email=eq.$([uri]::EscapeDataString($Email))&select=id"
    $rows = Invoke-RestMethod -Method Get -Uri $uri -Headers $RestHeaders
    if ($rows -and $rows.Count -gt 0) {
        return $rows[0].id
    }
    return $null
}

function Add-UserRole {
    param(
        [string]$Email,
        [string]$Role
    )

    $userId = Get-ProfileId -Email $Email
    if (-not $userId) {
        Write-Warning "  skip role $Role - no profile for $Email"
        return
    }

    $body = @{ user_id = $userId; role = $Role } | ConvertTo-Json -Compress
    $headers = $RestHeaders.Clone()
    $headers["Prefer"] = "resolution=ignore-duplicates"

    try {
        Invoke-RestMethod -Method Post -Uri "$($env:SUPABASE_URL)/rest/v1/user_roles?on_conflict=user_id,role" `
            -Headers $headers -Body $body | Out-Null
        Write-Host "  role $Role -> $Email"
    }
    catch {
        Write-Warning "  role $Role for $Email : $($_.Exception.Message)"
    }
}

function Set-SeedRoles {
    Add-UserRole "alex@acme.co"   "admin"
    Add-UserRole "maya@acme.co"   "warehouse_manager"
    Add-UserRole "jordan@acme.co" "warehouse_worker"
    Add-UserRole "sam@acme.co"    "inspector"
    Add-UserRole "riley@acme.co"  "cashier"
    Add-UserRole "devon@acme.co"  "cashier"

    $uri = "$($env:SUPABASE_URL)/rest/v1/profiles?email=eq.devon@acme.co"
    Invoke-RestMethod -Method Patch -Uri $uri -Headers $RestHeaders -Body '{"active":false}' | Out-Null
    Write-Host "  deactivated devon@acme.co"
}

function Set-UserBanned {
    param([string]$Email)

    $userId = Get-ProfileId -Email $Email
    if (-not $userId) {
        Write-Host "  skip ban - profile not found for $Email"
        return
    }

    Invoke-RestMethod -Method Put -Uri "$($env:SUPABASE_URL)/auth/v1/admin/users/$userId" `
        -Headers $AuthHeaders -ContentType "application/json" -Body '{"ban_duration":"876000h"}' | Out-Null
    Write-Host "  banned  $Email"
}

function Invoke-SeedDemoActivity {
    try {
        Invoke-RestMethod -Method Post -Uri "$($env:SUPABASE_URL)/rest/v1/rpc/seed_demo_activity" `
            -Headers $RestHeaders -Body '{}' | Out-Null
    }
    catch {
        $statusCode = $null
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        if ($statusCode -eq 409) {
            Write-Host "  activity seed returned 409 (may already be loaded) - checking..."
        }
        else {
            throw
        }
    }

    $movements = Invoke-RestMethod -Method Get `
        -Uri "$($env:SUPABASE_URL)/rest/v1/stock_movements?select=id&limit=1" `
        -Headers $RestHeaders
    if ($movements.Count -gt 0) {
        Write-Host "  stock_movements populated"
    }
    else {
        Write-Warning "  stock_movements still empty - run SQL in Supabase: SELECT seed_demo_activity();"
    }
}

Write-Host "Ensuring demo ecosystem..."
Ensure-DemoEcosystem

Write-Host "Creating dev auth users (password: see DEV_SEED_PASSWORD in backend/.env.example)..."

New-SeedUser "alex@acme.co"   "Alex Reyes"
New-SeedUser "maya@acme.co"   "Maya Chen"
New-SeedUser "jordan@acme.co" "Jordan Park"
New-SeedUser "sam@acme.co"    "Sam Holt"
New-SeedUser "riley@acme.co"  "Riley Vega"
New-SeedUser "devon@acme.co"  "Devon Cruz"

Write-Host "Assigning roles..."
Set-SeedRoles

Write-Host "Assigning demo ecosystem to seed profiles..."
Set-SeedEcosystem

Write-Host "Deactivating inactive user..."
Set-UserBanned "devon@acme.co"

Write-Host "Loading demo movements, audit entries, and transactions..."
Invoke-SeedDemoActivity

Write-Host "Done. Dev users ready (alex@acme.co = admin)."
