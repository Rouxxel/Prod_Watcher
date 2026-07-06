-- Run in Supabase SQL Editor if stock_movements / audit_entries / transactions are still empty
-- after seed-auth-users.ps1 (roles step only needs this if activity RPC fails).

SELECT public.seed_demo_activity();
