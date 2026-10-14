-- PostgreSQL : exécuter avant de démarrer le backend avec ROLE_LOGISTIQUE.
BEGIN;

-- Mettre à jour les CHECK énumérant les rôles, y compris ceux générés par Hibernate.
DO $$
DECLARE role_check record;
BEGIN
    FOR role_check IN
        SELECT conname FROM pg_constraint
        WHERE conrelid = 'user_role_names'::regclass AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%role_name%'
          AND pg_get_constraintdef(oid) LIKE '%ROLE_USER%'
    LOOP
        EXECUTE format('ALTER TABLE user_role_names DROP CONSTRAINT %I', role_check.conname);
    END LOOP;
END $$;

ALTER TABLE user_role_names ADD CONSTRAINT user_role_names_erole_check
    CHECK (role_name IN (
        'ROLE_USER', 'ROLE_SECRETAIRE', 'ROLE_BUREAU', 'ROLE_MEMBRECA',
        'ROLE_ADMIN', 'ROLE_COMPTABLE', 'ROLE_ENCADRANT', 'ROLE_REFERENT', 'ROLE_LOGISTIQUE'
    ));

COMMIT;
