-- Canonical greenfield reference for the post-V26 ProdWatch schema.
-- Phase 1 deliverable only: this file is deliberately outside Flyway's active path.
-- It is split into V1..V14 during Phase 2. Do not apply to an existing V1..V26 database.

CREATE TYPE public.app_role AS ENUM ('admin','warehouse_worker','warehouse_manager','inspector','cashier');
CREATE TYPE public.stock_movement_type AS ENUM ('IN','OUT','TRANSFER','ADJUSTMENT');
CREATE TYPE public.transaction_status AS ENUM ('completed','refunded','void_');

CREATE OR REPLACE FUNCTION public.set_updated_at() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END;
$$;

CREATE TABLE public.ecosystems (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name text NOT NULL DEFAULT '',
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX idx_ecosystems_name ON public.ecosystems(name);
CREATE TRIGGER ecosystems_set_updated_at BEFORE UPDATE ON public.ecosystems FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE public.profiles (
  id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  name text NOT NULL, email text NOT NULL UNIQUE, active boolean NOT NULL DEFAULT true,
  ecosystem_id uuid REFERENCES public.ecosystems(id),
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX idx_profiles_ecosystem_id ON public.profiles(ecosystem_id);
CREATE TABLE public.user_roles (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), user_id uuid NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
  role public.app_role NOT NULL, UNIQUE(user_id, role));
CREATE INDEX idx_user_roles_user_id ON public.user_roles(user_id);
CREATE INDEX idx_user_roles_role ON public.user_roles(role);
CREATE TRIGGER profiles_set_updated_at BEFORE UPDATE ON public.profiles FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE OR REPLACE FUNCTION public.handle_new_user() RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN INSERT INTO public.profiles(id,name,email) VALUES (NEW.id, COALESCE(NULLIF(trim(NEW.raw_user_meta_data->>'name'),''),NULLIF(trim(NEW.raw_user_meta_data->>'full_name'),''),split_part(NEW.email,'@',1)),NEW.email); RETURN NEW; END;
$$;
CREATE TRIGGER on_auth_user_created AFTER INSERT ON auth.users FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
CREATE OR REPLACE FUNCTION public.bootstrap_assign_admin(_user_id uuid) RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN IF NOT EXISTS (SELECT 1 FROM public.user_roles WHERE role='admin') THEN INSERT INTO public.user_roles(user_id,role) VALUES (_user_id,'admin') ON CONFLICT(user_id,role) DO NOTHING; END IF; END;
$$;
GRANT EXECUTE ON FUNCTION public.bootstrap_assign_admin(uuid) TO service_role;

CREATE TABLE public.warehouses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), ecosystem_id uuid NOT NULL REFERENCES public.ecosystems(id),
  name text NOT NULL, location text NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX idx_warehouses_name ON public.warehouses(name); CREATE INDEX idx_warehouses_ecosystem_id ON public.warehouses(ecosystem_id);
CREATE TRIGGER warehouses_set_updated_at BEFORE UPDATE ON public.warehouses FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE TABLE public.products (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), ecosystem_id uuid NOT NULL REFERENCES public.ecosystems(id),
  name text NOT NULL, sku text NOT NULL, category text NOT NULL, price numeric(12,2) NOT NULL CHECK(price >= 0),
  default_warehouse_id uuid NOT NULL REFERENCES public.warehouses(id), low_stock_threshold integer NOT NULL DEFAULT 0 CHECK(low_stock_threshold >= 0),
  images text[] NOT NULL DEFAULT '{}', created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(), UNIQUE(ecosystem_id,sku));
CREATE INDEX idx_products_sku ON public.products(sku); CREATE INDEX idx_products_category ON public.products(category); CREATE INDEX idx_products_default_warehouse_id ON public.products(default_warehouse_id); CREATE INDEX idx_products_ecosystem_id ON public.products(ecosystem_id);
CREATE TRIGGER products_set_updated_at BEFORE UPDATE ON public.products FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

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

CREATE TABLE public.audit_entries (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), ecosystem_id uuid NOT NULL REFERENCES public.ecosystems(id), user_id uuid REFERENCES public.profiles(id) ON DELETE SET NULL,
 action text NOT NULL, entity text NOT NULL, entity_id uuid NOT NULL, entity_label text, details text, created_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX idx_audit_entries_entity ON public.audit_entries(entity); CREATE INDEX idx_audit_entries_entity_id ON public.audit_entries(entity_id); CREATE INDEX idx_audit_entries_created_at ON public.audit_entries(created_at); CREATE INDEX idx_audit_entries_user_id ON public.audit_entries(user_id); CREATE INDEX idx_audit_entries_ecosystem_id ON public.audit_entries(ecosystem_id);

CREATE TABLE public.transactions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), ecosystem_id uuid NOT NULL REFERENCES public.ecosystems(id), items jsonb NOT NULL CHECK(jsonb_typeof(items)='array'),
 subtotal numeric(12,2) NOT NULL CHECK(subtotal>=0), tax numeric(12,2) NOT NULL CHECK(tax>=0), total numeric(12,2) NOT NULL CHECK(total>=0),
 cashier_id uuid NOT NULL REFERENCES public.profiles(id), status public.transaction_status NOT NULL DEFAULT 'completed', created_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX idx_transactions_cashier_id ON public.transactions(cashier_id); CREATE INDEX idx_transactions_created_at ON public.transactions(created_at); CREATE INDEX idx_transactions_status ON public.transactions(status); CREATE INDEX idx_transactions_ecosystem_id ON public.transactions(ecosystem_id);

CREATE TABLE public.workspace_settings (
 id uuid PRIMARY KEY DEFAULT '00000000-0000-4000-8000-000000000001'::uuid, ecosystem_id uuid NOT NULL REFERENCES public.ecosystems(id),
 business_name text NOT NULL DEFAULT '', contact_email text NOT NULL DEFAULT '', tax_rate numeric(6,4) NOT NULL DEFAULT .16, tax_label text NOT NULL DEFAULT 'Tax', receipt_footer text, receipt_logo_url text, business_mode text NOT NULL DEFAULT 'auto', updated_at timestamptz NOT NULL DEFAULT now(), updated_by uuid REFERENCES public.profiles(id) ON DELETE SET NULL,
 CONSTRAINT workspace_settings_tax_rate_check CHECK(tax_rate>=0 AND tax_rate<=1), CONSTRAINT workspace_settings_business_mode_check CHECK(business_mode IN ('auto','single','multi')), UNIQUE(ecosystem_id));
CREATE INDEX idx_workspace_settings_ecosystem_id ON public.workspace_settings(ecosystem_id);
CREATE TRIGGER workspace_settings_set_updated_at BEFORE UPDATE ON public.workspace_settings FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

CREATE VIEW public.inventory_balances AS
SELECT sm.ecosystem_id,sm.product_id,sm.warehouse_id,SUM(sm.delta)::integer quantity FROM (
 SELECT ecosystem_id,product_id,to_warehouse_id warehouse_id,qty delta FROM public.stock_movements WHERE type='IN'
 UNION ALL SELECT ecosystem_id,product_id,from_warehouse_id,-qty FROM public.stock_movements WHERE type='OUT'
 UNION ALL SELECT ecosystem_id,product_id,from_warehouse_id,-qty FROM public.stock_movements WHERE type='TRANSFER'
 UNION ALL SELECT ecosystem_id,product_id,to_warehouse_id,qty FROM public.stock_movements WHERE type='TRANSFER'
 UNION ALL SELECT ecosystem_id,product_id,from_warehouse_id,-qty FROM public.stock_movements WHERE type='ADJUSTMENT' AND from_warehouse_id IS NOT NULL
 UNION ALL SELECT ecosystem_id,product_id,to_warehouse_id,qty FROM public.stock_movements WHERE type='ADJUSTMENT' AND to_warehouse_id IS NOT NULL) sm GROUP BY sm.ecosystem_id,sm.product_id,sm.warehouse_id;
CREATE VIEW public.product_stock_summary AS SELECT p.id product_id,p.default_warehouse_id warehouse_id,COALESCE(ib.quantity,0)::integer quantity FROM public.products p LEFT JOIN public.inventory_balances ib ON ib.product_id=p.id AND ib.warehouse_id=p.default_warehouse_id AND ib.ecosystem_id=p.ecosystem_id;
GRANT SELECT ON public.inventory_balances, public.product_stock_summary TO authenticated, service_role;

CREATE OR REPLACE FUNCTION public.has_role(_user_id uuid,_role public.app_role) RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path=public AS $$ SELECT EXISTS(SELECT 1 FROM public.user_roles WHERE user_id=_user_id AND role=_role) $$;
CREATE OR REPLACE FUNCTION public.current_user_role() RETURNS public.app_role LANGUAGE sql STABLE SECURITY DEFINER SET search_path=public AS $$ SELECT role FROM public.user_roles WHERE user_id=auth.uid() LIMIT 1 $$;
CREATE OR REPLACE FUNCTION public.is_active_user(_user_id uuid) RETURNS boolean LANGUAGE sql STABLE SECURITY DEFINER SET search_path=public AS $$ SELECT EXISTS(SELECT 1 FROM public.profiles WHERE id=_user_id AND active) $$;
CREATE OR REPLACE FUNCTION public.current_user_ecosystem_id() RETURNS uuid LANGUAGE plpgsql STABLE SECURITY DEFINER SET search_path=public AS $$ DECLARE _ecosystem_id uuid; BEGIN SELECT ecosystem_id INTO _ecosystem_id FROM public.profiles WHERE id=auth.uid(); RETURN _ecosystem_id; END; $$;
GRANT EXECUTE ON FUNCTION public.has_role(uuid,public.app_role), public.current_user_role(), public.is_active_user(uuid), public.current_user_ecosystem_id() TO authenticated,service_role;

ALTER TABLE public.ecosystems ENABLE ROW LEVEL SECURITY; ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY; ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY; ALTER TABLE public.warehouses ENABLE ROW LEVEL SECURITY; ALTER TABLE public.products ENABLE ROW LEVEL SECURITY; ALTER TABLE public.stock_movements ENABLE ROW LEVEL SECURITY; ALTER TABLE public.audit_entries ENABLE ROW LEVEL SECURITY; ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY; ALTER TABLE public.workspace_settings ENABLE ROW LEVEL SECURITY;
-- Final V23 policies. has_role is intentionally retained as the shipped role gate.
CREATE POLICY ecosystems_select_own ON public.ecosystems FOR SELECT TO authenticated USING(is_active_user(auth.uid()) AND id=current_user_ecosystem_id());
CREATE POLICY profiles_select_own_or_admin ON public.profiles FOR SELECT TO authenticated USING(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id() AND (id=auth.uid() OR has_role(auth.uid(),'admin')));
CREATE POLICY profiles_update_admin ON public.profiles FOR UPDATE TO authenticated USING(has_role(auth.uid(),'admin') AND ecosystem_id=current_user_ecosystem_id()) WITH CHECK(has_role(auth.uid(),'admin') AND ecosystem_id=current_user_ecosystem_id());
CREATE POLICY user_roles_select_own_or_admin ON public.user_roles FOR SELECT TO authenticated USING(is_active_user(auth.uid()) AND EXISTS(SELECT 1 FROM public.profiles p WHERE p.id=user_roles.user_id AND p.ecosystem_id=current_user_ecosystem_id()) AND (user_id=auth.uid() OR has_role(auth.uid(),'admin')));
CREATE POLICY user_roles_insert_admin ON public.user_roles FOR INSERT TO authenticated WITH CHECK(has_role(auth.uid(),'admin') AND EXISTS(SELECT 1 FROM public.profiles p WHERE p.id=user_roles.user_id AND p.ecosystem_id=current_user_ecosystem_id()));
CREATE POLICY user_roles_update_admin ON public.user_roles FOR UPDATE TO authenticated USING(has_role(auth.uid(),'admin') AND EXISTS(SELECT 1 FROM public.profiles p WHERE p.id=user_roles.user_id AND p.ecosystem_id=current_user_ecosystem_id())) WITH CHECK(has_role(auth.uid(),'admin') AND EXISTS(SELECT 1 FROM public.profiles p WHERE p.id=user_roles.user_id AND p.ecosystem_id=current_user_ecosystem_id()));
CREATE POLICY user_roles_delete_admin ON public.user_roles FOR DELETE TO authenticated USING(has_role(auth.uid(),'admin') AND EXISTS(SELECT 1 FROM public.profiles p WHERE p.id=user_roles.user_id AND p.ecosystem_id=current_user_ecosystem_id()));
CREATE POLICY warehouses_select_authenticated ON public.warehouses FOR SELECT TO authenticated USING(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id());
CREATE POLICY warehouses_insert_manager_admin ON public.warehouses FOR INSERT TO authenticated WITH CHECK(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id() AND (has_role(auth.uid(),'admin') OR has_role(auth.uid(),'warehouse_manager')));
CREATE POLICY warehouses_update_manager_admin ON public.warehouses FOR UPDATE TO authenticated USING(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id() AND (has_role(auth.uid(),'admin') OR has_role(auth.uid(),'warehouse_manager'))) WITH CHECK(ecosystem_id=current_user_ecosystem_id() AND (has_role(auth.uid(),'admin') OR has_role(auth.uid(),'warehouse_manager')));
CREATE POLICY warehouses_delete_manager_admin ON public.warehouses FOR DELETE TO authenticated USING(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id() AND (has_role(auth.uid(),'admin') OR has_role(auth.uid(),'warehouse_manager')));
CREATE POLICY products_select_authenticated ON public.products FOR SELECT TO authenticated USING(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id());
CREATE POLICY products_insert_inventory_roles ON public.products FOR INSERT TO authenticated WITH CHECK(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id() AND (has_role(auth.uid(),'admin') OR has_role(auth.uid(),'warehouse_manager') OR has_role(auth.uid(),'warehouse_worker')));
CREATE POLICY products_update_inventory_roles ON public.products FOR UPDATE TO authenticated USING(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id() AND (has_role(auth.uid(),'admin') OR has_role(auth.uid(),'warehouse_manager') OR has_role(auth.uid(),'warehouse_worker'))) WITH CHECK(ecosystem_id=current_user_ecosystem_id() AND (has_role(auth.uid(),'admin') OR has_role(auth.uid(),'warehouse_manager') OR has_role(auth.uid(),'warehouse_worker')));
CREATE POLICY products_delete_admin ON public.products FOR DELETE TO authenticated USING(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id() AND has_role(auth.uid(),'admin'));
CREATE POLICY stock_movements_select_inventory_roles ON public.stock_movements FOR SELECT TO authenticated USING(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id() AND (has_role(auth.uid(),'admin') OR has_role(auth.uid(),'warehouse_worker') OR has_role(auth.uid(),'warehouse_manager') OR has_role(auth.uid(),'inspector')));
CREATE POLICY stock_movements_insert_inventory_roles ON public.stock_movements FOR INSERT TO authenticated WITH CHECK(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id() AND (has_role(auth.uid(),'admin') OR has_role(auth.uid(),'warehouse_worker') OR has_role(auth.uid(),'warehouse_manager')));
CREATE POLICY audit_entries_select_authenticated ON public.audit_entries FOR SELECT TO authenticated USING(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id());
CREATE POLICY transactions_select_cashier_admin_manager ON public.transactions FOR SELECT TO authenticated USING(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id() AND (has_role(auth.uid(),'admin') OR has_role(auth.uid(),'cashier') OR has_role(auth.uid(),'warehouse_manager')));
CREATE POLICY transactions_insert_cashier_admin ON public.transactions FOR INSERT TO authenticated WITH CHECK(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id() AND (has_role(auth.uid(),'admin') OR has_role(auth.uid(),'cashier')));
CREATE POLICY workspace_settings_select_authenticated ON public.workspace_settings FOR SELECT TO authenticated USING(is_active_user(auth.uid()) AND ecosystem_id=current_user_ecosystem_id());
CREATE POLICY workspace_settings_update_admin ON public.workspace_settings FOR UPDATE TO authenticated USING(has_role(auth.uid(),'admin') AND ecosystem_id=current_user_ecosystem_id()) WITH CHECK(has_role(auth.uid(),'admin') AND ecosystem_id=current_user_ecosystem_id());
ALTER VIEW public.inventory_balances SET(security_invoker=true); ALTER VIEW public.product_stock_summary SET(security_invoker=true);

INSERT INTO storage.buckets(id,name,public,file_size_limit,allowed_mime_types) VALUES('product-images','product-images',true,5242880,ARRAY['image/jpeg','image/png','image/webp','image/gif']) ON CONFLICT(id) DO UPDATE SET public=EXCLUDED.public,file_size_limit=EXCLUDED.file_size_limit,allowed_mime_types=EXCLUDED.allowed_mime_types;
CREATE POLICY product_images_public_read ON storage.objects FOR SELECT TO public USING(bucket_id='product-images');
CREATE POLICY product_images_inventory_insert ON storage.objects FOR INSERT TO authenticated WITH CHECK(bucket_id='product-images' AND public.is_active_user(auth.uid()) AND public.current_user_ecosystem_id() IS NOT NULL AND split_part(name,'/',1)=public.current_user_ecosystem_id()::text AND (public.has_role(auth.uid(),'admin') OR public.has_role(auth.uid(),'warehouse_manager') OR public.has_role(auth.uid(),'warehouse_worker')));
CREATE POLICY product_images_inventory_update ON storage.objects FOR UPDATE TO authenticated USING(bucket_id='product-images' AND public.is_active_user(auth.uid()) AND public.current_user_ecosystem_id() IS NOT NULL AND split_part(name,'/',1)=public.current_user_ecosystem_id()::text AND (public.has_role(auth.uid(),'admin') OR public.has_role(auth.uid(),'warehouse_manager') OR public.has_role(auth.uid(),'warehouse_worker'))) WITH CHECK(bucket_id='product-images' AND public.current_user_ecosystem_id() IS NOT NULL AND split_part(name,'/',1)=public.current_user_ecosystem_id()::text AND (public.has_role(auth.uid(),'admin') OR public.has_role(auth.uid(),'warehouse_manager') OR public.has_role(auth.uid(),'warehouse_worker')));
CREATE POLICY product_images_inventory_delete ON storage.objects FOR DELETE TO authenticated USING(bucket_id='product-images' AND public.is_active_user(auth.uid()) AND public.current_user_ecosystem_id() IS NOT NULL AND split_part(name,'/',1)=public.current_user_ecosystem_id()::text AND (public.has_role(auth.uid(),'admin') OR public.has_role(auth.uid(),'warehouse_manager') OR public.has_role(auth.uid(),'warehouse_worker')));

-- Canonical demo seed. Product values and image arrays are carried from legacy V12 during Phase 2 split.
INSERT INTO public.ecosystems(id,name) VALUES('33333333-3333-4333-8333-333333333301','Acme Demo') ON CONFLICT(id) DO NOTHING;
INSERT INTO public.workspace_settings(id,ecosystem_id,business_name,contact_email,tax_rate,tax_label,receipt_footer,business_mode) VALUES('00000000-0000-4000-8000-000000000001','33333333-3333-4333-8333-333333333301','ProdWatch Demo Co.','ops@prodwatch.app',.16,'VAT','Thank you for your purchase!','auto') ON CONFLICT(id) DO NOTHING;
INSERT INTO public.warehouses(id,ecosystem_id,name,location) VALUES
('11111111-1111-4111-8111-111111111101','33333333-3333-4333-8333-333333333301','Central Depot','Caracas, VE'),
('11111111-1111-4111-8111-111111111102','33333333-3333-4333-8333-333333333301','North Hub','Valencia, VE'),
('11111111-1111-4111-8111-111111111103','33333333-3333-4333-8333-333333333301','Coastal Annex','Maracaibo, VE') ON CONFLICT(id) DO NOTHING;
INSERT INTO public.products(id,ecosystem_id,name,sku,category,price,default_warehouse_id,low_stock_threshold,images) VALUES
('22222222-2222-4222-8222-222222222201','33333333-3333-4333-8333-333333333301','Ceramic Pour-Over Kettle','KTL-001','Kitchen',78,'11111111-1111-4111-8111-111111111101',10,ARRAY['https://picsum.photos/seed/kettle-1/600/600','https://picsum.photos/seed/kettle-2/600/600','https://picsum.photos/seed/kettle-3/600/600','https://picsum.photos/seed/kettle-4/600/600']),
('22222222-2222-4222-8222-222222222202','33333333-3333-4333-8333-333333333301','Walnut Cutting Board','WCB-220','Kitchen',54.5,'11111111-1111-4111-8111-111111111101',10,ARRAY['https://picsum.photos/seed/board-1/600/600','https://picsum.photos/seed/board-2/600/600','https://picsum.photos/seed/board-3/600/600']),
('22222222-2222-4222-8222-222222222203','33333333-3333-4333-8333-333333333301','Linen Apron — Charcoal','APR-CHR','Apparel',39,'11111111-1111-4111-8111-111111111102',5,ARRAY['https://picsum.photos/seed/apron-1/600/600','https://picsum.photos/seed/apron-2/600/600']),
('22222222-2222-4222-8222-222222222204','33333333-3333-4333-8333-333333333301','Brass Espresso Tamper','TMP-58','Barista',62,'11111111-1111-4111-8111-111111111101',8,ARRAY['https://picsum.photos/seed/tamper-1/600/600','https://picsum.photos/seed/tamper-2/600/600','https://picsum.photos/seed/tamper-3/600/600']),
('22222222-2222-4222-8222-222222222205','33333333-3333-4333-8333-333333333301','Stoneware Mug Set (4)','MUG-S4','Kitchen',48,'11111111-1111-4111-8111-111111111103',15,ARRAY['https://picsum.photos/seed/mugs-1/600/600','https://picsum.photos/seed/mugs-2/600/600','https://picsum.photos/seed/mugs-3/600/600','https://picsum.photos/seed/mugs-4/600/600']),
('22222222-2222-4222-8222-222222222206','33333333-3333-4333-8333-333333333301','Cold Brew Carafe — 1L','CBC-1L','Barista',34,'11111111-1111-4111-8111-111111111102',6,ARRAY['https://picsum.photos/seed/carafe-1/600/600','https://picsum.photos/seed/carafe-2/600/600']),
('22222222-2222-4222-8222-222222222207','33333333-3333-4333-8333-333333333301','Cast Iron Skillet 10"','CIS-10','Kitchen',92,'11111111-1111-4111-8111-111111111101',5,ARRAY['https://picsum.photos/seed/skillet-1/600/600','https://picsum.photos/seed/skillet-2/600/600','https://picsum.photos/seed/skillet-3/600/600']),
('22222222-2222-4222-8222-222222222208','33333333-3333-4333-8333-333333333301','Bamboo Tea Tray','TEA-BMB','Kitchen',28,'11111111-1111-4111-8111-111111111103',10,ARRAY['https://picsum.photos/seed/tray-1/600/600','https://picsum.photos/seed/tray-2/600/600']),
('22222222-2222-4222-8222-222222222209','33333333-3333-4333-8333-333333333301','Hand-Thrown Vase','VAS-HT1','Home',120,'11111111-1111-4111-8111-111111111102',4,ARRAY['https://picsum.photos/seed/vase-1/600/600','https://picsum.photos/seed/vase-2/600/600','https://picsum.photos/seed/vase-3/600/600','https://picsum.photos/seed/vase-4/600/600']),
('22222222-2222-4222-8222-222222222210','33333333-3333-4333-8333-333333333301','Wool Throw Blanket','WTB-77','Home',145,'11111111-1111-4111-8111-111111111103',4,ARRAY['https://picsum.photos/seed/blanket-1/600/600','https://picsum.photos/seed/blanket-2/600/600','https://picsum.photos/seed/blanket-3/600/600']),
('22222222-2222-4222-8222-222222222211','33333333-3333-4333-8333-333333333301','Beeswax Candle Trio','CDL-BW3','Home',36,'11111111-1111-4111-8111-111111111101',10,ARRAY['https://picsum.photos/seed/candle-1/600/600','https://picsum.photos/seed/candle-2/600/600','https://picsum.photos/seed/candle-3/600/600']),
('22222222-2222-4222-8222-222222222212','33333333-3333-4333-8333-333333333301','Stainless Milk Pitcher','MLK-350','Barista',22,'11111111-1111-4111-8111-111111111102',6,ARRAY['https://picsum.photos/seed/pitcher-1/600/600']) ON CONFLICT(id) DO NOTHING;

-- Final V25 activity function, corrected for the final V15 provider/recipient constraint.
CREATE OR REPLACE FUNCTION public.seed_demo_activity() RETURNS jsonb LANGUAGE plpgsql SECURITY DEFINER SET search_path=public AS $$
DECLARE demo_eco uuid := '33333333-3333-4333-8333-333333333301'; u_admin uuid; u_manager uuid; u_worker uuid; u_inspector uuid; u_cashier uuid; u_inactive uuid; w_central uuid := '11111111-1111-4111-8111-111111111101'; w_north uuid := '11111111-1111-4111-8111-111111111102'; w_coastal uuid := '11111111-1111-4111-8111-111111111103';
BEGIN
 SELECT id INTO u_admin FROM profiles WHERE email='alex@acme.co'; SELECT id INTO u_manager FROM profiles WHERE email='maya@acme.co'; SELECT id INTO u_worker FROM profiles WHERE email='jordan@acme.co'; SELECT id INTO u_inspector FROM profiles WHERE email='sam@acme.co'; SELECT id INTO u_cashier FROM profiles WHERE email='riley@acme.co'; SELECT id INTO u_inactive FROM profiles WHERE email='devon@acme.co';
 IF u_worker IS NULL OR u_cashier IS NULL THEN RETURN jsonb_build_object('status','skipped','reason','provision auth users first'); END IF;
 UPDATE public.profiles SET ecosystem_id=demo_eco WHERE email IN ('alex@acme.co','maya@acme.co','jordan@acme.co','sam@acme.co','riley@acme.co','devon@acme.co') AND (ecosystem_id IS NULL OR ecosystem_id<>demo_eco);
 INSERT INTO stock_movements(id,ecosystem_id,type,product_id,qty,to_warehouse_id,user_id,provider,note,created_at) VALUES
 ('33333333-3333-4333-8333-333333333001',demo_eco,'IN','22222222-2222-4222-8222-222222222201',22,w_central,u_worker,'Opening balance','Opening balance',now()-interval '30 days'),('33333333-3333-4333-8333-333333333002',demo_eco,'IN','22222222-2222-4222-8222-222222222202',10,w_central,u_worker,'Opening balance','Opening balance',now()-interval '30 days'),('33333333-3333-4333-8333-333333333004',demo_eco,'IN','22222222-2222-4222-8222-222222222204',27,w_central,u_worker,'Opening balance','Opening balance',now()-interval '30 days'),('33333333-3333-4333-8333-333333333005',demo_eco,'IN','22222222-2222-4222-8222-222222222205',100,w_coastal,u_worker,'Opening balance','Opening balance',now()-interval '30 days'),('33333333-3333-4333-8333-333333333006',demo_eco,'IN','22222222-2222-4222-8222-222222222206',6,w_north,u_worker,'Opening balance','Opening balance',now()-interval '30 days'),('33333333-3333-4333-8333-333333333007',demo_eco,'IN','22222222-2222-4222-8222-222222222207',17,w_central,u_worker,'Opening balance','Opening balance',now()-interval '30 days'),('33333333-3333-4333-8333-333333333008',demo_eco,'IN','22222222-2222-4222-8222-222222222208',31,w_coastal,u_worker,'Opening balance','Opening balance',now()-interval '30 days'),('33333333-3333-4333-8333-333333333009',demo_eco,'IN','22222222-2222-4222-8222-222222222209',9,w_north,u_worker,'Opening balance','Opening balance',now()-interval '30 days'),('33333333-3333-4333-8333-333333333010',demo_eco,'IN','22222222-2222-4222-8222-222222222210',4,w_coastal,u_worker,'Opening balance','Opening balance',now()-interval '30 days'),('33333333-3333-4333-8333-333333333011',demo_eco,'IN','22222222-2222-4222-8222-222222222211',80,w_central,u_worker,'Opening balance','Opening balance',now()-interval '30 days'),('33333333-3333-4333-8333-333333333012',demo_eco,'IN','22222222-2222-4222-8222-222222222212',1,w_north,u_worker,'Opening balance','Opening balance',now()-interval '30 days') ON CONFLICT(id) DO NOTHING;
 INSERT INTO stock_movements(id,ecosystem_id,type,product_id,qty,from_warehouse_id,to_warehouse_id,user_id,provider,recipient,note) VALUES
 ('33333333-3333-4333-8333-333333333101',demo_eco,'IN','22222222-2222-4222-8222-222222222201',20,NULL,w_central,u_worker,'Supplier delivery',NULL,'Supplier delivery'),
 ('33333333-3333-4333-8333-333333333102',demo_eco,'OUT','22222222-2222-4222-8222-222222222202',4,w_central,NULL,u_cashier,NULL,'POS sale','POS sale'),
 ('33333333-3333-4333-8333-333333333103',demo_eco,'TRANSFER','22222222-2222-4222-8222-222222222205',12,w_coastal,w_central,u_manager,NULL,NULL,NULL),
 ('33333333-3333-4333-8333-333333333104',demo_eco,'ADJUSTMENT','22222222-2222-4222-8222-222222222206',2,w_north,NULL,u_inspector,NULL,NULL,'Damaged units'),
 ('33333333-3333-4333-8333-333333333105',demo_eco,'IN','22222222-2222-4222-8222-222222222210',8,NULL,w_coastal,u_worker,'Supplier delivery',NULL,NULL),
 ('33333333-3333-4333-8333-333333333106',demo_eco,'OUT','22222222-2222-4222-8222-222222222204',3,w_central,NULL,u_cashier,NULL,'POS sale',NULL),
 ('33333333-3333-4333-8333-333333333107',demo_eco,'TRANSFER','22222222-2222-4222-8222-222222222211',20,w_central,w_north,u_manager,NULL,NULL,NULL),
 ('33333333-3333-4333-8333-333333333108',demo_eco,'ADJUSTMENT','22222222-2222-4222-8222-222222222212',1,NULL,w_north,u_inspector,NULL,NULL,'Recount') ON CONFLICT(id) DO NOTHING;
 INSERT INTO audit_entries(id,ecosystem_id,user_id,action,entity,entity_id,entity_label,details) VALUES
 ('44444444-4444-4444-8444-444444444401',demo_eco,u_admin,'USER_CREATED','user',COALESCE(u_inactive,u_admin),COALESCE((SELECT name FROM profiles WHERE id=COALESCE(u_inactive,u_admin)),'Devon Cruz'),'Invited Devon Cruz'),
 ('44444444-4444-4444-8444-444444444406',demo_eco,u_admin,'ROLE_CHANGED','user',u_worker,COALESCE((SELECT name FROM profiles WHERE id=u_worker),'Jordan'),'worker → manager (reverted)') ON CONFLICT(id) DO NOTHING;
 INSERT INTO audit_entries(id,ecosystem_id,user_id,action,entity,entity_id,details) VALUES
 ('44444444-4444-4444-8444-444444444402',demo_eco,u_manager,'PRODUCT_UPDATED','product','22222222-2222-4222-8222-222222222202','Price 52.00 → 54.50'),
 ('44444444-4444-4444-8444-444444444403',demo_eco,u_inspector,'AUDIT_RUN','warehouse',w_north,'Spot check, 2 variances'),
 ('44444444-4444-4444-8444-444444444404',demo_eco,u_worker,'STOCK_RECEIVED','movement','33333333-3333-4333-8333-333333333101',NULL),
 ('44444444-4444-4444-8444-444444444405',demo_eco,u_cashier,'TRANSACTION_COMPLETED','transaction','55555555-5555-4555-8555-555555555501',NULL) ON CONFLICT(id) DO NOTHING;
 INSERT INTO transactions(id,ecosystem_id,items,subtotal,tax,total,cashier_id,status) VALUES
 ('55555555-5555-4555-8555-555555555501',demo_eco,'[{"productId":"22222222-2222-4222-8222-222222222202","name":"Walnut Cutting Board","sku":"WCB-220","qty":2,"unitPrice":54.5},{"productId":"22222222-2222-4222-8222-222222222211","name":"Beeswax Candle Trio","sku":"CDL-BW3","qty":1,"unitPrice":36.0}]'::jsonb,145,23.2,168.2,u_cashier,'completed'),
 ('55555555-5555-4555-8555-555555555502',demo_eco,'[{"productId":"22222222-2222-4222-8222-222222222204","name":"Brass Espresso Tamper","sku":"TMP-58","qty":1,"unitPrice":62.0}]'::jsonb,62,9.92,71.92,u_cashier,'completed') ON CONFLICT(id) DO NOTHING;
 RETURN jsonb_build_object('status','ok','ecosystem_id',demo_eco,'stock_movements',(SELECT count(*) FROM stock_movements WHERE ecosystem_id=demo_eco),'audit_entries',(SELECT count(*) FROM audit_entries WHERE ecosystem_id=demo_eco),'transactions',(SELECT count(*) FROM transactions WHERE ecosystem_id=demo_eco));
END;
$$;
GRANT EXECUTE ON FUNCTION public.seed_demo_activity() TO service_role;
