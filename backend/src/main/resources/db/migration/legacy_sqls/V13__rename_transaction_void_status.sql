-- Align the transaction_status 'void' label with the JPA enum constant name.
--
-- The Transaction entity binds status as the native transaction_status enum. Because 'void' is a
-- reserved Java keyword, the enum constant is named `void_`, and Hibernate's native enum binding
-- uses the constant name verbatim. Renaming the DB label to 'void_' keeps writes/reads consistent.
--
-- The REST API contract is unaffected: TransactionStatus exposes "void" externally via its
-- @JsonValue/@JsonCreator mapping. RENAME VALUE updates existing rows automatically.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_enum e
        JOIN pg_type t ON t.oid = e.enumtypid
        WHERE t.typname = 'transaction_status'
          AND e.enumlabel = 'void'
    ) THEN
        ALTER TYPE public.transaction_status RENAME VALUE 'void' TO 'void_';
    END IF;
END$$;
