-- Computed inventory balances (regular view — evaluated at query time for MVP).
-- ADJUSTMENT direction: from_warehouse_id = decrease, to_warehouse_id = increase.

CREATE OR REPLACE VIEW public.inventory_balances AS
SELECT
    product_id,
    warehouse_id,
    SUM(delta)::integer AS quantity
FROM (
    SELECT product_id, to_warehouse_id AS warehouse_id, qty AS delta
    FROM public.stock_movements
    WHERE type = 'IN'

    UNION ALL

    SELECT product_id, from_warehouse_id, -qty
    FROM public.stock_movements
    WHERE type = 'OUT'

    UNION ALL

    SELECT product_id, from_warehouse_id, -qty
    FROM public.stock_movements
    WHERE type = 'TRANSFER'

    UNION ALL

    SELECT product_id, to_warehouse_id, qty
    FROM public.stock_movements
    WHERE type = 'TRANSFER'

    UNION ALL

    SELECT product_id, from_warehouse_id, -qty
    FROM public.stock_movements
    WHERE type = 'ADJUSTMENT'
      AND from_warehouse_id IS NOT NULL

    UNION ALL

    SELECT product_id, to_warehouse_id, qty
    FROM public.stock_movements
    WHERE type = 'ADJUSTMENT'
      AND to_warehouse_id IS NOT NULL
) AS deltas
GROUP BY product_id, warehouse_id;

-- Product stock at default warehouse (maps to frontend Product.stock).
CREATE OR REPLACE VIEW public.product_stock_summary AS
SELECT
    p.id AS product_id,
    p.default_warehouse_id AS warehouse_id,
    COALESCE(ib.quantity, 0)::integer AS quantity
FROM public.products p
LEFT JOIN public.inventory_balances ib
    ON ib.product_id = p.id
   AND ib.warehouse_id = p.default_warehouse_id;

GRANT SELECT ON public.inventory_balances TO authenticated, service_role;
GRANT SELECT ON public.product_stock_summary TO authenticated, service_role;
