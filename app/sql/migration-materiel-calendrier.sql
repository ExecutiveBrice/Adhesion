-- PostgreSQL : appliquer avant le déploiement des périodes d’emprunt.
-- Les anciens emprunts conservent des dates prévues non renseignées.
BEGIN;
ALTER TABLE emprunt_materiel ADD COLUMN IF NOT EXISTS date_debut date;
ALTER TABLE emprunt_materiel ADD COLUMN IF NOT EXISTS date_fin_prevue date;
COMMIT;
