-- Run in Supabase SQL Editor if stock_movements / audit_entries / transactions are still empty
-- after seed-auth-users.ps1 (roles step only needs this if activity RPC fails).
--
-- Requires V25+ (seed_demo_activity includes demo ecosystem_id: 33333333-3333-4333-8333-333333333301).
-- Safe to re-run — inserts are idempotent (ON CONFLICT DO NOTHING).

SELECT public.seed_demo_activity();
