CREATE TABLE public.transactions (
 id uuid PRIMARY KEY DEFAULT gen_random_uuid(), ecosystem_id uuid NOT NULL REFERENCES public.ecosystems(id), items jsonb NOT NULL CHECK(jsonb_typeof(items)='array'),
 subtotal numeric(12,2) NOT NULL CHECK(subtotal>=0), tax numeric(12,2) NOT NULL CHECK(tax>=0), total numeric(12,2) NOT NULL CHECK(total>=0),
 cashier_id uuid NOT NULL REFERENCES public.profiles(id), status public.transaction_status NOT NULL DEFAULT 'completed', created_at timestamptz NOT NULL DEFAULT now());
CREATE INDEX idx_transactions_cashier_id ON public.transactions(cashier_id); CREATE INDEX idx_transactions_created_at ON public.transactions(created_at); CREATE INDEX idx_transactions_status ON public.transactions(status); CREATE INDEX idx_transactions_ecosystem_id ON public.transactions(ecosystem_id);
