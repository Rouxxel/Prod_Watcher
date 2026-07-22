-- Auth helper functions for RLS policies and backend checks.
-- References profiles / user_roles (created in V3); PL/pgSQL defers table validation to runtime.

CREATE OR REPLACE FUNCTION public.has_role(_user_id uuid, _role app_role)
RETURNS boolean
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1
        FROM public.user_roles
        WHERE user_id = _user_id
          AND role = _role
    );
END;
$$;

CREATE OR REPLACE FUNCTION public.current_user_role()
RETURNS app_role
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    _role app_role;
BEGIN
    SELECT role
    INTO _role
    FROM public.user_roles
    WHERE user_id = auth.uid()
    LIMIT 1;

    RETURN _role;
END;
$$;

CREATE OR REPLACE FUNCTION public.is_active_user(_user_id uuid)
RETURNS boolean
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    _active boolean;
BEGIN
    SELECT active
    INTO _active
    FROM public.profiles
    WHERE id = _user_id;

    RETURN COALESCE(_active, false);
END;
$$;

GRANT EXECUTE ON FUNCTION public.has_role(uuid, app_role) TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.current_user_role() TO authenticated, service_role;
GRANT EXECUTE ON FUNCTION public.is_active_user(uuid) TO authenticated, service_role;
