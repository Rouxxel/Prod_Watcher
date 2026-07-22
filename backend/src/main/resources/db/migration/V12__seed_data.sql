-- Reference seed data (mirrors frontend/src/mock/seed.ts).
-- Warehouses and products apply on Flyway migrate. Activity data (movements, audit,
-- transactions) is loaded by seed_demo_activity() after auth users exist.
-- Step 2: run backend/scripts/seed-auth-users.ps1 — see docs/DATABASE_VERIFICATION.md
--
-- Ecosystem: V12 predates the ecosystems table (V19). Demo rows are tagged with
-- ecosystem_id in V21__ecosystem_backfill.sql. seed_demo_activity() is updated
-- in V25 to insert activity rows with the demo ecosystem id.

-- ---------------------------------------------------------------------------
-- Warehouses (fixed IDs for idempotent seed)
-- ---------------------------------------------------------------------------

INSERT INTO public.warehouses (id, name, location)
VALUES
    ('11111111-1111-4111-8111-111111111101', 'Central Depot', 'Caracas, VE'),
    ('11111111-1111-4111-8111-111111111102', 'North Hub', 'Valencia, VE'),
    ('11111111-1111-4111-8111-111111111103', 'Coastal Annex', 'Maracaibo, VE')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Products
-- ---------------------------------------------------------------------------

INSERT INTO public.products (id, name, sku, category, price, default_warehouse_id, low_stock_threshold, images)
VALUES
    (
        '22222222-2222-4222-8222-222222222201',
        'Ceramic Pour-Over Kettle',
        'KTL-001',
        'Kitchen',
        78.00,
        '11111111-1111-4111-8111-111111111101',
        10,
        ARRAY[
            'https://picsum.photos/seed/kettle-1/600/600',
            'https://picsum.photos/seed/kettle-2/600/600',
            'https://picsum.photos/seed/kettle-3/600/600',
            'https://picsum.photos/seed/kettle-4/600/600'
        ]
    ),
    (
        '22222222-2222-4222-8222-222222222202',
        'Walnut Cutting Board',
        'WCB-220',
        'Kitchen',
        54.50,
        '11111111-1111-4111-8111-111111111101',
        10,
        ARRAY[
            'https://picsum.photos/seed/board-1/600/600',
            'https://picsum.photos/seed/board-2/600/600',
            'https://picsum.photos/seed/board-3/600/600'
        ]
    ),
    (
        '22222222-2222-4222-8222-222222222203',
        'Linen Apron — Charcoal',
        'APR-CHR',
        'Apparel',
        39.00,
        '11111111-1111-4111-8111-111111111102',
        5,
        ARRAY[
            'https://picsum.photos/seed/apron-1/600/600',
            'https://picsum.photos/seed/apron-2/600/600'
        ]
    ),
    (
        '22222222-2222-4222-8222-222222222204',
        'Brass Espresso Tamper',
        'TMP-58',
        'Barista',
        62.00,
        '11111111-1111-4111-8111-111111111101',
        8,
        ARRAY[
            'https://picsum.photos/seed/tamper-1/600/600',
            'https://picsum.photos/seed/tamper-2/600/600',
            'https://picsum.photos/seed/tamper-3/600/600'
        ]
    ),
    (
        '22222222-2222-4222-8222-222222222205',
        'Stoneware Mug Set (4)',
        'MUG-S4',
        'Kitchen',
        48.00,
        '11111111-1111-4111-8111-111111111103',
        15,
        ARRAY[
            'https://picsum.photos/seed/mugs-1/600/600',
            'https://picsum.photos/seed/mugs-2/600/600',
            'https://picsum.photos/seed/mugs-3/600/600',
            'https://picsum.photos/seed/mugs-4/600/600'
        ]
    ),
    (
        '22222222-2222-4222-8222-222222222206',
        'Cold Brew Carafe — 1L',
        'CBC-1L',
        'Barista',
        34.00,
        '11111111-1111-4111-8111-111111111102',
        6,
        ARRAY[
            'https://picsum.photos/seed/carafe-1/600/600',
            'https://picsum.photos/seed/carafe-2/600/600'
        ]
    ),
    (
        '22222222-2222-4222-8222-222222222207',
        'Cast Iron Skillet 10"',
        'CIS-10',
        'Kitchen',
        92.00,
        '11111111-1111-4111-8111-111111111101',
        5,
        ARRAY[
            'https://picsum.photos/seed/skillet-1/600/600',
            'https://picsum.photos/seed/skillet-2/600/600',
            'https://picsum.photos/seed/skillet-3/600/600'
        ]
    ),
    (
        '22222222-2222-4222-8222-222222222208',
        'Bamboo Tea Tray',
        'TEA-BMB',
        'Kitchen',
        28.00,
        '11111111-1111-4111-8111-111111111103',
        10,
        ARRAY[
            'https://picsum.photos/seed/tray-1/600/600',
            'https://picsum.photos/seed/tray-2/600/600'
        ]
    ),
    (
        '22222222-2222-4222-8222-222222222209',
        'Hand-Thrown Vase',
        'VAS-HT1',
        'Home',
        120.00,
        '11111111-1111-4111-8111-111111111102',
        4,
        ARRAY[
            'https://picsum.photos/seed/vase-1/600/600',
            'https://picsum.photos/seed/vase-2/600/600',
            'https://picsum.photos/seed/vase-3/600/600',
            'https://picsum.photos/seed/vase-4/600/600'
        ]
    ),
    (
        '22222222-2222-4222-8222-222222222210',
        'Wool Throw Blanket',
        'WTB-77',
        'Home',
        145.00,
        '11111111-1111-4111-8111-111111111103',
        4,
        ARRAY[
            'https://picsum.photos/seed/blanket-1/600/600',
            'https://picsum.photos/seed/blanket-2/600/600',
            'https://picsum.photos/seed/blanket-3/600/600'
        ]
    ),
    (
        '22222222-2222-4222-8222-222222222211',
        'Beeswax Candle Trio',
        'CDL-BW3',
        'Home',
        36.00,
        '11111111-1111-4111-8111-111111111101',
        10,
        ARRAY[
            'https://picsum.photos/seed/candle-1/600/600',
            'https://picsum.photos/seed/candle-2/600/600',
            'https://picsum.photos/seed/candle-3/600/600'
        ]
    ),
    (
        '22222222-2222-4222-8222-222222222212',
        'Stainless Milk Pitcher',
        'MLK-350',
        'Barista',
        22.00,
        '11111111-1111-4111-8111-111111111102',
        6,
        ARRAY[
            'https://picsum.photos/seed/pitcher-1/600/600'
        ]
    )
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Activity seed (movements, audit, transactions) — requires auth users / profiles
-- ---------------------------------------------------------------------------

-- Returns jsonb so it works over the Supabase REST RPC endpoint (PostgREST).
CREATE OR REPLACE FUNCTION public.seed_demo_activity()
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    u_admin     uuid;
    u_manager   uuid;
    u_worker    uuid;
    u_inspector uuid;
    u_cashier   uuid;
    u_inactive  uuid;
    w_central   uuid := '11111111-1111-4111-8111-111111111101';
    w_north     uuid := '11111111-1111-4111-8111-111111111102';
    w_coastal   uuid := '11111111-1111-4111-8111-111111111103';
BEGIN
    SELECT id INTO u_admin FROM profiles WHERE email = 'alex@acme.co';
    SELECT id INTO u_manager FROM profiles WHERE email = 'maya@acme.co';
    SELECT id INTO u_worker FROM profiles WHERE email = 'jordan@acme.co';
    SELECT id INTO u_inspector FROM profiles WHERE email = 'sam@acme.co';
    SELECT id INTO u_cashier FROM profiles WHERE email = 'riley@acme.co';
    SELECT id INTO u_inactive FROM profiles WHERE email = 'devon@acme.co';

    IF u_worker IS NULL OR u_cashier IS NULL THEN
        RETURN jsonb_build_object('status', 'skipped', 'reason', 'provision auth users first');
    END IF;

    -- Opening balances so product_stock_summary matches frontend mock Product.stock
    INSERT INTO stock_movements (id, type, product_id, qty, to_warehouse_id, user_id, note, created_at)
    VALUES
        ('33333333-3333-4333-8333-333333333001', 'IN', '22222222-2222-4222-8222-222222222201', 22, w_central, u_worker, 'Opening balance', now() - interval '30 days'),
        ('33333333-3333-4333-8333-333333333002', 'IN', '22222222-2222-4222-8222-222222222202', 10, w_central, u_worker, 'Opening balance', now() - interval '30 days'),
        ('33333333-3333-4333-8333-333333333004', 'IN', '22222222-2222-4222-8222-222222222204', 27, w_central, u_worker, 'Opening balance', now() - interval '30 days'),
        ('33333333-3333-4333-8333-333333333005', 'IN', '22222222-2222-4222-8222-222222222205', 100, w_coastal, u_worker, 'Opening balance', now() - interval '30 days'),
        ('33333333-3333-4333-8333-333333333006', 'IN', '22222222-2222-4222-8222-222222222206', 6, w_north, u_worker, 'Opening balance', now() - interval '30 days'),
        ('33333333-3333-4333-8333-333333333007', 'IN', '22222222-2222-4222-8222-222222222207', 17, w_central, u_worker, 'Opening balance', now() - interval '30 days'),
        ('33333333-3333-4333-8333-333333333008', 'IN', '22222222-2222-4222-8222-222222222208', 31, w_coastal, u_worker, 'Opening balance', now() - interval '30 days'),
        ('33333333-3333-4333-8333-333333333009', 'IN', '22222222-2222-4222-8222-222222222209', 9, w_north, u_worker, 'Opening balance', now() - interval '30 days'),
        ('33333333-3333-4333-8333-333333333010', 'IN', '22222222-2222-4222-8222-222222222210', 4, w_coastal, u_worker, 'Opening balance', now() - interval '30 days'),
        ('33333333-3333-4333-8333-333333333011', 'IN', '22222222-2222-4222-8222-222222222211', 80, w_central, u_worker, 'Opening balance', now() - interval '30 days'),
        ('33333333-3333-4333-8333-333333333012', 'IN', '22222222-2222-4222-8222-222222222212', 1, w_north, u_worker, 'Opening balance', now() - interval '30 days')
    ON CONFLICT (id) DO NOTHING;

    -- 8 demo movements (all movement types)
    INSERT INTO stock_movements (id, type, product_id, qty, from_warehouse_id, to_warehouse_id, user_id, note, created_at)
    VALUES
        ('33333333-3333-4333-8333-333333333101', 'IN', '22222222-2222-4222-8222-222222222201', 20, NULL, w_central, u_worker, 'Supplier delivery', now() - interval '0 days' + interval '8 hours'),
        ('33333333-3333-4333-8333-333333333102', 'OUT', '22222222-2222-4222-8222-222222222202', 4, w_central, NULL, u_cashier, 'POS sale', now() - interval '0 days' + interval '10 hours'),
        ('33333333-3333-4333-8333-333333333103', 'TRANSFER', '22222222-2222-4222-8222-222222222205', 12, w_coastal, w_central, u_manager, NULL, now() - interval '1 day' + interval '14 hours'),
        ('33333333-3333-4333-8333-333333333104', 'ADJUSTMENT', '22222222-2222-4222-8222-222222222206', 2, w_north, NULL, u_inspector, 'Damaged units', now() - interval '1 day' + interval '16 hours'),
        ('33333333-3333-4333-8333-333333333105', 'IN', '22222222-2222-4222-8222-222222222210', 8, NULL, w_coastal, u_worker, NULL, now() - interval '2 days' + interval '9 hours'),
        ('33333333-3333-4333-8333-333333333106', 'OUT', '22222222-2222-4222-8222-222222222204', 3, w_central, NULL, u_cashier, NULL, now() - interval '2 days' + interval '11 hours'),
        ('33333333-3333-4333-8333-333333333107', 'TRANSFER', '22222222-2222-4222-8222-222222222211', 20, w_central, w_north, u_manager, NULL, now() - interval '3 days' + interval '13 hours'),
        ('33333333-3333-4333-8333-333333333108', 'ADJUSTMENT', '22222222-2222-4222-8222-222222222212', 1, NULL, w_north, u_inspector, 'Recount', now() - interval '4 days' + interval '10 hours')
    ON CONFLICT (id) DO NOTHING;

    -- Audit entries
    INSERT INTO audit_entries (id, user_id, action, entity, entity_id, details, created_at)
    VALUES
        ('44444444-4444-4444-8444-444444444401', u_admin, 'USER_CREATED', 'user', COALESCE(u_inactive, u_admin), 'Invited Devon Cruz', now() - interval '0 days' + interval '7 hours'),
        ('44444444-4444-4444-8444-444444444402', u_manager, 'PRODUCT_UPDATED', 'product', '22222222-2222-4222-8222-222222222202', 'Price 52.00 → 54.50', now() - interval '0 days' + interval '9 hours'),
        ('44444444-4444-4444-8444-444444444403', u_inspector, 'AUDIT_RUN', 'warehouse', w_north, 'Spot check, 2 variances', now() - interval '1 day' + interval '16 hours'),
        ('44444444-4444-4444-8444-444444444404', u_worker, 'STOCK_RECEIVED', 'movement', '33333333-3333-4333-8333-333333333101', NULL, now() - interval '0 days' + interval '8 hours'),
        ('44444444-4444-4444-8444-444444444405', u_cashier, 'TRANSACTION_COMPLETED', 'transaction', '55555555-5555-4555-8555-555555555501', NULL, now() - interval '0 days' + interval '10 hours'),
        ('44444444-4444-4444-8444-444444444406', u_admin, 'ROLE_CHANGED', 'user', u_worker, 'worker → manager (reverted)', now() - interval '5 days' + interval '11 hours')
    ON CONFLICT (id) DO NOTHING;

    -- 2 sample POS transactions
    INSERT INTO transactions (id, items, subtotal, tax, total, cashier_id, status, created_at)
    VALUES
        (
            '55555555-5555-4555-8555-555555555501',
            '[
                {"productId": "22222222-2222-4222-8222-222222222202", "name": "Walnut Cutting Board", "sku": "WCB-220", "qty": 2, "unitPrice": 54.5},
                {"productId": "22222222-2222-4222-8222-222222222211", "name": "Beeswax Candle Trio", "sku": "CDL-BW3", "qty": 1, "unitPrice": 36.0}
            ]'::jsonb,
            145.00,
            23.20,
            168.20,
            u_cashier,
            'completed',
            now() - interval '0 days' + interval '10 hours'
        ),
        (
            '55555555-5555-4555-8555-555555555502',
            '[
                {"productId": "22222222-2222-4222-8222-222222222204", "name": "Brass Espresso Tamper", "sku": "TMP-58", "qty": 1, "unitPrice": 62.0}
            ]'::jsonb,
            62.00,
            9.92,
            71.92,
            u_cashier,
            'completed',
            now() - interval '1 day' + interval '12 hours'
        )
    ON CONFLICT (id) DO NOTHING;

    RETURN jsonb_build_object(
        'status', 'ok',
        'stock_movements', (SELECT count(*) FROM stock_movements),
        'audit_entries', (SELECT count(*) FROM audit_entries),
        'transactions', (SELECT count(*) FROM transactions)
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.seed_demo_activity() TO service_role;

-- If auth users were provisioned before this migration, load activity data now.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.profiles WHERE email = 'jordan@acme.co') THEN
        PERFORM public.seed_demo_activity();
    END IF;
END;
$$;
