-- External supplier for IN movements. Internal warehouse transfers still use from_warehouse_id.
ALTER TABLE public.stock_movements
    ADD COLUMN provider text;

UPDATE public.stock_movements
SET provider = COALESCE(NULLIF(btrim(note), ''), 'Unknown supplier')
WHERE type = 'IN'
  AND provider IS NULL;

ALTER TABLE public.stock_movements
    DROP CONSTRAINT stock_movements_type_warehouses_check;

ALTER TABLE public.stock_movements
    ADD CONSTRAINT stock_movements_type_warehouses_check CHECK (
        (
            type = 'IN'
            AND to_warehouse_id IS NOT NULL
            AND from_warehouse_id IS NULL
            AND provider IS NOT NULL
            AND btrim(provider) <> ''
        )
        OR (
            type = 'OUT'
            AND from_warehouse_id IS NOT NULL
            AND to_warehouse_id IS NULL
            AND provider IS NULL
        )
        OR (
            type = 'TRANSFER'
            AND from_warehouse_id IS NOT NULL
            AND to_warehouse_id IS NOT NULL
            AND from_warehouse_id <> to_warehouse_id
            AND provider IS NULL
        )
        OR (
            type = 'ADJUSTMENT'
            AND provider IS NULL
            AND (
                (from_warehouse_id IS NOT NULL AND to_warehouse_id IS NULL)
                OR (to_warehouse_id IS NOT NULL AND from_warehouse_id IS NULL)
            )
        )
    );
