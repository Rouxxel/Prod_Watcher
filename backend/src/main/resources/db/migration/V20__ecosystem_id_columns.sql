-- Add nullable ecosystem_id to tenant-scoped tables (backfill in V21).

ALTER TABLE public.profiles
    ADD COLUMN ecosystem_id uuid REFERENCES public.ecosystems (id);

ALTER TABLE public.warehouses
    ADD COLUMN ecosystem_id uuid REFERENCES public.ecosystems (id);

ALTER TABLE public.products
    ADD COLUMN ecosystem_id uuid REFERENCES public.ecosystems (id);

ALTER TABLE public.stock_movements
    ADD COLUMN ecosystem_id uuid REFERENCES public.ecosystems (id);

ALTER TABLE public.transactions
    ADD COLUMN ecosystem_id uuid REFERENCES public.ecosystems (id);

ALTER TABLE public.audit_entries
    ADD COLUMN ecosystem_id uuid REFERENCES public.ecosystems (id);

ALTER TABLE public.workspace_settings
    ADD COLUMN ecosystem_id uuid REFERENCES public.ecosystems (id);

CREATE INDEX idx_profiles_ecosystem_id ON public.profiles (ecosystem_id);
CREATE INDEX idx_warehouses_ecosystem_id ON public.warehouses (ecosystem_id);
CREATE INDEX idx_products_ecosystem_id ON public.products (ecosystem_id);
CREATE INDEX idx_stock_movements_ecosystem_id ON public.stock_movements (ecosystem_id);
CREATE INDEX idx_transactions_ecosystem_id ON public.transactions (ecosystem_id);
CREATE INDEX idx_audit_entries_ecosystem_id ON public.audit_entries (ecosystem_id);
CREATE INDEX idx_workspace_settings_ecosystem_id ON public.workspace_settings (ecosystem_id);
