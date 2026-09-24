DO $$
BEGIN
    IF to_regclass('user_role_names') IS NOT NULL
       AND EXISTS (
           SELECT 1
           FROM pg_constraint
           WHERE conrelid = 'user_role_names'::regclass
             AND conname = 'user_role_names_erole_check'
       ) THEN
        ALTER TABLE user_role_names DROP CONSTRAINT user_role_names_erole_check;
        ALTER TABLE user_role_names ADD CONSTRAINT user_role_names_erole_check
            CHECK (role_name IN (
                'ROLE_USER', 'ROLE_SECRETAIRE', 'ROLE_BUREAU', 'ROLE_MEMBRECA',
                'ROLE_ADMIN', 'ROLE_COMPTABLE', 'ROLE_ENCADRANT', 'ROLE_REFERENT',
                'ROLE_RESPONSABLE_BOUTIQUE'
            ));
    END IF;
END $$;
