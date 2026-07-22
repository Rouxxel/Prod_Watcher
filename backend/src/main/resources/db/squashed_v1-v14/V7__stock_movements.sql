CREATE TABLE public.stock_movements (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), ecosystem_id uuid NOT NULL REFERENCES public.ecosystems(id), type public.stock_movement_type NOT NULL,
 product_id uuid NOT NULL REFERENCES public.products(id), qty integer NOT NULL CHECK(qty>0), from_warehouse_id uuid REFERENCES public.warehouses(id), to_warehouse_id uuid REFERENCES public.warehouses(id),
 user_id uuid REFERENCES public.profiles(id) ON DELETE SET NULL, provider text, recipient text, note text, created_at timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT stock_movements_type_warehouses_check CHECK (
  (type='IN' AND to_warehouse_id IS NOT NULL AND from_warehouse_id IS NULL AND provider IS NOT NULL AND btrim(provider)<>'' AND recipient IS NULL) OR
  (type='OUT' AND from_warehouse_id IS NOT NULL AND to_warehouse_id IS NULL AND provider IS NULL AND recipient IS NOT NULL AND btrim(recipient)<>'') OR
  (type='TRANSFER' AND from_warehouse_id IS NOT NULL AND to_warehouse_id IS NOT NULL AND from_warehouse_id<>to_warehouse_id AND provider IS NULL AND recipient IS NULL) OR
  (type='ADJUSTMENT' AND provider IS NULL AND recipient IS NULL AND ((from_warehouse_id IS NOT NULL AND to_warehouse_id IS NULL) OR (to_warehouse_id IS NOT NULL AND from_warehouse_id IS NULL))));
CREATE INDEX idx_stock_movements_product_id ON public.stock_movements(product_id); CREATE INDEX idx_stock_movements_from_warehouse_id ON public.stock_movements(from_warehouse_id); CREATE INDEX idx_stock_movements_to_warehouse_id ON public.stock_movements(to_warehouse_id); CREATE INDEX idx_stock_movements_created_at ON public.stock_movements(created_at); CREATE INDEX idx_stock_movements_user_id ON public.stock_movements(user_id); CREATE INDEX idx_stock_movements_ecosystem_id ON public.stock_movements(ecosystem_id);
