CREATE TABLE public.audit_entries (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), ecosystem_id uuid NOT NULL REFERENCES public.ecosystems(id), user_id uuid REFERENCES public.profiles(id) ON DELETE SET NULL,
 action text NOT NULL, entity text NOT NULL, entity_id uuid NOT NULL, entity_label text, details text, created_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX idx_audit_entries_entity ON public.audit_entries(entity); CREATE INDEX idx_audit_entries_entity_id ON public.audit_entries(entity_id); CREATE INDEX idx_audit_entries_created_at ON public.audit_entries(created_at); CREATE INDEX idx_audit_entries_user_id ON public.audit_entries(user_id); CREATE INDEX idx_audit_entries_ecosystem_id ON public.audit_entries(ecosystem_id);
