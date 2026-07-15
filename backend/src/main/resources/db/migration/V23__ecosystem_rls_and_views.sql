-- Ecosystem-scoped RLS + helper; views join on matching ecosystem_id.
-- Java API still bypasses RLS via postgres role — defense in depth for direct Supabase access.

-- ---------------------------------------------------------------------------
-- Helper — caller's ecosystem from profiles
-- ---------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.current_user_ecosystem_id()
RETURNS uuid
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    _ecosystem_id uuid;
BEGIN
    SELECT ecosystem_id
    INTO _ecosystem_id
    FROM public.profiles
    WHERE id = auth.uid();

    RETURN _ecosystem_id;
END;
$$;

GRANT EXECUTE ON FUNCTION public.current_user_ecosystem_id() TO authenticated, service_role;

-- ---------------------------------------------------------------------------
-- ecosystems — users read own ecosystem only
-- ---------------------------------------------------------------------------

ALTER TABLE public.ecosystems ENABLE ROW LEVEL SECURITY;

CREATE POLICY ecosystems_select_own
    ON public.ecosystems
    FOR SELECT
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND id = current_user_ecosystem_id()
    );

-- ---------------------------------------------------------------------------
-- profiles
-- ---------------------------------------------------------------------------

DROP POLICY IF EXISTS profiles_select_own_or_admin ON public.profiles;
DROP POLICY IF EXISTS profiles_update_admin ON public.profiles;

CREATE POLICY profiles_select_own_or_admin
    ON public.profiles
    FOR SELECT
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
        AND (id = auth.uid() OR has_role(auth.uid(), 'admin'))
    );

CREATE POLICY profiles_update_admin
    ON public.profiles
    FOR UPDATE
    TO authenticated
    USING (
        has_role(auth.uid(), 'admin')
        AND ecosystem_id = current_user_ecosystem_id()
    )
    WITH CHECK (
        has_role(auth.uid(), 'admin')
        AND ecosystem_id = current_user_ecosystem_id()
    );

-- ---------------------------------------------------------------------------
-- user_roles — same ecosystem only
-- ---------------------------------------------------------------------------

DROP POLICY IF EXISTS user_roles_select_own_or_admin ON public.user_roles;
DROP POLICY IF EXISTS user_roles_insert_admin ON public.user_roles;
DROP POLICY IF EXISTS user_roles_update_admin ON public.user_roles;
DROP POLICY IF EXISTS user_roles_delete_admin ON public.user_roles;

CREATE POLICY user_roles_select_own_or_admin
    ON public.user_roles
    FOR SELECT
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND EXISTS (
            SELECT 1
            FROM public.profiles p
            WHERE p.id = user_roles.user_id
              AND p.ecosystem_id = current_user_ecosystem_id()
        )
        AND (user_id = auth.uid() OR has_role(auth.uid(), 'admin'))
    );

CREATE POLICY user_roles_insert_admin
    ON public.user_roles
    FOR INSERT
    TO authenticated
    WITH CHECK (
        has_role(auth.uid(), 'admin')
        AND EXISTS (
            SELECT 1
            FROM public.profiles p
            WHERE p.id = user_roles.user_id
              AND p.ecosystem_id = current_user_ecosystem_id()
        )
    );

CREATE POLICY user_roles_update_admin
    ON public.user_roles
    FOR UPDATE
    TO authenticated
    USING (
        has_role(auth.uid(), 'admin')
        AND EXISTS (
            SELECT 1
            FROM public.profiles p
            WHERE p.id = user_roles.user_id
              AND p.ecosystem_id = current_user_ecosystem_id()
        )
    )
    WITH CHECK (
        has_role(auth.uid(), 'admin')
        AND EXISTS (
            SELECT 1
            FROM public.profiles p
            WHERE p.id = user_roles.user_id
              AND p.ecosystem_id = current_user_ecosystem_id()
        )
    );

CREATE POLICY user_roles_delete_admin
    ON public.user_roles
    FOR DELETE
    TO authenticated
    USING (
        has_role(auth.uid(), 'admin')
        AND EXISTS (
            SELECT 1
            FROM public.profiles p
            WHERE p.id = user_roles.user_id
              AND p.ecosystem_id = current_user_ecosystem_id()
        )
    );

-- ---------------------------------------------------------------------------
-- warehouses
-- ---------------------------------------------------------------------------

DROP POLICY IF EXISTS warehouses_select_authenticated ON public.warehouses;
DROP POLICY IF EXISTS warehouses_insert_manager_admin ON public.warehouses;
DROP POLICY IF EXISTS warehouses_update_manager_admin ON public.warehouses;
DROP POLICY IF EXISTS warehouses_delete_manager_admin ON public.warehouses;

CREATE POLICY warehouses_select_authenticated
    ON public.warehouses
    FOR SELECT
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
    );

CREATE POLICY warehouses_insert_manager_admin
    ON public.warehouses
    FOR INSERT
    TO authenticated
    WITH CHECK (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'warehouse_manager')
        )
    );

CREATE POLICY warehouses_update_manager_admin
    ON public.warehouses
    FOR UPDATE
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'warehouse_manager')
        )
    )
    WITH CHECK (
        ecosystem_id = current_user_ecosystem_id()
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'warehouse_manager')
        )
    );

CREATE POLICY warehouses_delete_manager_admin
    ON public.warehouses
    FOR DELETE
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'warehouse_manager')
        )
    );

-- ---------------------------------------------------------------------------
-- products
-- ---------------------------------------------------------------------------

DROP POLICY IF EXISTS products_select_authenticated ON public.products;
DROP POLICY IF EXISTS products_insert_inventory_roles ON public.products;
DROP POLICY IF EXISTS products_update_inventory_roles ON public.products;
DROP POLICY IF EXISTS products_delete_admin ON public.products;

CREATE POLICY products_select_authenticated
    ON public.products
    FOR SELECT
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
    );

CREATE POLICY products_insert_inventory_roles
    ON public.products
    FOR INSERT
    TO authenticated
    WITH CHECK (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'warehouse_manager')
            OR has_role(auth.uid(), 'warehouse_worker')
        )
    );

CREATE POLICY products_update_inventory_roles
    ON public.products
    FOR UPDATE
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'warehouse_manager')
            OR has_role(auth.uid(), 'warehouse_worker')
        )
    )
    WITH CHECK (
        ecosystem_id = current_user_ecosystem_id()
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'warehouse_manager')
            OR has_role(auth.uid(), 'warehouse_worker')
        )
    );

CREATE POLICY products_delete_admin
    ON public.products
    FOR DELETE
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
        AND has_role(auth.uid(), 'admin')
    );

-- ---------------------------------------------------------------------------
-- stock_movements
-- ---------------------------------------------------------------------------

DROP POLICY IF EXISTS stock_movements_select_inventory_roles ON public.stock_movements;
DROP POLICY IF EXISTS stock_movements_insert_inventory_roles ON public.stock_movements;

CREATE POLICY stock_movements_select_inventory_roles
    ON public.stock_movements
    FOR SELECT
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'warehouse_worker')
            OR has_role(auth.uid(), 'warehouse_manager')
            OR has_role(auth.uid(), 'inspector')
        )
    );

CREATE POLICY stock_movements_insert_inventory_roles
    ON public.stock_movements
    FOR INSERT
    TO authenticated
    WITH CHECK (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'warehouse_worker')
            OR has_role(auth.uid(), 'warehouse_manager')
        )
    );

-- ---------------------------------------------------------------------------
-- audit_entries
-- ---------------------------------------------------------------------------

DROP POLICY IF EXISTS audit_entries_select_authenticated ON public.audit_entries;

CREATE POLICY audit_entries_select_authenticated
    ON public.audit_entries
    FOR SELECT
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
    );

-- ---------------------------------------------------------------------------
-- transactions
-- ---------------------------------------------------------------------------

DROP POLICY IF EXISTS transactions_select_cashier_admin_manager ON public.transactions;
DROP POLICY IF EXISTS transactions_insert_cashier_admin ON public.transactions;

CREATE POLICY transactions_select_cashier_admin_manager
    ON public.transactions
    FOR SELECT
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'cashier')
            OR has_role(auth.uid(), 'warehouse_manager')
        )
    );

CREATE POLICY transactions_insert_cashier_admin
    ON public.transactions
    FOR INSERT
    TO authenticated
    WITH CHECK (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'cashier')
        )
    );

-- ---------------------------------------------------------------------------
-- workspace_settings
-- ---------------------------------------------------------------------------

DROP POLICY IF EXISTS workspace_settings_select_authenticated ON public.workspace_settings;
DROP POLICY IF EXISTS workspace_settings_update_admin ON public.workspace_settings;

CREATE POLICY workspace_settings_select_authenticated
    ON public.workspace_settings
    FOR SELECT
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND ecosystem_id = current_user_ecosystem_id()
    );

CREATE POLICY workspace_settings_update_admin
    ON public.workspace_settings
    FOR UPDATE
    TO authenticated
    USING (
        has_role(auth.uid(), 'admin')
        AND ecosystem_id = current_user_ecosystem_id()
    )
    WITH CHECK (
        has_role(auth.uid(), 'admin')
        AND ecosystem_id = current_user_ecosystem_id()
    );

-- ---------------------------------------------------------------------------
-- Views — aggregate within ecosystem (matching product/warehouse tenant)
-- PostgreSQL cannot add/reorder view columns via CREATE OR REPLACE; drop first.
-- ---------------------------------------------------------------------------

DROP VIEW IF EXISTS public.product_stock_summary;
DROP VIEW IF EXISTS public.inventory_balances;

CREATE VIEW public.inventory_balances AS
SELECT
    sm.ecosystem_id,
    sm.product_id,
    sm.warehouse_id,
    SUM(sm.delta)::integer AS quantity
FROM (
    SELECT ecosystem_id, product_id, to_warehouse_id AS warehouse_id, qty AS delta
    FROM public.stock_movements
    WHERE type = 'IN'

    UNION ALL

    SELECT ecosystem_id, product_id, from_warehouse_id, -qty
    FROM public.stock_movements
    WHERE type = 'OUT'

    UNION ALL

    SELECT ecosystem_id, product_id, from_warehouse_id, -qty
    FROM public.stock_movements
    WHERE type = 'TRANSFER'

    UNION ALL

    SELECT ecosystem_id, product_id, to_warehouse_id, qty
    FROM public.stock_movements
    WHERE type = 'TRANSFER'

    UNION ALL

    SELECT ecosystem_id, product_id, from_warehouse_id, -qty
    FROM public.stock_movements
    WHERE type = 'ADJUSTMENT'
      AND from_warehouse_id IS NOT NULL

    UNION ALL

    SELECT ecosystem_id, product_id, to_warehouse_id, qty
    FROM public.stock_movements
    WHERE type = 'ADJUSTMENT'
      AND to_warehouse_id IS NOT NULL
) AS sm
GROUP BY sm.ecosystem_id, sm.product_id, sm.warehouse_id;

CREATE VIEW public.product_stock_summary AS
SELECT
    p.id AS product_id,
    p.default_warehouse_id AS warehouse_id,
    COALESCE(ib.quantity, 0)::integer AS quantity
FROM public.products p
LEFT JOIN public.inventory_balances ib
    ON ib.product_id = p.id
   AND ib.warehouse_id = p.default_warehouse_id
   AND ib.ecosystem_id = p.ecosystem_id;

GRANT SELECT ON public.inventory_balances TO authenticated, service_role;
GRANT SELECT ON public.product_stock_summary TO authenticated, service_role;

ALTER VIEW public.inventory_balances SET (security_invoker = true);
ALTER VIEW public.product_stock_summary SET (security_invoker = true);
