# Schéma et conservation des données

Source : [guide Liquibase](../../app/src/main/resources/db/changelog/README.md).

### DATA-001 — Le schéma évolue par nouveaux changesets

Statut : Documentée.
Liquibase gère le schéma et Hibernate le valide. Une correction d'un changeset déployé
utilise un nouveau changeset, ajouté à la fin du point d'entrée V2.
Exemple : redémarrer après migration ne rejoue pas la création du schéma.

### DATA-002 — La migration conserve les données et valide les mappings

Statut : Documentée.
Tester la création complète, la reprise du socle V1, la conservation des données et le redémarrage.
Exécuter ces scénarios sur PostgreSQL jetable en complément des tests H2.
Exemple : des commandes présentes avant migration restent disponibles après migration.

## Limites

Les migrations automatiques ne remplacent pas une répétition sur une copie appropriée de la base
avant déploiement. Aucun contrôle de CI n'utilise les données ou les secrets de production.
