-- Run in Supabase SQL Editor after migrations + auth seed.
-- Expected counts documented in docs/DATABASE_VERIFICATION.md
-- Demo ecosystem id: 33333333-3333-4333-8333-333333333301 (Acme Demo)

-- ---------------------------------------------------------------------------
-- 1. Row counts (global)
-- ---------------------------------------------------------------------------
SELECT 'warehouses' AS entity, count(*) AS actual, 3 AS expected FROM warehouses
UNION ALL SELECT 'products', count(*), 12 FROM products
UNION ALL SELECT 'profiles', count(*), 6 FROM profiles
UNION ALL SELECT 'user_roles', count(*), 6 FROM user_roles
UNION ALL SELECT 'stock_movements', count(*), 19 FROM stock_movements
UNION ALL SELECT 'audit_entries', count(*), 6 FROM audit_entries
UNION ALL SELECT 'transactions', count(*), 2 FROM transactions
ORDER BY entity;

-- ---------------------------------------------------------------------------
-- 1b. Per-ecosystem counts (demo seed should be isolated to Acme Demo)
-- ---------------------------------------------------------------------------
SELECT e.name AS ecosystem, e.id AS ecosystem_id,
       (SELECT count(*) FROM warehouses w WHERE w.ecosystem_id = e.id) AS warehouses,
       (SELECT count(*) FROM products p WHERE p.ecosystem_id = e.id) AS products,
       (SELECT count(*) FROM profiles pr WHERE pr.ecosystem_id = e.id) AS profiles,
       (SELECT count(*) FROM stock_movements sm WHERE sm.ecosystem_id = e.id) AS stock_movements,
       (SELECT count(*) FROM audit_entries ae WHERE ae.ecosystem_id = e.id) AS audit_entries,
       (SELECT count(*) FROM transactions t WHERE t.ecosystem_id = e.id) AS transactions
FROM ecosystems e
ORDER BY e.name;

-- Demo ecosystem expected (after full seed): 3 WH, 12 products, 6 profiles, 19 movements, 6 audit, 2 transactions
SELECT
    (SELECT count(*) FROM warehouses WHERE ecosystem_id = '33333333-3333-4333-8333-333333333301') = 3 AS demo_warehouses_ok,
    (SELECT count(*) FROM products WHERE ecosystem_id = '33333333-3333-4333-8333-333333333301') = 12 AS demo_products_ok,
    (SELECT count(*) FROM profiles WHERE ecosystem_id = '33333333-3333-4333-8333-333333333301') = 6 AS demo_profiles_ok,
    (SELECT count(*) FROM stock_movements WHERE ecosystem_id = '33333333-3333-4333-8333-333333333301') = 19 AS demo_movements_ok;

-- Seed profiles must belong to demo ecosystem
SELECT p.email, p.ecosystem_id,
       p.ecosystem_id = '33333333-3333-4333-8333-333333333301'::uuid AS in_demo_ecosystem
FROM profiles p
WHERE p.email LIKE '%@acme.co'
ORDER BY p.email;

-- ---------------------------------------------------------------------------
-- 2. Schema — no stock column on products
-- ---------------------------------------------------------------------------
SELECT column_name FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'products' AND column_name = 'stock';
-- Expected: 0 rows

-- ---------------------------------------------------------------------------
-- 3. Enums
-- ---------------------------------------------------------------------------
SELECT typname FROM pg_type
WHERE typname IN ('app_role', 'stock_movement_type', 'transaction_status')
ORDER BY typname;
-- Expected: 3 rows

-- ---------------------------------------------------------------------------
-- 4. RLS enabled
-- ---------------------------------------------------------------------------
SELECT c.relname, c.relrowsecurity
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public'
  AND c.relname IN ('profiles','user_roles','warehouses','products','stock_movements','audit_entries','transactions')
ORDER BY c.relname;
-- Expected: relrowsecurity = true for all

-- ---------------------------------------------------------------------------
-- 5. Roles on user_roles, not profiles
-- ---------------------------------------------------------------------------
SELECT column_name FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'role';
-- Expected: 0 rows

SELECT p.email, ur.role, p.active
FROM profiles p
LEFT JOIN user_roles ur ON ur.user_id = p.id
ORDER BY p.email;

-- ---------------------------------------------------------------------------
-- 6. Helper functions
-- ---------------------------------------------------------------------------
SELECT public.has_role(
    (SELECT id FROM profiles WHERE email = 'alex@acme.co'),
    'admin'::app_role
) AS alex_is_admin;
-- Expected: true

SELECT public.is_active_user(
    (SELECT id FROM profiles WHERE email = 'devon@acme.co')
) AS devon_active;
-- Expected: false

-- ---------------------------------------------------------------------------
-- 7. Stock vs frontend mock (product_stock_summary)
-- ---------------------------------------------------------------------------
SELECT p.sku, pss.quantity AS stock,
       CASE p.sku
           WHEN 'KTL-001' THEN 42 WHEN 'WCB-220' THEN 6 WHEN 'APR-CHR' THEN 0
           WHEN 'TMP-58' THEN 24 WHEN 'MUG-S4' THEN 88 WHEN 'CBC-1L' THEN 4
           WHEN 'CIS-10' THEN 17 WHEN 'TEA-BMB' THEN 31 WHEN 'VAS-HT1' THEN 9
           WHEN 'WTB-77' THEN 12 WHEN 'CDL-BW3' THEN 60 WHEN 'MLK-350' THEN 2
       END AS expected,
       pss.quantity = CASE p.sku
           WHEN 'KTL-001' THEN 42 WHEN 'WCB-220' THEN 6 WHEN 'APR-CHR' THEN 0
           WHEN 'TMP-58' THEN 24 WHEN 'MUG-S4' THEN 88 WHEN 'CBC-1L' THEN 4
           WHEN 'CIS-10' THEN 17 WHEN 'TEA-BMB' THEN 31 WHEN 'VAS-HT1' THEN 9
           WHEN 'WTB-77' THEN 12 WHEN 'CDL-BW3' THEN 60 WHEN 'MLK-350' THEN 2
       END AS matches_mock
FROM products p
JOIN product_stock_summary pss ON pss.product_id = p.id
ORDER BY p.sku;

-- ---------------------------------------------------------------------------
-- 8. Audit append-only (no UPDATE/DELETE policies for clients)
-- ---------------------------------------------------------------------------
SELECT polname, polcmd
FROM pg_policy pol
JOIN pg_class c ON c.oid = pol.polrelid
WHERE c.relname = 'audit_entries'
ORDER BY polname;
-- Expected: SELECT only (insert via service role / backend)

-- ---------------------------------------------------------------------------
-- 9. FK integrity
-- ---------------------------------------------------------------------------
SELECT count(*) AS orphan_movements FROM stock_movements sm
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.id = sm.product_id)
   OR NOT EXISTS (SELECT 1 FROM profiles pr WHERE pr.id = sm.user_id);
-- Expected: 0
