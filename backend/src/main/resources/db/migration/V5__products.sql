-- Product catalog. Stock quantity is derived from stock_movements (see inventory_balances view).

CREATE TABLE public.products (
    id                    uuid           PRIMARY KEY DEFAULT gen_random_uuid(),
    name                  text           NOT NULL,
    sku                   text           NOT NULL UNIQUE,
    category              text           NOT NULL,
    price                 numeric(12, 2) NOT NULL CHECK (price >= 0),
    default_warehouse_id  uuid           NOT NULL REFERENCES public.warehouses (id),
    low_stock_threshold   integer        NOT NULL DEFAULT 0 CHECK (low_stock_threshold >= 0),
    images                text[]         NOT NULL DEFAULT '{}',
    created_at            timestamptz    NOT NULL DEFAULT now(),
    updated_at            timestamptz    NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_sku ON public.products (sku);
CREATE INDEX idx_products_category ON public.products (category);
CREATE INDEX idx_products_default_warehouse_id ON public.products (default_warehouse_id);

CREATE TRIGGER products_set_updated_at
    BEFORE UPDATE ON public.products
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();
