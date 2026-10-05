# Schéma de la base de données

`db.changelog-master.xml` est le point d'entrée configuré dans Spring Boot.
Liquibase s'exécute avant JPA ; Hibernate utilise `ddl-auto: validate` et ne
crée ni ne modifie les tables.

## Organisation

- `changeset-v1.xml` décrit la version actuellement en production, arrêtée au
  commit `8a2cb14325940a31d9144dd50b5a20c9795725f0`. Il inclut le schéma historique
  complet de `baseline/master-changeset.xml`.
- `changeset-v2.xml` est le point d'entrée unique des évolutions postérieures.
  Il les ordonne par module dans `v2/activity.xml`, `v2/shop.xml`,
  `v2/chat.xml` et `v2/communication.xml`.
- Chaque fichier de module V2 contient un seul changeset et uniquement des
  opérations Liquibase XML, sans balise `sql` ni fichier SQL externe.

La V2 part exclusivement du schéma V1 de production. Les états intermédiaires
des branches de développement ne sont donc pas rejoués. La reprise des données
existantes des activités vers les sections est effectuée pendant la migration,
puis le schéma final des modules est créé directement.

## Bases existantes

La V1 adopte les tables historiques déjà présentes grâce à ses préconditions.
Les changesets V2 exigent ensuite une base au niveau V1 et s'arrêtent si une
table propre à la V2 existe déjà sans avoir été enregistrée par Liquibase.
Hibernate vérifie enfin la compatibilité des mappings et arrête le démarrage en
cas d'écart.

Avant le premier déploiement, sauvegarder la base et essayer cette version sur
une copie. Retirer les éventuelles surcharges `SPRING_JPA_HIBERNATE_DDL_AUTO=update`
et `SPRING_JPA_GENERATE_DDL=true` pour conserver la gestion par Liquibase.
Conserver `flyway_schema_history` pour l'historique ; aucune synchronisation
globale (`changelogSync`) n'est nécessaire. Le premier lancement crée
`databasechangelog` et `databasechangeloglock` automatiquement.

## Évolutions suivantes

Ajouter les prochaines évolutions dans un nouveau changeset de version et des
fichiers par module, puis l'inclure à la fin du master. Ne pas modifier un
changeset déjà déployé : Liquibase vérifie son checksum. Ajouter un changeset
correctif.

## Vérification

`LiquibaseMigrationTest` vérifie la création complète, la validation de toutes
les entités JPA, le redémarrage, la reprise du socle et la conservation des
données boutique. Pour PostgreSQL, utiliser une base **jetable** avec
`MIGRATION_TEST_URL=jdbc:postgresql://localhost:5432/migration`, utilisateur et
mot de passe `migration`, puis lancer `mvn -Dtest=LiquibaseMigrationTest test`.
Chaque scénario crée son propre schéma dans cette base. Sans cette variable,
les tests utilisent H2 en mode PostgreSQL.
