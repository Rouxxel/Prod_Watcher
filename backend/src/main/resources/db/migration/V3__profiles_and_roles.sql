-- Profiles and role assignments (auth.users → profiles via trigger).

CREATE TABLE public.profiles (
    id          uuid        PRIMARY KEY REFERENCES auth.users (id) ON DELETE CASCADE,
    name        text        NOT NULL,
    email       text        NOT NULL UNIQUE,
    active      boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE public.user_roles (
    id       uuid      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id  uuid      NOT NULL REFERENCES public.profiles (id) ON DELETE CASCADE,
    role     app_role  NOT NULL,
    UNIQUE (user_id, role)
);

CREATE INDEX idx_user_roles_user_id ON public.user_roles (user_id);
CREATE INDEX idx_user_roles_role ON public.user_roles (role);

-- Keep updated_at current on row changes.
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

CREATE TRIGGER profiles_set_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

-- Sync auth.users → profiles on sign-up / admin createUser.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.profiles (id, name, email)
    VALUES (
        NEW.id,
        COALESCE(
            NULLIF(trim(NEW.raw_user_meta_data ->> 'name'), ''),
            NULLIF(trim(NEW.raw_user_meta_data ->> 'full_name'), ''),
            split_part(NEW.email, '@', 1)
        ),
        NEW.email
    );
    RETURN NEW;
END;
$$;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();

-- Called by backend after bootstrap owner email confirmation (no admin exists yet).
CREATE OR REPLACE FUNCTION public.bootstrap_assign_admin(_user_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.user_roles WHERE role = 'admin') THEN
        INSERT INTO public.user_roles (user_id, role)
        VALUES (_user_id, 'admin')
        ON CONFLICT (user_id, role) DO NOTHING;
    END IF;
END;
$$;

GRANT EXECUTE ON FUNCTION public.bootstrap_assign_admin(uuid) TO service_role;
