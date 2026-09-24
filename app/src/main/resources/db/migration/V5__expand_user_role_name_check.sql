DO $$
DECLARE
    role_check record;
BEGIN
    IF to_regclass('user_role_names') IS NULL THEN
        RETURN;
    END IF;

    FOR role_check IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'user_role_names'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%role_name%'
    LOOP
        EXECUTE format('ALTER TABLE user_role_names DROP CONSTRAINT %I', role_check.conname);
    END LOOP;

    ALTER TABLE user_role_names ADD CONSTRAINT user_role_names_role_name_check
        CHECK (role_name IN (
            'ROLE_USER', 'ROLE_SECRETAIRE', 'ROLE_BUREAU', 'ROLE_MEMBRECA',
            'ROLE_ADMIN', 'ROLE_COMPTABLE', 'ROLE_ENCADRANT', 'ROLE_REFERENT',
            'ROLE_RESPONSABLE_BOUTIQUE'
        ));
END $$;
