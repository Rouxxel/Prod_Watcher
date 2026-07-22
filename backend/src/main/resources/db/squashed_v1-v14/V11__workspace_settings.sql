CREATE TABLE public.workspace_settings (
 id uuid PRIMARY KEY DEFAULT '00000000-0000-4000-8000-000000000001'::uuid, ecosystem_id uuid NOT NULL REFERENCES public.ecosystems(id),
 business_name text NOT NULL DEFAULT '', contact_email text NOT NULL DEFAULT '', tax_rate numeric(6,4) NOT NULL DEFAULT .16, tax_label text NOT NULL DEFAULT 'Tax', receipt_footer text, receipt_logo_url text, business_mode text NOT NULL DEFAULT 'auto', updated_at timestamptz NOT NULL DEFAULT now(), updated_by uuid REFERENCES public.profiles(id) ON DELETE SET NULL,
 CONSTRAINT workspace_settings_tax_rate_check CHECK(tax_rate>=0 AND tax_rate<=1), CONSTRAINT workspace_settings_business_mode_check CHECK(business_mode IN ('auto','single','multi')), UNIQUE(ecosystem_id));
CREATE INDEX idx_workspace_settings_ecosystem_id ON public.workspace_settings(ecosystem_id);
CREATE TRIGGER workspace_settings_set_updated_at BEFORE UPDATE ON public.workspace_settings FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();
