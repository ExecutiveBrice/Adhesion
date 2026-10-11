-- PostgreSQL : exécuter avant de déployer le journal des changements HS
-- sur une base possédant déjà la table emprunt_materiel.
BEGIN;
ALTER TABLE emprunt_materiel ADD COLUMN IF NOT EXISTS hors_service boolean;
ALTER TABLE emprunt_materiel ALTER COLUMN emprunteur_id DROP NOT NULL;
ALTER TABLE emprunt_materiel ALTER COLUMN emprunteur_nom DROP NOT NULL;
COMMIT;
