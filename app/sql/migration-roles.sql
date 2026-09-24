-- PostgreSQL. Exécuter sur la base de l'application, avant de démarrer le nouveau backend.
-- Les anciennes tables roles/users_roles sont facultatives : le script gère aussi une base déjà convertie.
BEGIN;

CREATE TABLE IF NOT EXISTS user_role_names (
    user_id bigint NOT NULL REFERENCES users(id),
    role_name varchar(32) NOT NULL
);

-- Un ancien CHECK généré à partir d'ERole peut refuser ROLE_MEMBRECA ou ROLE_ENCADRANT.
DO $$
DECLARE old_check record;
BEGIN
    FOR old_check IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'user_role_names'::regclass
          AND contype = 'c'
          AND (pg_get_constraintdef(oid) LIKE '%ROLE_ADMINISTRATEUR%'
               OR pg_get_constraintdef(oid) LIKE '%ROLE_PROF%')
    LOOP
        EXECUTE format('ALTER TABLE user_role_names DROP CONSTRAINT %I', old_check.conname);
    END LOOP;
END $$;

-- Transférer les attributions encore présentes dans les anciennes tables, si elles existent.
DO $$
BEGIN
    IF (to_regclass('roles') IS NULL) <> (to_regclass('users_roles') IS NULL) THEN
        RAISE EXCEPTION 'Anciennes tables de rôles incomplètes';
    END IF;

    IF to_regclass('roles') IS NOT NULL THEN
        INSERT INTO user_role_names (user_id, role_name)
        SELECT DISTINCT ur.user_id,
               CASE r.name
                   WHEN 'ROLE_ADMINISTRATEUR' THEN 'ROLE_MEMBRECA'
                   WHEN 'ROLE_PROF' THEN 'ROLE_ENCADRANT'
                   ELSE r.name
               END
        FROM users_roles ur
        JOIN roles r ON r.id = ur.roles_id
        WHERE NOT EXISTS (
            SELECT 1
            FROM user_role_names existing_role
            WHERE existing_role.user_id = ur.user_id
              AND existing_role.role_name = CASE r.name
                  WHEN 'ROLE_ADMINISTRATEUR' THEN 'ROLE_MEMBRECA'
                  WHEN 'ROLE_PROF' THEN 'ROLE_ENCADRANT'
                  ELSE r.name
              END
        );
    END IF;
END $$;

-- Renommer les rôles déjà stockés dans user_role_names, sans doubler une attribution existante.
INSERT INTO user_role_names (user_id, role_name)
SELECT DISTINCT old.user_id,
       CASE old.role_name
           WHEN 'ROLE_ADMINISTRATEUR' THEN 'ROLE_MEMBRECA'
           WHEN 'ROLE_PROF' THEN 'ROLE_ENCADRANT'
       END
FROM user_role_names old
WHERE old.role_name IN ('ROLE_ADMINISTRATEUR', 'ROLE_PROF')
  AND NOT EXISTS (
      SELECT 1
      FROM user_role_names existing_role
      WHERE existing_role.user_id = old.user_id
        AND existing_role.role_name = CASE old.role_name
            WHEN 'ROLE_ADMINISTRATEUR' THEN 'ROLE_MEMBRECA'
            WHEN 'ROLE_PROF' THEN 'ROLE_ENCADRANT'
        END
  );

DELETE FROM user_role_names
WHERE role_name IN ('ROLE_ADMINISTRATEUR', 'ROLE_PROF');

-- Arrêter avant de supprimer les anciennes tables si un rôle inconnu reste enregistré.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM user_role_names
        WHERE role_name NOT IN (
            'ROLE_USER', 'ROLE_SECRETAIRE', 'ROLE_BUREAU', 'ROLE_MEMBRECA',
            'ROLE_ADMIN', 'ROLE_COMPTABLE', 'ROLE_ENCADRANT', 'ROLE_REFERENT',
            'ROLE_RESPONSABLE_BOUTIQUE'
        )
    ) THEN
        RAISE EXCEPTION 'Un rôle inconnu reste dans user_role_names';
    END IF;
END $$;

-- Supprimer les doublons éventuels puis empêcher leur réapparition.
DELETE FROM user_role_names a
USING user_role_names b
WHERE a.ctid < b.ctid
  AND a.user_id = b.user_id
  AND a.role_name = b.role_name;

CREATE UNIQUE INDEX IF NOT EXISTS user_role_names_user_role_unique
    ON user_role_names (user_id, role_name);

DO $$
DECLARE
    role_check record;
BEGIN
    FOR role_check IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'user_role_names'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) LIKE '%role_name%'
    LOOP
        EXECUTE format('ALTER TABLE user_role_names DROP CONSTRAINT %I', role_check.conname);
    END LOOP;
END $$;
ALTER TABLE user_role_names ADD CONSTRAINT user_role_names_erole_check
    CHECK (role_name IN (
        'ROLE_USER', 'ROLE_SECRETAIRE', 'ROLE_BUREAU', 'ROLE_MEMBRECA',
        'ROLE_ADMIN', 'ROLE_COMPTABLE', 'ROLE_ENCADRANT', 'ROLE_REFERENT',
        'ROLE_RESPONSABLE_BOUTIQUE'
    ));

DROP TABLE IF EXISTS users_roles;
DROP TABLE IF EXISTS roles;

COMMIT;

-- Contrôle après exécution : doit retourner zéro ligne.
SELECT role_name, count(*)
FROM user_role_names
WHERE role_name NOT IN (
    'ROLE_USER', 'ROLE_SECRETAIRE', 'ROLE_BUREAU', 'ROLE_MEMBRECA',
    'ROLE_ADMIN', 'ROLE_COMPTABLE', 'ROLE_ENCADRANT', 'ROLE_REFERENT',
    'ROLE_RESPONSABLE_BOUTIQUE'
)
GROUP BY role_name;
