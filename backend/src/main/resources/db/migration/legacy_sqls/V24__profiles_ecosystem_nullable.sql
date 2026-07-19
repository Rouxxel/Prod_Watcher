-- Profiles are created by auth trigger before bootstrap assigns an ecosystem.
-- Nullable until AuthBootstrapService.createForOwner runs on sign-up / login.

ALTER TABLE public.profiles ALTER COLUMN ecosystem_id DROP NOT NULL;
