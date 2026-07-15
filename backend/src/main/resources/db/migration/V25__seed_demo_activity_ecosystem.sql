-- Replace seed_demo_activity() so activity rows include demo ecosystem_id (V20+).
-- Warehouses/products from V12 are backfilled in V21; this fixes movements/audit/transactions on re-run.

CREATE OR REPLACE FUNCTION public.seed_demo_activity()
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    demo_eco     uuid := '33333333-3333-4333-8333-333333333301';
    u_admin      uuid;
    u_manager    uuid;
    u_worker     uuid;
    u_inspector  uuid;
    u_cashier    uuid;
    u_inactive   uuid;
    w_central    uuid := '11111111-1111-4111-8111-111111111101';
    w_north      uuid := '11111111-1111-4111-8111-111111111102';
    w_coastal    uuid := '11111111-1111-4111-8111-111111111103';
BEGIN
    INSERT INTO public.ecosystems (id, name)
    VALUES (demo_eco, 'Acme Demo')
    ON CONFLICT (id) DO NOTHING;

    SELECT id INTO u_admin FROM profiles WHERE email = 'alex@acme.co';
    SELECT id INTO u_manager FROM profiles WHERE email = 'maya@acme.co';
    SELECT id INTO u_worker FROM profiles WHERE email = 'jordan@acme.co';
    SELECT id INTO u_inspector FROM profiles WHERE email = 'sam@acme.co';
    SELECT id INTO u_cashier FROM profiles WHERE email = 'riley@acme.co';
    SELECT id INTO u_inactive FROM profiles WHERE email = 'devon@acme.co';

    IF u_worker IS NULL OR u_cashier IS NULL THEN
        RETURN jsonb_build_object('status', 'skipped', 'reason', 'provision auth users first');
    END IF;

    UPDATE public.profiles
    SET ecosystem_id = demo_eco
    WHERE email IN (
        'alex@acme.co', 'maya@acme.co', 'jordan@acme.co',
        'sam@acme.co', 'riley@acme.co', 'devon@acme.co'
    )
      AND (ecosystem_id IS NULL OR ecosystem_id <> demo_eco);

    -- Opening balances so product_stock_summary matches frontend mock Product.stock
    INSERT INTO stock_movements (
        id, type, product_id, qty, to_warehouse_id, user_id, note, created_at, ecosystem_id)
    VALUES
        ('33333333-3333-4333-8333-333333333001', 'IN', '22222222-2222-4222-8222-222222222201', 22, w_central, u_worker, 'Opening balance', now() - interval '30 days', demo_eco),
        ('33333333-3333-4333-8333-333333333002', 'IN', '22222222-2222-4222-8222-222222222202', 10, w_central, u_worker, 'Opening balance', now() - interval '30 days', demo_eco),
        ('33333333-3333-4333-8333-333333333004', 'IN', '22222222-2222-4222-8222-222222222204', 27, w_central, u_worker, 'Opening balance', now() - interval '30 days', demo_eco),
        ('33333333-3333-4333-8333-333333333005', 'IN', '22222222-2222-4222-8222-222222222205', 100, w_coastal, u_worker, 'Opening balance', now() - interval '30 days', demo_eco),
        ('33333333-3333-4333-8333-333333333006', 'IN', '22222222-2222-4222-8222-222222222206', 6, w_north, u_worker, 'Opening balance', now() - interval '30 days', demo_eco),
        ('33333333-3333-4333-8333-333333333007', 'IN', '22222222-2222-4222-8222-222222222207', 17, w_central, u_worker, 'Opening balance', now() - interval '30 days', demo_eco),
        ('33333333-3333-4333-8333-333333333008', 'IN', '22222222-2222-4222-8222-222222222208', 31, w_coastal, u_worker, 'Opening balance', now() - interval '30 days', demo_eco),
        ('33333333-3333-4333-8333-333333333009', 'IN', '22222222-2222-4222-8222-222222222209', 9, w_north, u_worker, 'Opening balance', now() - interval '30 days', demo_eco),
        ('33333333-3333-4333-8333-333333333010', 'IN', '22222222-2222-4222-8222-222222222210', 4, w_coastal, u_worker, 'Opening balance', now() - interval '30 days', demo_eco),
        ('33333333-3333-4333-8333-333333333011', 'IN', '22222222-2222-4222-8222-222222222211', 80, w_central, u_worker, 'Opening balance', now() - interval '30 days', demo_eco),
        ('33333333-3333-4333-8333-333333333012', 'IN', '22222222-2222-4222-8222-222222222212', 1, w_north, u_worker, 'Opening balance', now() - interval '30 days', demo_eco)
    ON CONFLICT (id) DO NOTHING;

    -- 8 demo movements (all movement types)
    INSERT INTO stock_movements (
        id, type, product_id, qty, from_warehouse_id, to_warehouse_id, user_id, note, created_at, ecosystem_id)
    VALUES
        ('33333333-3333-4333-8333-333333333101', 'IN', '22222222-2222-4222-8222-222222222201', 20, NULL, w_central, u_worker, 'Supplier delivery', now() - interval '0 days' + interval '8 hours', demo_eco),
        ('33333333-3333-4333-8333-333333333102', 'OUT', '22222222-2222-4222-8222-222222222202', 4, w_central, NULL, u_cashier, 'POS sale', now() - interval '0 days' + interval '10 hours', demo_eco),
        ('33333333-3333-4333-8333-333333333103', 'TRANSFER', '22222222-2222-4222-8222-222222222205', 12, w_coastal, w_central, u_manager, NULL, now() - interval '1 day' + interval '14 hours', demo_eco),
        ('33333333-3333-4333-8333-333333333104', 'ADJUSTMENT', '22222222-2222-4222-8222-222222222206', 2, w_north, NULL, u_inspector, 'Damaged units', now() - interval '1 day' + interval '16 hours', demo_eco),
        ('33333333-3333-4333-8333-333333333105', 'IN', '22222222-2222-4222-8222-222222222210', 8, NULL, w_coastal, u_worker, NULL, now() - interval '2 days' + interval '9 hours', demo_eco),
        ('33333333-3333-4333-8333-333333333106', 'OUT', '22222222-2222-4222-8222-222222222204', 3, w_central, NULL, u_cashier, NULL, now() - interval '2 days' + interval '11 hours', demo_eco),
        ('33333333-3333-4333-8333-333333333107', 'TRANSFER', '22222222-2222-4222-8222-222222222211', 20, w_central, w_north, u_manager, NULL, now() - interval '3 days' + interval '13 hours', demo_eco),
        ('33333333-3333-4333-8333-333333333108', 'ADJUSTMENT', '22222222-2222-4222-8222-222222222212', 1, NULL, w_north, u_inspector, 'Recount', now() - interval '4 days' + interval '10 hours', demo_eco)
    ON CONFLICT (id) DO NOTHING;

    -- Audit entries
    INSERT INTO audit_entries (id, user_id, action, entity, entity_id, details, created_at, ecosystem_id)
    VALUES
        ('44444444-4444-4444-8444-444444444401', u_admin, 'USER_CREATED', 'user', COALESCE(u_inactive, u_admin), 'Invited Devon Cruz', now() - interval '0 days' + interval '7 hours', demo_eco),
        ('44444444-4444-4444-8444-444444444402', u_manager, 'PRODUCT_UPDATED', 'product', '22222222-2222-4222-8222-222222222202', 'Price 52.00 → 54.50', now() - interval '0 days' + interval '9 hours', demo_eco),
        ('44444444-4444-4444-8444-444444444403', u_inspector, 'AUDIT_RUN', 'warehouse', w_north, 'Spot check, 2 variances', now() - interval '1 day' + interval '16 hours', demo_eco),
        ('44444444-4444-4444-8444-444444444404', u_worker, 'STOCK_RECEIVED', 'movement', '33333333-3333-4333-8333-333333333101', NULL, now() - interval '0 days' + interval '8 hours', demo_eco),
        ('44444444-4444-4444-8444-444444444405', u_cashier, 'TRANSACTION_COMPLETED', 'transaction', '55555555-5555-4555-8555-555555555501', NULL, now() - interval '0 days' + interval '10 hours', demo_eco),
        ('44444444-4444-4444-8444-444444444406', u_admin, 'ROLE_CHANGED', 'user', u_worker, 'worker → manager (reverted)', now() - interval '5 days' + interval '11 hours', demo_eco)
    ON CONFLICT (id) DO NOTHING;

    -- 2 sample POS transactions
    INSERT INTO transactions (id, items, subtotal, tax, total, cashier_id, status, created_at, ecosystem_id)
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
            now() - interval '0 days' + interval '10 hours',
            demo_eco
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
            now() - interval '1 day' + interval '12 hours',
            demo_eco
        )
    ON CONFLICT (id) DO NOTHING;

    -- Backfill ecosystem_id on rows inserted by the legacy function (pre-V25)
    UPDATE public.stock_movements SET ecosystem_id = demo_eco WHERE ecosystem_id IS NULL;
    UPDATE public.audit_entries SET ecosystem_id = demo_eco WHERE ecosystem_id IS NULL;
    UPDATE public.transactions SET ecosystem_id = demo_eco WHERE ecosystem_id IS NULL;

    RETURN jsonb_build_object(
        'status', 'ok',
        'ecosystem_id', demo_eco,
        'stock_movements', (SELECT count(*) FROM stock_movements WHERE ecosystem_id = demo_eco),
        'audit_entries', (SELECT count(*) FROM audit_entries WHERE ecosystem_id = demo_eco),
        'transactions', (SELECT count(*) FROM transactions WHERE ecosystem_id = demo_eco)
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.seed_demo_activity() TO service_role;
