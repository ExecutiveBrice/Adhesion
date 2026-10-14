-- PostgreSQL : import de l'inventaire foyer du 28/07/2025.
-- Source : Inventaire foyer 28.07.2025.xlsx - Feuil1.csv.
-- 62 entrées, 11 zonages. Une cellule non vide = une fiche matériel (lot).
-- Les quantités et les libellés restent tels que fournis, y compris
-- « env 500 gobelets ALOD ». Un emprunt porte sur la fiche entière.
-- Hypothèse : tous les nouveaux lots sont disponibles et en service.
-- Les couleurs ne figurent pas dans le CSV : gris par défaut pour les nouveaux zonages.
-- Les matériels et les couleurs existants ne sont pas modifiés.
-- Rejouable si les noms et les zonages importés restent inchangés.
-- Exécuter le script entier dans une même session.

BEGIN;

-- Évite les insertions concurrentes pendant la recherche des entrées existantes.
LOCK TABLE zonage, materiel IN SHARE ROW EXCLUSIVE MODE;

CREATE TEMP TABLE inventaire_foyer_import (
    zonage_nom varchar(100) NOT NULL,
    materiel_nom varchar(150) NOT NULL,
    PRIMARY KEY (zonage_nom, materiel_nom)
) ON COMMIT DROP;

INSERT INTO inventaire_foyer_import (zonage_nom, materiel_nom) VALUES
    ('RACK 1', 'Barnum ancien tubes'),
    ('RACK 1', 'Compteur chantier'),
    ('RACK 1', 'Cannes à pêche à la ligne'),
    ('RACK 1', 'Câble videoprojecteur'),
    ('RACK 1', 'Pied parasol (ALHCC)'),
    ('RACK 1', 'Spot led (ALHCC)'),
    ('RACK 1', 'Aspirateur sans sac'),
    ('RACK 1', 'Compresseur portatif'),
    ('RACK 1', '3 rallonges noires 20m'),
    ('RACK 2', '8 grandes tables'),
    ('RACK 2', 'char carnaval'),
    ('RACK 3', '2 barnums 3x3'),
    ('RACK 3', '1 barnum 3x6'),
    ('RACK 3', '1 batterie'),
    ('RACK 3', '1 échelle double'),
    ('RACK 3', '1 poursuite lumineuse'),
    ('RACK 4', '11 tables bois'),
    ('RACK 4', '11 planches (anciennes tables)'),
    ('RACK 4', 'maboule electrique'),
    ('RACK 4', '2 escabeaux'),
    ('RACK 5', '2 bacs gastro H80'),
    ('RACK 5', '1 bac gastro H100'),
    ('RACK 5', '1 bac gastro H200'),
    ('RACK 5', '4 seaux alimentaires'),
    ('RACK 5', '2 rallonges 5m'),
    ('RACK 5', '1 percolateur'),
    ('RACK 5', '1 bilig'),
    ('RACK 5', '1 crépière electrique'),
    ('RACK 5', '1 friteuse electrique 2 bacs'),
    ('RACK 5', '1 plancha gaz'),
    ('RACK 5', '1 four à pizza'),
    ('RACK 5', '1 machine à hot-dog'),
    ('RACK 5', '2 sets instruments barbecue'),
    ('RACK 5', '1 grill électrique'),
    ('RACK 5', '1 robot multifonction (rape, mixeur…)'),
    ('RACK 5', 'env 500 gobelets ALOD'),
    ('RACK 5', '2 lests 15kg'),
    ('Cours Eveil musical', 'Xylophone 8 notes'),
    ('Cours Eveil musical', 'Jeux de 8 cloches'),
    ('RACK 6', '1 ventilateur diam 60'),
    ('RACK 6', '1 chamboultout'),
    ('RACK 6', 'maquillage'),
    ('RACK 6', 'Jeux en bois'),
    ('SCENE', '2 bancs en bois'),
    ('SALLE', '58 chaises beiges'),
    ('SALLE', '55 chaises bleues'),
    ('SALLE', '4 mange-debouts'),
    ('SALLE', '1 piano'),
    ('SALLE', '2 porte-manteaux'),
    ('SALLE', '2 chaises hautes'),
    ('BAR', '50 verres à pied'),
    ('BAR', '1 micro-ondes'),
    ('BAR', '2 cafetières'),
    ('BAR', '1 bouilloire'),
    ('BUREAU', '1 PC + écran'),
    ('BUREAU', '1 imprimante'),
    ('BUREAU', '1 PC portable'),
    ('BUREAU', '1 video projecteur'),
    ('BUREAU', '1 chaise ergo'),
    ('BUREAU', '1 bureau'),
    ('BUREAU', '1 armoire'),
    ('BUREAU', '1 table');

-- Réutilise les zonages sans tenir compte de la casse et des espaces de bord.
-- Plusieurs zonages correspondants rendraient le rattachement ambigu : arrêt.
DO $$
BEGIN
    IF EXISTS (
        SELECT upper(btrim(z.nom))
        FROM zonage z
        WHERE upper(btrim(z.nom)) IN (
            SELECT upper(btrim(zonage_nom)) FROM inventaire_foyer_import
        )
        GROUP BY upper(btrim(z.nom))
        HAVING count(*) > 1
    ) THEN
        RAISE EXCEPTION 'Plusieurs zonages correspondent à un emplacement de l''inventaire. Résoudre les doublons avant l''import.';
    END IF;
END $$;

INSERT INTO zonage (nom, couleur)
SELECT DISTINCT i.zonage_nom, '#64748B'
FROM inventaire_foyer_import i
WHERE NOT EXISTS (
    SELECT 1 FROM zonage z
    WHERE upper(btrim(z.nom)) = upper(btrim(i.zonage_nom))
);

-- Les identifiants sont générés par la base. Aucun historique d'emprunt
-- n'est créé, car le CSV ne fournit aucune information sur les emprunts.
WITH inseres AS (
    INSERT INTO materiel (nom, zonage_id, hors_service)
    SELECT i.materiel_nom, z.id, false
    FROM inventaire_foyer_import i
    JOIN zonage z ON upper(btrim(z.nom)) = upper(btrim(i.zonage_nom))
    WHERE NOT EXISTS (
        SELECT 1 FROM materiel m
        WHERE m.zonage_id = z.id
          AND upper(btrim(m.nom)) = upper(btrim(i.materiel_nom))
    )
    RETURNING id
)
SELECT count(*) AS materiels_ajoutes FROM inseres;

-- Contrôle : attendus par zonage = 9, 2, 5, 4, 17, 2, 4, 1, 6, 4, 8
-- dans l'ordre des colonnes du CSV. Total attendu : 62.
SELECT i.zonage_nom, count(*) AS entrees_source,
       count(*) FILTER (WHERE EXISTS (
           SELECT 1 FROM materiel m
           WHERE m.zonage_id = z.id
             AND upper(btrim(m.nom)) = upper(btrim(i.materiel_nom))
       )) AS entrees_presentes
FROM inventaire_foyer_import i
JOIN zonage z ON upper(btrim(z.nom)) = upper(btrim(i.zonage_nom))
GROUP BY i.zonage_nom
ORDER BY i.zonage_nom;

COMMIT;
-- Pour une simulation, remplacer COMMIT par ROLLBACK avant exécution.
