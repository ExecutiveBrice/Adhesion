# Référentiel du projet

Ce référentiel accompagne le code et les tests. Il sert à préparer une évolution,
à comprendre les comportements à préserver et à vérifier les effets sur les autres domaines.

## Point de départ

| Besoin | Document |
| --- | --- |
| Règles pour les contributions et Codex | [AGENTS.md](../AGENTS.md) |
| Comprendre les responsabilités et dépendances | [Architecture](architecture.md) |
| Préparer une évolution | [Modèle de fiche](templates/change.md) |
| Terminer et revoir une modification | [Définition de terminé](quality/definition-of-done.md) |
| Exécuter les contrôles et configurer GitHub | [Validation](quality/validation.md) |
| Consulter les résultats de la mise en place | [Bilan initial](quality/initial-validation.md) |

## Domaines fonctionnels

| Domaine | Fiche | Effets à examiner |
| --- | --- | --- |
| Identité, session et droits | [Accès](functional/access.md) | Tous les domaines, tribu, périmètre des données |
| Adhérents et adhésions | [Adhésions](functional/memberships.md) | Présences, chat, comptabilité, exports |
| Activités, séances et agenda | [Activités](functional/activities.md) | Adhésions, référents, présences, calendrier public |
| Comptabilité | [Comptabilité](functional/accounting.md) | Paiements d'adhésion, période, rapprochement |
| Discussions | [Chat](functional/chat.md) | Affectations, rôles, révocations, non-lus |
| Boutique, commandes et paiements | [Boutique](functional/shop.md) | Stock, prestataire, concurrence, expiration |
| Schéma et conservation des données | [Migrations](functional/migrations.md) | Tous les domaines |

## Statut des règles

- **Documentée** : présente dans une documentation métier préexistante. Sa reprise ne constitue pas une nouvelle validation métier.
- **Observée** : déduite du code ou des tests actuels ; à confirmer avant de la modifier ou de la considérer comme une exigence métier définitive.
- **Proposée** : souhait d'évolution, non applicable au comportement livré tant qu'il n'a pas été accepté.

Les fiches constituent un socle initial, pas une description exhaustive de l'application.
Les questions ouvertes restent explicites. Une évolution demandée peut changer une règle : conserver son ID,
expliquer le nouveau comportement dans la modification et adapter les exemples et les tests.

## Couverture

[coverage.json](functional/coverage.json) associe chaque règle à un test nommé et indique
les lacunes connues. `node scripts/check-specs.mjs` vérifie les IDs, les statuts,
les liens locaux et l'existence des références de tests. Il ne juge pas la qualité des assertions
et n'exécute pas les tests ; la revue et la CI complètent cette vérification.

Ajouter une règle dans sa fiche et dans la matrice dans la même modification. Les tests
unitaires, d'intégration et navigateur ont des responsabilités distinctes : un parcours
avec une API simulée ne couvre pas les autorisations du serveur.
