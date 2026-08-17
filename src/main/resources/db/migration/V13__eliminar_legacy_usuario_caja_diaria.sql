DO $$
DECLARE fk_name TEXT;
BEGIN
    FOR fk_name IN
        SELECT tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
         AND tc.table_schema = kcu.table_schema
        WHERE tc.table_schema = 'public'
          AND tc.table_name = 'usuario'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name = 'caja_diaria_id'
    LOOP
        EXECUTE format('ALTER TABLE public.usuario DROP CONSTRAINT IF EXISTS %I', fk_name);
    END LOOP;
END $$;

DO $$
DECLARE idx_name TEXT;
BEGIN
    FOR idx_name IN
        SELECT indexname
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND tablename = 'usuario'
          AND indexdef ILIKE '%(caja_diaria_id%'
    LOOP
        EXECUTE format('DROP INDEX IF EXISTS public.%I', idx_name);
    END LOOP;
END $$;

ALTER TABLE usuario DROP COLUMN IF EXISTS caja_diaria_id;
