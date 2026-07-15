-- Workspace settings singleton (MVP: one row per deployment).
-- See docs/TASK_04_settings.md Phase 1.

CREATE TABLE public.workspace_settings (
    id                  uuid            PRIMARY KEY DEFAULT '00000000-0000-4000-8000-000000000001'::uuid,
    business_name       text            NOT NULL DEFAULT '',
    contact_email       text            NOT NULL DEFAULT '',
    tax_rate            numeric(6, 4)   NOT NULL DEFAULT 0.16,
    tax_label           text            NOT NULL DEFAULT 'Tax',
    receipt_footer      text,
    receipt_logo_url    text,
    business_mode       text            NOT NULL DEFAULT 'auto',
    updated_at          timestamptz     NOT NULL DEFAULT now(),
    updated_by          uuid            REFERENCES public.profiles (id) ON DELETE SET NULL,
    CONSTRAINT workspace_settings_singleton CHECK (id = '00000000-0000-4000-8000-000000000001'::uuid),
    CONSTRAINT workspace_settings_tax_rate_check CHECK (tax_rate >= 0 AND tax_rate <= 1),
    CONSTRAINT workspace_settings_business_mode_check CHECK (business_mode IN ('auto', 'single', 'multi'))
);

CREATE TRIGGER workspace_settings_set_updated_at
    BEFORE UPDATE ON public.workspace_settings
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

-- Demo-friendly defaults matching Settings page placeholders.
INSERT INTO public.workspace_settings (
    id,
    business_name,
    contact_email,
    tax_rate,
    tax_label,
    receipt_footer,
    business_mode
)
VALUES (
    '00000000-0000-4000-8000-000000000001',
    'ProdWatch Demo Co.',
    'ops@prodwatch.app',
    0.16,
    'VAT',
    'Thank you for your purchase!',
    'auto'
)
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- RLS — authenticated read; admin update only
-- ---------------------------------------------------------------------------

ALTER TABLE public.workspace_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY workspace_settings_select_authenticated
    ON public.workspace_settings
    FOR SELECT
    TO authenticated
    USING (is_active_user(auth.uid()));

CREATE POLICY workspace_settings_update_admin
    ON public.workspace_settings
    FOR UPDATE
    TO authenticated
    USING (has_role(auth.uid(), 'admin'))
    WITH CHECK (has_role(auth.uid(), 'admin'));
