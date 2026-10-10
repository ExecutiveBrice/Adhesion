# Validation locale et continue

## Prérequis

- Java 25 et Maven 3.9+ ; `JAVA_HOME` doit désigner Java 25.
- Node 24, npm et Chrome pour Karma. Installer les dépendances avec `npm ci` dans `client`.
- Chromium pour Playwright : `npx playwright install chromium` dans `client`.
- PostgreSQL 17 jetable pour la vérification des migrations sur le moteur de production.

## Commandes

| Contrôle | Répertoire | Commande |
| --- | --- | --- |
| Liens, règles et références de tests | Racine | `node scripts/check-specs.mjs` |
| Vérificateur du référentiel | Racine | `node --test scripts/check-specs.test.mjs` |
| Backend et architecture | app | `mvn -B verify` |
| Angular | client | `npm run test:ci` |
| Build de production | client | `npm run build:production` |
| Parcours navigateur | client | `npm run test:browser` |
| Migrations PostgreSQL | app | `mvn -B -Dtest=LiquibaseMigrationTest test` avec MIGRATION_TEST_URL |

Mockito est chargé comme agent au démarrage du processus de test, pour éviter l'attachement dynamique
sur Java 25. La version provient de la gestion des dépendances Spring Boot.

## Ce que couvrent les parcours navigateur

Playwright démarre une instance Angular dédiée sur `127.0.0.1:4300` et un contexte vierge par test.
Les fixtures interceptent l'API locale `localhost:8000`, utilisent des données fictives et refusent
les requêtes non prévues. Les exceptions Angular échouent aussi le test.

Les parcours vérifient connexion, refus d'une liste gérée sans affectation, consultation du référent,
catalogue → panier → commande, conservation du panier après erreur, attente et confirmation de paiement.
Le JWT est fictif et l'API simulée : ces tests ne prouvent pas l'authentification, la sécurité serveur,
le débit HelloAsso ou la chaîne complète navigateur + base. Les tests Java vérifient les règles serveur.

## PostgreSQL jetable

Créer un conteneur dédié, sans réutiliser le docker-compose de déploiement ni une base existante :

```sh
docker run --rm --name adhesion-migration-tests -e POSTGRES_DB=migration -e POSTGRES_USER=migration -e POSTGRES_PASSWORD=migration -p 55432:5432 postgres:17
```

Dans un autre terminal, définir `MIGRATION_TEST_URL=jdbc:postgresql://localhost:55432/migration`
puis lancer les tests de migration dans `app`. Sous PowerShell :

```powershell
$env:MIGRATION_TEST_URL = 'jdbc:postgresql://localhost:55432/migration'
mvn -B -Dtest=LiquibaseMigrationTest test
Remove-Item Env:MIGRATION_TEST_URL
```

Le compte de test est `migration` / `migration`. Chaque scénario utilise un schéma distinct.
Arrêter le conteneur de test à la fin. La CI possède son propre service jetable.

## GitHub Actions et blocage des fusions

[quality.yml](../../.github/workflows/quality.yml) s'exécute sur les PR, pushes, groupes de fusion
et lancements manuels. Il lance cinq validations et un contrôle agrégé nommé **Quality gate**.
Ce contrôle échoue si une validation échoue, est annulée ou ignorée. Les résultats Java et traces navigateur
sont conservés sept jours. Aucun secret de production n'est nécessaire.

Le fichier de workflow ne peut pas activer lui-même la protection du dépôt. Après sa publication et un
premier passage, configurer dans GitHub un ruleset de la branche d'intégration ou de livraison :

1. Exiger une pull request avant fusion.
2. Exiger le status check **Quality gate**, produit par GitHub Actions.
3. Exiger une branche à jour avant fusion, ou utiliser une merge queue.
4. Empêcher les contournements qui rendraient les contrôles facultatifs.

Ne pas activer un contrôle obligatoire inexistant avant le premier passage du workflow.
L'activation du ruleset est une opération d'administration distante distincte du contenu du dépôt.
Référence : [contrôles obligatoires GitHub](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches).

## Avant livraison

Consulter [la définition de terminé](definition-of-done.md). Pour les paiements,
effectuer aussi [le parcours sandbox HelloAsso](../../app/SHOP_PAYMENTS.md).
Pour un changement de schéma, répéter la migration avant déploiement sur une copie appropriée de la base.
