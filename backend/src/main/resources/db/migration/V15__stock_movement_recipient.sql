-- External recipient for OUT movements. Internal warehouse transfers still use to_warehouse_id.
ALTER TABLE public.stock_movements
    ADD COLUMN recipient text;

UPDATE public.stock_movements
SET recipient = COALESCE(NULLIF(btrim(note), ''), 'Unknown recipient')
WHERE type = 'OUT'
  AND recipient IS NULL;

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
            AND recipient IS NULL
        )
        OR (
            type = 'OUT'
            AND from_warehouse_id IS NOT NULL
            AND to_warehouse_id IS NULL
            AND provider IS NULL
            AND recipient IS NOT NULL
            AND btrim(recipient) <> ''
        )
        OR (
            type = 'TRANSFER'
            AND from_warehouse_id IS NOT NULL
            AND to_warehouse_id IS NOT NULL
            AND from_warehouse_id <> to_warehouse_id
            AND provider IS NULL
            AND recipient IS NULL
        )
        OR (
            type = 'ADJUSTMENT'
            AND provider IS NULL
            AND recipient IS NULL
            AND (
                (from_warehouse_id IS NOT NULL AND to_warehouse_id IS NULL)
                OR (to_warehouse_id IS NOT NULL AND from_warehouse_id IS NULL)
            )
        )
    );
