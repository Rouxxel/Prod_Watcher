-- NOT NULL + FK enforcement; per-ecosystem uniqueness (SKU, workspace settings).

ALTER TABLE public.profiles
    ALTER COLUMN ecosystem_id SET NOT NULL;

ALTER TABLE public.warehouses
    ALTER COLUMN ecosystem_id SET NOT NULL;

ALTER TABLE public.products
    ALTER COLUMN ecosystem_id SET NOT NULL;

ALTER TABLE public.stock_movements
    ALTER COLUMN ecosystem_id SET NOT NULL;

ALTER TABLE public.transactions
    ALTER COLUMN ecosystem_id SET NOT NULL;

ALTER TABLE public.audit_entries
    ALTER COLUMN ecosystem_id SET NOT NULL;

ALTER TABLE public.workspace_settings
    ALTER COLUMN ecosystem_id SET NOT NULL;

-- SKU unique per ecosystem, not globally.
ALTER TABLE public.products DROP CONSTRAINT IF EXISTS products_sku_key;
CREATE UNIQUE INDEX idx_products_ecosystem_sku ON public.products (ecosystem_id, sku);

-- One settings row per ecosystem (replaces V18 singleton check).
ALTER TABLE public.workspace_settings DROP CONSTRAINT IF EXISTS workspace_settings_singleton;
CREATE UNIQUE INDEX idx_workspace_settings_ecosystem_id_unique ON public.workspace_settings (ecosystem_id);
