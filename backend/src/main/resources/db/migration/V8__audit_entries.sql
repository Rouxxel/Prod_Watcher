-- Append-only audit trail (immutability enforced via RLS in V10).

CREATE TABLE public.audit_entries (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid        NOT NULL REFERENCES public.profiles (id),
    action      text        NOT NULL,
    entity      text        NOT NULL,
    entity_id   uuid        NOT NULL,
    details     text,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_entries_entity ON public.audit_entries (entity);
CREATE INDEX idx_audit_entries_entity_id ON public.audit_entries (entity_id);
CREATE INDEX idx_audit_entries_created_at ON public.audit_entries (created_at);
CREATE INDEX idx_audit_entries_user_id ON public.audit_entries (user_id);
