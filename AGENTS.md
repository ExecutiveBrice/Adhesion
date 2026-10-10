# Travailler sur Adhesion

## Avant de modifier

- Lire `docs/README.md`, puis les fiches fonctionnelles des domaines concernés. Utiliser `docs/functional/coverage.json` pour retrouver les tests associés.
- Identifier les règles affectées par leur ID, les autorisations, les effets sur les domaines voisins et les données existantes. Pour un bug, reproduire le défaut avec un test avant de le corriger lorsque c'est possible.
- Distinguer les règles documentées, les comportements observés et les propositions. Ne pas transformer un comportement observé ou une question ouverte en exigence validée. En cas de contradiction métier non résolue, expliciter le choix nécessaire.

## Réaliser une évolution

- Préserver les règles existantes sauf changement fonctionnel explicitement demandé. Une modification de test doit être justifiée par ce changement, jamais par le seul besoin de faire passer la suite.
- Une fixture devenue incomplète ou une assertion technique obsolète peut être corrigée sur preuve du comportement existant. Expliquer cette preuve dans le compte rendu et conserver les assertions métier ; ne pas désactiver le test.
- Modifier ensemble code, spécification, critères d'acceptation et liens de couverture. Garder les IDs des règles ; ajouter un nouvel ID pour une nouvelle règle.
- Garder les contrôleurs minces. Extraire les décisions métier dans `domain/<domaine>` lorsqu'elles peuvent être testées sans serveur, base ou prestataire. La boutique conserve ses frontières `catalog`, `order`, `payment`.
- Les gardes Angular améliorent la navigation ; les autorisations et le périmètre des données doivent être vérifiés côté serveur à chaque requête. Ne pas élargir un rôle ou une route publique implicitement.
- Ajouter les migrations dans un nouveau changeset Liquibase ; ne pas modifier un changeset déployé. Ne pas modifier les clients générés sous `target/`.
- Utiliser des données fictives et des bases jetables pour les tests. Ne pas utiliser les `.env`, secrets, sauvegardes ou données de production comme fixtures.
- Séparer les refactorings étendus des nouvelles fonctionnalités. Ne pas introduire une nouvelle abstraction sans responsabilité concrète.

## Vérifier et livrer

- Suivre `docs/quality/definition-of-done.md` et `docs/quality/validation.md`.
- À la racine : `node scripts/check-specs.mjs` et `git diff --check`.
- Backend (Java 25, Maven 3.9+) : dans `app`, `mvn -B verify`. Les tests d'architecture font partie de cette suite.
- Frontend (Node 24) : dans `client`, `npm ci`, `npm run test:ci`, `npm run build:production`.
- Parcours navigateur : dans `client`, `npx playwright install chromium`, puis `npm run test:browser`. Ces tests simulent l'API : ne pas les présenter comme une validation du serveur ou d'HelloAsso.
- Migration : avec une base PostgreSQL jetable dédiée et `MIGRATION_TEST_URL`, lancer dans `app` `mvn -B -Dtest=LiquibaseMigrationTest test` (utilisateur et mot de passe `migration`).
- Exécuter les contrôles adaptés au changement, puis les contrôles de livraison requis. Signaler précisément les contrôles non exécutés ou en échec ; ne pas déclarer une validation complète dans ce cas.
- Dans le compte rendu : comportement obtenu, IDs concernés, preuves de validation, limites et éventuelles décisions métier restantes.
