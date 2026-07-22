CREATE OR REPLACE FUNCTION public.set_updated_at() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN NEW.updated_at = now(); RETURN NEW; END;
$$;

CREATE TABLE public.ecosystems (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name text NOT NULL DEFAULT '',
  created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX idx_ecosystems_name ON public.ecosystems(name);
CREATE TRIGGER ecosystems_set_updated_at BEFORE UPDATE ON public.ecosystems FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
