-- Snapshot display label for audit entity (e.g. user name after profile deletion).

-- Backfill user names where the profile still exists.
UPDATE public.audit_entries ae
SET entity_label = p.name
FROM public.profiles p
WHERE ae.entity = 'user'
  AND ae.entity_id = p.id
  AND ae.entity_label IS NULL
  AND p.name IS NOT NULL
  AND trim(p.name) <> '';
