-- Product image storage: paths must be scoped by ecosystem.
-- Layout: {ecosystem_id}/{product_id|draft-*}/{filename}
-- Public read unchanged (catalog URLs). Writes restricted to caller's ecosystem prefix.

-- ---------------------------------------------------------------------------
-- storage.objects RLS — replace V11 inventory policies
-- ---------------------------------------------------------------------------

DROP POLICY IF EXISTS product_images_inventory_insert ON storage.objects;
CREATE POLICY product_images_inventory_insert
    ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'product-images'
        AND public.is_active_user(auth.uid())
        AND public.current_user_ecosystem_id() IS NOT NULL
        AND split_part(name, '/', 1) = public.current_user_ecosystem_id()::text
        AND (
            public.has_role(auth.uid(), 'admin')
            OR public.has_role(auth.uid(), 'warehouse_manager')
            OR public.has_role(auth.uid(), 'warehouse_worker')
        )
    );

DROP POLICY IF EXISTS product_images_inventory_update ON storage.objects;
CREATE POLICY product_images_inventory_update
    ON storage.objects
    FOR UPDATE
    TO authenticated
    USING (
        bucket_id = 'product-images'
        AND public.is_active_user(auth.uid())
        AND public.current_user_ecosystem_id() IS NOT NULL
        AND split_part(name, '/', 1) = public.current_user_ecosystem_id()::text
        AND (
            public.has_role(auth.uid(), 'admin')
            OR public.has_role(auth.uid(), 'warehouse_manager')
            OR public.has_role(auth.uid(), 'warehouse_worker')
        )
    )
    WITH CHECK (
        bucket_id = 'product-images'
        AND public.current_user_ecosystem_id() IS NOT NULL
        AND split_part(name, '/', 1) = public.current_user_ecosystem_id()::text
        AND (
            public.has_role(auth.uid(), 'admin')
            OR public.has_role(auth.uid(), 'warehouse_manager')
            OR public.has_role(auth.uid(), 'warehouse_worker')
        )
    );

DROP POLICY IF EXISTS product_images_inventory_delete ON storage.objects;
CREATE POLICY product_images_inventory_delete
    ON storage.objects
    FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'product-images'
        AND public.is_active_user(auth.uid())
        AND public.current_user_ecosystem_id() IS NOT NULL
        AND split_part(name, '/', 1) = public.current_user_ecosystem_id()::text
        AND (
            public.has_role(auth.uid(), 'admin')
            OR public.has_role(auth.uid(), 'warehouse_manager')
            OR public.has_role(auth.uid(), 'warehouse_worker')
        )
    );

-- product_images_public_read (V11) unchanged — public catalog URLs
