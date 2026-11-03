-- PostgreSQL : appliquer avant le déploiement de la gestion des quantités.
-- Une fiche existante reste une référence de stock 1 ; aucun regroupement automatique.
BEGIN;
ALTER TABLE materiel ADD COLUMN IF NOT EXISTS quantite_totale integer NOT NULL DEFAULT 1;
ALTER TABLE materiel ADD COLUMN IF NOT EXISTS quantite_hors_service integer NOT NULL DEFAULT 0;
ALTER TABLE emprunt_materiel ADD COLUMN IF NOT EXISTS quantite integer NOT NULL DEFAULT 1;
ALTER TABLE emprunt_materiel ADD COLUMN IF NOT EXISTS quantite_retournee integer NOT NULL DEFAULT 0;
-- Les anciens matériels pouvaient être empruntés ET HS : conserver l'emprunt en priorité.
UPDATE materiel SET quantite_hors_service = 1 WHERE hors_service AND emprunteur_id IS NULL AND quantite_hors_service = 0;
UPDATE emprunt_materiel SET quantite_retournee = quantite WHERE date_retour IS NOT NULL AND hors_service IS NULL;
INSERT INTO emprunt_materiel (materiel_id, emprunteur_id, emprunteur_nom, date_emprunt, quantite, quantite_retournee)
SELECT m.id, m.emprunteur_id, m.emprunteur_nom, m.date_emprunt, 1, 0 FROM materiel m
WHERE m.emprunteur_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM emprunt_materiel e WHERE e.materiel_id = m.id AND e.hors_service IS NULL AND e.date_retour IS NULL
);
UPDATE materiel SET emprunteur_id = NULL, emprunteur_nom = NULL, date_emprunt = NULL, hors_service = (quantite_hors_service > 0);
CREATE TABLE IF NOT EXISTS retour_materiel (
    emprunt_id bigint NOT NULL REFERENCES emprunt_materiel(id),
    quantite integer NOT NULL CHECK (quantite > 0),
    date_retour timestamptz NOT NULL,
    rapporte_par_id bigint,
    rapporte_par_nom varchar(511)
);
CREATE INDEX IF NOT EXISTS idx_retour_materiel_emprunt ON retour_materiel(emprunt_id);
COMMIT;
