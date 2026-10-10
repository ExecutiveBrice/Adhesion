# Bilan initial — 9 octobre 2026

## Socle installé

Les cinq actions sont représentées dans le dépôt : fiches fonctionnelles et matrice de tests,
instructions `AGENTS.md`, workflow de validation, première extraction de règles métier avec
contrôles d'architecture, définition de terminé et modèles d'évolution et de pull request.

Les 30 règles sont issues de la documentation ou des comportements existants. Elles ne constituent
pas une validation métier exhaustive. Six règles indiquent explicitement une couverture partielle
ou absente dans [la matrice](../functional/coverage.json).

## Contrôles exécutés localement

| Contrôle | Résultat |
| --- | --- |
| Backend : `mvn -B verify`, Java 25 | 392 tests réussis ; JAR construit |
| Angular : `npm run test:ci`, Chrome Headless | 238 tests réussis |
| Production : `npm run build:production` | Réussi ; avertissement existant sur le budget du bundle initial |
| Playwright : `npm run test:browser`, Chromium | 7 parcours réussis, API simulée |
| Migrations : `LiquibaseMigrationTest`, PostgreSQL 17 jetable | 11 tests réussis |
| Vérificateur des spécifications | 4 tests réussis et 30 références de règles vérifiées |
| Workflow et diff | YAML analysé ; `git diff --check` réussi |

La CI GitHub n'a pas été exécutée à distance. Ces résultats locaux ne remplacent pas son premier passage.
Les migrations PostgreSQL utilisent des schémas distincts dans une base jetable dédiée.

## Corrections nécessaires à une validation fiable

- Les fixtures MVC de maintenance et d'e-mail ne fournissaient plus `SectionRepository` : le mock
  a été ajouté, sans changer les attentes d'autorisation.
- Les fixtures du tableau de stock ne fournissaient plus `supplierOrders`. Le titre attendu a été
  aligné sur le titre existant « Besoins et approvisionnements » ; les assertions de chargement et
  de création de variante sont conservées.
- Les tests d'interface utilisent une interaction d'onglet, une modification d'input Angular ou
  attendent la stabilisation du rendu lorsque nécessaire. Les attentes fonctionnelles sont conservées.
- Le test de navigation du chat confondait membre et administrateur. Le cas membre est corrigé et
  un cas administrateur distinct ajouté : la documentation préexistante conserve l'accès à la gestion
  même quand l'affichage du chat est désactivé.
- Le test des publicités attendait l'ordre inverse des `displayOrder` de sa fixture. L'attente suit
  désormais le tri ascendant déjà implémenté ; le code de tri n'a pas changé.
- Le décompte des changesets suivait une ancienne liste de migrations. Il inclut désormais les
  migrations existantes et le nouveau changeset v14.
- Mockito est chargé comme agent au démarrage des tests Java pour éviter l'attachement dynamique.

La validation sur PostgreSQL a également exposé deux défauts de migration :

- Le modèle `Adhesion` utilisait `rapprochement`, absent des changelogs. Le changeset **v14** ajoute
  la colonne si elle manque, initialise les lignes existantes à `false` et préserve une colonne
  historique déjà présente. Deux tests vérifient la conservation de sa valeur et le redémarrage.
- La recherche de clé primaire de `RoleDataMigration` pouvait sélectionner une clé d'un autre
  schéma lorsque la table historique n'avait aucune clé. La recherche reste dans le schéma courant.
  Le scénario de reprise historique passe désormais sur PostgreSQL.

## Étapes de livraison et couverture restante

Publier le workflow, attendre son premier passage puis rendre **Quality gate** obligatoire
dans les paramètres GitHub, selon [les instructions](validation.md). Le fichier de workflow
ne configure pas lui-même la protection de branche.

Le socle navigateur simule l'API. Une chaîne navigateur + serveur complète, la concurrence des
confirmations de paiement sur PostgreSQL, le sandbox HelloAsso, les scénarios d'inscription et
certains comportements du chat restent à compléter. Les migrations de production doivent être
répétées sur une copie adaptée de la base avant déploiement.
