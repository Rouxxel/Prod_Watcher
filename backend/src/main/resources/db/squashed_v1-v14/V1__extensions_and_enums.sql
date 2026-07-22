-- ProdWatch: shared enum types (mirrors frontend/src/types/index.ts)
-- UUIDs use gen_random_uuid() (PostgreSQL 13+; no extension required).

CREATE TYPE public.app_role AS ENUM (
    'admin',
    'warehouse_worker',
    'warehouse_manager',
    'inspector',
    'cashier'
);

CREATE TYPE public.stock_movement_type AS ENUM (
    'IN',
    'OUT',
    'TRANSFER',
    'ADJUSTMENT'
);

CREATE TYPE public.transaction_status AS ENUM (
    'completed',
    'refunded',
    'void'
);
