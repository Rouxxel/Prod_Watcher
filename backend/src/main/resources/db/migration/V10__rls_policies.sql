-- Row Level Security for ProdWatch application tables.
--
-- Backend connection strategy (Phase 4.3):
--   The Java API on Render connects via DATABASE_URL using the Supabase postgres
--   role (or service role), which bypasses RLS. Controller-level RBAC validates
--   the Supabase JWT before mutating data.
--   Preferred future path: SET LOCAL request.jwt.claims and connect as the
--   authenticated role so Postgres policies enforce access directly.
--   SUPABASE_SERVICE_ROLE_KEY is used only for Auth Admin API (createUser, etc.),
--   not for routine inventory queries.

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.warehouses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stock_movements ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;

-- Views inherit caller permissions (PG 15+) so underlying table RLS applies.
ALTER VIEW public.inventory_balances SET (security_invoker = true);
ALTER VIEW public.product_stock_summary SET (security_invoker = true);

-- ---------------------------------------------------------------------------
-- profiles — users read own row; admin reads and updates all
-- ---------------------------------------------------------------------------

CREATE POLICY profiles_select_own_or_admin
    ON public.profiles
    FOR SELECT
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND (id = auth.uid() OR has_role(auth.uid(), 'admin'))
    );

CREATE POLICY profiles_update_admin
    ON public.profiles
    FOR UPDATE
    TO authenticated
    USING (has_role(auth.uid(), 'admin'))
    WITH CHECK (has_role(auth.uid(), 'admin'));

-- ---------------------------------------------------------------------------
-- user_roles — admin read/write; users read own roles only
-- ---------------------------------------------------------------------------

CREATE POLICY user_roles_select_own_or_admin
    ON public.user_roles
    FOR SELECT
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND (user_id = auth.uid() OR has_role(auth.uid(), 'admin'))
    );

CREATE POLICY user_roles_insert_admin
    ON public.user_roles
    FOR INSERT
    TO authenticated
    WITH CHECK (has_role(auth.uid(), 'admin'));

CREATE POLICY user_roles_update_admin
    ON public.user_roles
    FOR UPDATE
    TO authenticated
    USING (has_role(auth.uid(), 'admin'))
    WITH CHECK (has_role(auth.uid(), 'admin'));

CREATE POLICY user_roles_delete_admin
    ON public.user_roles
    FOR DELETE
    TO authenticated
    USING (has_role(auth.uid(), 'admin'));

-- ---------------------------------------------------------------------------
-- warehouses — all authenticated read; admin + warehouse_manager write
-- ---------------------------------------------------------------------------

CREATE POLICY warehouses_select_authenticated
    ON public.warehouses
    FOR SELECT
    TO authenticated
    USING (is_active_user(auth.uid()));

CREATE POLICY warehouses_insert_manager_admin
    ON public.warehouses
    FOR INSERT
    TO authenticated
    WITH CHECK (
        is_active_user(auth.uid())
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
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'warehouse_manager')
        )
    )
    WITH CHECK (
        has_role(auth.uid(), 'admin')
        OR has_role(auth.uid(), 'warehouse_manager')
    );

CREATE POLICY warehouses_delete_manager_admin
    ON public.warehouses
    FOR DELETE
    TO authenticated
    USING (
        is_active_user(auth.uid())
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'warehouse_manager')
        )
    );

-- ---------------------------------------------------------------------------
-- products — all authenticated read; worker/manager/admin create/update; admin delete
-- ---------------------------------------------------------------------------

CREATE POLICY products_select_authenticated
    ON public.products
    FOR SELECT
    TO authenticated
    USING (is_active_user(auth.uid()));

CREATE POLICY products_insert_inventory_roles
    ON public.products
    FOR INSERT
    TO authenticated
    WITH CHECK (
        is_active_user(auth.uid())
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
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'warehouse_manager')
            OR has_role(auth.uid(), 'warehouse_worker')
        )
    )
    WITH CHECK (
        has_role(auth.uid(), 'admin')
        OR has_role(auth.uid(), 'warehouse_manager')
        OR has_role(auth.uid(), 'warehouse_worker')
    );

CREATE POLICY products_delete_admin
    ON public.products
    FOR DELETE
    TO authenticated
    USING (is_active_user(auth.uid()) AND has_role(auth.uid(), 'admin'));

-- ---------------------------------------------------------------------------
-- stock_movements — inventory roles read; worker/manager/admin insert; no delete
-- ---------------------------------------------------------------------------

CREATE POLICY stock_movements_select_inventory_roles
    ON public.stock_movements
    FOR SELECT
    TO authenticated
    USING (
        is_active_user(auth.uid())
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
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'warehouse_worker')
            OR has_role(auth.uid(), 'warehouse_manager')
        )
    );

-- ---------------------------------------------------------------------------
-- audit_entries — all authenticated read; insert via backend/service role only
-- ---------------------------------------------------------------------------

CREATE POLICY audit_entries_select_authenticated
    ON public.audit_entries
    FOR SELECT
    TO authenticated
    USING (is_active_user(auth.uid()));

-- ---------------------------------------------------------------------------
-- transactions — cashier + admin insert/read; manager read
-- ---------------------------------------------------------------------------

CREATE POLICY transactions_select_cashier_admin_manager
    ON public.transactions
    FOR SELECT
    TO authenticated
    USING (
        is_active_user(auth.uid())
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
        AND (
            has_role(auth.uid(), 'admin')
            OR has_role(auth.uid(), 'cashier')
        )
    );
