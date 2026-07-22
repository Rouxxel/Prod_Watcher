-- Supabase Storage: product-images bucket (public read, inventory-role write).
-- Public URL pattern: {SUPABASE_URL}/storage/v1/object/public/product-images/{path}
-- Path prefix enforced per ecosystem in V26__storage_ecosystem_paths.sql.
-- Seed products still use external picsum URLs until images are uploaded to this bucket.

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'product-images',
    'product-images',
    true,
    5242880,
    ARRAY['image/jpeg', 'image/png', 'image/webp', 'image/gif']
)
ON CONFLICT (id) DO UPDATE
SET
    public = EXCLUDED.public,
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

-- ---------------------------------------------------------------------------
-- storage.objects RLS (bucket policies)
-- ---------------------------------------------------------------------------

DROP POLICY IF EXISTS product_images_public_read ON storage.objects;
CREATE POLICY product_images_public_read
    ON storage.objects
    FOR SELECT
    TO public
    USING (bucket_id = 'product-images');

DROP POLICY IF EXISTS product_images_inventory_insert ON storage.objects;
CREATE POLICY product_images_inventory_insert
    ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'product-images'
        AND public.is_active_user(auth.uid())
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
        AND (
            public.has_role(auth.uid(), 'admin')
            OR public.has_role(auth.uid(), 'warehouse_manager')
            OR public.has_role(auth.uid(), 'warehouse_worker')
        )
    )
    WITH CHECK (
        bucket_id = 'product-images'
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
        AND (
            public.has_role(auth.uid(), 'admin')
            OR public.has_role(auth.uid(), 'warehouse_manager')
            OR public.has_role(auth.uid(), 'warehouse_worker')
        )
    );
