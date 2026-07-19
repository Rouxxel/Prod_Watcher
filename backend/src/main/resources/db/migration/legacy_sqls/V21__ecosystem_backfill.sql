-- Backfill: one demo ecosystem for all pre-existing rows (Alex seed + V12 data).
-- Fixed id for scripts/docs reference; new sign-ups get a new id in Phase 3.

INSERT INTO public.ecosystems (id, name)
VALUES ('33333333-3333-4333-8333-333333333301', 'Acme Demo')
ON CONFLICT (id) DO NOTHING;

UPDATE public.profiles
SET ecosystem_id = '33333333-3333-4333-8333-333333333301'
WHERE ecosystem_id IS NULL;

UPDATE public.warehouses
SET ecosystem_id = '33333333-3333-4333-8333-333333333301'
WHERE ecosystem_id IS NULL;

UPDATE public.products
SET ecosystem_id = '33333333-3333-4333-8333-333333333301'
WHERE ecosystem_id IS NULL;

UPDATE public.stock_movements
SET ecosystem_id = '33333333-3333-4333-8333-333333333301'
WHERE ecosystem_id IS NULL;

UPDATE public.transactions
SET ecosystem_id = '33333333-3333-4333-8333-333333333301'
WHERE ecosystem_id IS NULL;

UPDATE public.audit_entries
SET ecosystem_id = '33333333-3333-4333-8333-333333333301'
WHERE ecosystem_id IS NULL;

UPDATE public.workspace_settings
SET ecosystem_id = '33333333-3333-4333-8333-333333333301'
WHERE ecosystem_id IS NULL;
