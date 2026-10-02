# Schéma de la base de données

`db.changelog-master.xml` est le point d'entrée configuré dans Spring Boot.
Liquibase s'exécute avant JPA ; Hibernate utilise `ddl-auto: validate` et ne
crée ni ne modifie les tables.

## Organisation

- `baseline/master-changeset.xml` : schéma complet de `master` avant la boutique
  (commit `8a2cb14325940a31d9144dd50b5a20c9795725f0`), avec ses 26 tables,
  tables de liaison, identités, contraintes et clés étrangères. La contrainte
  des rôles du socle contient uniquement les huit rôles historiques.
- `shop/01-*.xml` à `shop/10-*.xml` : créations et évolutions de la branche
  `feature/ajoutBoutique` : catalogue, commandes, paiements, clé de checkout,
  images, responsable boutique, statuts des articles, remboursements,
  conversations, commandes fournisseur, stock et coûts d'achat. Le dernier
  changeset aligne les devises historiques `CHAR(3)` sur les mappings JPA
  `VARCHAR(3)` pour permettre la validation Hibernate.

Les doublons de bootstrap et les corrections de baseline propres à Flyway ne
sont pas repris : les créations s'exécutent toujours avant leurs évolutions.
Les anciens SQL sont conservés dans `app/sql/legacy-flyway` pour référence et
ne sont plus exécutés par l'application. La réparation des rôles au démarrage
est remplacée par le changeset `shop-04-roles`.

## Bases existantes

Les préconditions `MARK_RAN` adoptent les tables, colonnes, index, séquences et
clés étrangères déjà présents sans les recréer. Les objets manquants sont
créés. Les évolutions des contraintes et du stock sont exécutées une fois par
Liquibase ; le statut des articles n'est initialisé que si la colonne manque.
Une table existante est supposée conforme au schéma historique : les
préconditions ne corrigent pas arbitrairement ses colonnes. Hibernate vérifie
ensuite la compatibilité des mappings et arrête le démarrage en cas d'écart.

Avant le premier déploiement, sauvegarder la base et essayer cette version sur
une copie. Retirer les éventuelles surcharges `SPRING_JPA_HIBERNATE_DDL_AUTO=update`
et `SPRING_JPA_GENERATE_DDL=true` pour conserver la gestion par Liquibase.
Conserver `flyway_schema_history` pour l'historique ; aucune synchronisation
globale (`changelogSync`) n'est nécessaire. Le premier lancement crée
`databasechangelog` et `databasechangeloglock` automatiquement.

## Évolutions suivantes

Ajouter un nouveau fichier avec des identifiants de changesets uniques, puis
un `include` explicite à la fin du master. Ne pas modifier un changeset déjà
déployé : Liquibase vérifie son checksum. Ajouter un changeset correctif.

## Vérification

`LiquibaseMigrationTest` vérifie la création complète, la validation de toutes
les entités JPA, le redémarrage, la reprise du socle et la conservation des
données boutique. Pour PostgreSQL, utiliser une base **jetable** avec
`MIGRATION_TEST_URL=jdbc:postgresql://localhost:5432/migration`, utilisateur et
mot de passe `migration`, puis lancer `mvn -Dtest=LiquibaseMigrationTest test`.
Chaque scénario crée son propre schéma dans cette base. Sans cette variable,
les tests utilisent H2 en mode PostgreSQL ; la réparation des rôles via les
catalogues PostgreSQL est vérifiée uniquement sur PostgreSQL.
