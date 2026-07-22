-- Allow profile deletion while keeping historical audit/movement rows (user unlinked).

ALTER TABLE public.stock_movements
    DROP CONSTRAINT stock_movements_user_id_fkey;

ALTER TABLE public.stock_movements
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE public.stock_movements
    ADD CONSTRAINT stock_movements_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES public.profiles (id) ON DELETE SET NULL;

ALTER TABLE public.audit_entries
    DROP CONSTRAINT audit_entries_user_id_fkey;

ALTER TABLE public.audit_entries
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE public.audit_entries
    ADD CONSTRAINT audit_entries_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES public.profiles (id) ON DELETE SET NULL;
