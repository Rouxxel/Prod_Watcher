CREATE TABLE public.warehouses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), ecosystem_id uuid NOT NULL REFERENCES public.ecosystems(id),
  name text NOT NULL, location text NOT NULL, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX idx_warehouses_name ON public.warehouses(name); CREATE INDEX idx_warehouses_ecosystem_id ON public.warehouses(ecosystem_id);
CREATE TRIGGER warehouses_set_updated_at BEFORE UPDATE ON public.warehouses FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
