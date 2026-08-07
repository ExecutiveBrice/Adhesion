# AGENTS.md — Projet Adhesion

## Objet et périmètre

Ce fichier s'applique à tout le dépôt. Adhesion est une application de gestion
associative : adhérents et tribus, inscriptions, activités, séances et
présences, paiements, documents, courriels et reporting.

Avant toute modification, lire les fichiers proches du code concerné et
vérifier l'état Git. Le dépôt peut contenir des travaux locaux sans rapport avec
la tâche : les préserver et limiter le diff au besoin exprimé.

## Carte du dépôt

- `app/` : backend Java 21, Spring Boot/Spring Cloud, Maven, JPA/PostgreSQL,
  Spring Security avec JWT.
- `app/src/main/java/com/wild/corp/adhesion/controllers/` : API REST.
- `app/src/main/java/com/wild/corp/adhesion/services/` : règles métier et
  limites transactionnelles.
- `app/src/main/java/com/wild/corp/adhesion/repository/` : accès Spring Data.
- `app/src/main/java/com/wild/corp/adhesion/models/` : entités JPA ; les objets
  d'échange spécifiques sont sous `models/resources/`.
- `app/src/main/resources/` : configuration, modèles de courriel/PDF et
  spécifications OpenAPI utilisées par Maven.
- `app/src/test/` : tests JUnit 5, AssertJ et Mockito.
- `client/` : frontend Angular 17 en modules Angular, TypeScript strict, RxJS,
  Bootstrap/ng-bootstrap et Karma/Jasmine.
- `client/src/app/_services/` : appels HTTP ; `client/src/app/models/` : contrats
  TypeScript ; `page/` et `template/` : écrans et composants.
- `docker-compose.yml` : déploiement ; `docker-compose.local.yml` : surcharge
  de développement.

Ne jamais modifier manuellement `app/target/`, `client/dist/`,
`client/node_modules/` ni les clients générés sous `app/target/generated-sources/`.
Pour changer un client généré, modifier sa spécification JSON dans
`app/src/main/resources/`, puis relancer Maven.

## Principes de modification

- Faire le changement minimal complet ; éviter les renommages, reformattages
  ou migrations techniques sans rapport avec la demande.
- Respecter le style du fichier existant. Aucun formateur ou linter global
  n'est configuré ; ne pas inventer une commande `lint`.
- Conserver les fichiers en UTF-8 et les libellés visibles par l'utilisateur en
  français.
- Toute évolution de contrat API doit être répercutée dans le service Angular,
  les modèles TypeScript et les tests concernés.
- Ne pas casser les URL publiques : le client est servi sous `/adhesion/`,
  utilise un routage Angular avec hash et appelle l'API à partir de
  `environment.server`.

## Backend

- Garder les contrôleurs minces : validation et traduction HTTP dans le
  contrôleur, logique métier dans un service, persistance dans un repository.
- Pour une nouvelle classe, préférer l'injection par constructeur. Ne pas
  convertir les injections existantes en masse dans une tâche sans rapport.
- Préserver les routes, verbes, paramètres, statuts HTTP et formats de réponse
  existants sauf évolution explicitement demandée. Pour une nouvelle route,
  préférer les annotations Spring `@PathVariable` et `@RequestParam`.
- Vérifier la sécurité à deux endroits : `WebSecurityConfig` et les
  `@PreAuthorize` du contrôleur. Une route d'écriture ne doit jamais devenir
  publique par défaut. Les rôles sont centralisés dans `ERole` et initialisés
  au démarrage de l'application.
- Ne pas accepter aveuglément un graphe d'entités JPA provenant du client.
  Résoudre par identifiant les entités liées, modifier les instances gérées et
  utiliser un DTO de `models/resources/` lorsque le contrat ne correspond pas
  exactement à une entité.
- Lors d'un changement de relation, examiner les deux côtés de l'association,
  les cascades, `orphanRemoval`, les contraintes d'unicité et les annotations
  Jackson. Éviter les suppressions implicites et les boucles de sérialisation.
- Les services métier sont généralement transactionnels. Garder ensemble les
  mises à jour qui doivent réussir ou échouer comme une seule opération.
- Pour les séances et calendriers, utiliser `java.time`, des bornes explicites
  et des dates fixes dans les tests. Préserver les règles liées à la zone
  scolaire, aux vacances, aux jours fériés et à Google Agenda.
- Les appels Brevo, HelloAsso, Google Agenda et jeux de données publics doivent
  être simulés dans les tests. Ne jamais envoyer de vrai courriel ni appeler un
  service externe depuis une suite de tests.
- Ajouter un test de service ciblé pour chaque règle métier corrigée ou ajoutée,
  notamment sur les relations, suppressions, autorisations, dates et cas
  limites.

## Frontend

- Le projet utilise encore `AppModule` et des composants non standalone : ne
  pas introduire une architecture standalone isolée sans demande de migration.
- Respecter le mode strict TypeScript et typer les réponses HTTP. Éviter `any`
  dans le nouveau code lorsqu'un modèle ou une interface locale suffit.
- Centraliser les appels API dans `_services/`, construire les paramètres avec
  `HttpParams` et ne jamais coder en dur l'hôte du backend dans un composant.
- Garder les modèles TypeScript alignés sur les DTO/réponses Java, y compris la
  pagination Spring (`content`, `number`, `size`, `totalElements`,
  `totalPages`).
- Pour les listes potentiellement volumineuses, préférer pagination et filtrage
  côté serveur. Ne pas réintroduire un chargement complet si une route paginée
  existe.
- Nettoyer les souscriptions longues (`takeUntil`, `async` pipe ou mécanisme
  équivalent) et conserver les retours utilisateur via les composants déjà
  utilisés, notamment Toastr.
- Pour les sélecteurs d'objets, définir un comparateur stable basé sur
  l'identifiant lorsque l'égalité par référence n'est pas garantie.
- Une évolution visible doit rester utilisable aux largeurs d'écran déjà
  supportées et conserver des états explicites de chargement, vide et erreur.
- Ajouter ou adapter les tests Jasmine ciblés. Les nombreux tests de création de
  composant ne remplacent pas un test du comportement modifié.

## Données sensibles et effets externes

- Les `.env*` sont locaux et ignorés par Git. Ne jamais afficher, copier,
  modifier ou versionner leurs valeurs. Ne jamais ajouter de secret dans
  `application.yml` ou les fichiers `environment*.ts`.
- Les données d'adhérents, adresses, courriels, paiements, documents, jetons JWT
  et liens d'usurpation sont sensibles. Ne pas les écrire dans les logs, jeux de
  tests ou messages d'erreur.
- Ne pas affaiblir CORS, JWT, les contrôles de rôles ou la validation des
  fichiers sans demande explicite et justification.
- Ne pas lancer d'opération de nettoyage annuel, suppression de données,
  envoi de courriels, synchronisation Google ou régénération globale pendant
  une vérification locale.
- Ne jamais supprimer un volume Docker ni réinitialiser une base sans demande
  explicite.

## Développement local

Prérequis : JDK 21, Maven installé globalement (aucun wrapper n'est versionné),
Node.js/npm et Docker Compose. Installer les dépendances du client depuis
`client/` avec `npm install` seulement lorsque nécessaire.

Commandes usuelles :

```text
# Backend — depuis app/
mvn test
mvn package -DskipTests

# Frontend — depuis client/
npm start -- --host 127.0.0.1 --port 4201
npm run build
npm test -- --watch=false --browsers=ChromeHeadless

# Stack locale — depuis la racine, après construction du JAR
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.local.yml ps
docker compose -f docker-compose.yml -f docker-compose.local.yml logs --tail=80 app_adhesion
```

Le frontend local est attendu sur `http://127.0.0.1:4201/adhesion/` et l'API
sur `http://127.0.0.1:8000` lorsque la surcharge Docker est utilisée. Cette
surcharge dirige actuellement l'API vers une PostgreSQL accessible via
`host.docker.internal:8105`, tandis que le conteneur PostgreSQL du compose est
publié sur `8106` : confirmer la base visée avant de démarrer ou de modifier la
configuration.

## Validation proportionnée

Exécuter au minimum les contrôles correspondant au périmètre :

- Backend seul : test JUnit ciblé, puis `mvn test` si le changement est
  transversal.
- Frontend seul : test Jasmine ciblé lorsque possible, puis `npm run build`.
- Contrat API ou fonctionnalité de bout en bout : tests backend, build frontend
  et vérification des deux côtés du contrat.
- Configuration/Docker : commencer par
  `docker compose -f docker-compose.yml -f docker-compose.local.yml config`,
  puis vérifier l'état sans supprimer les services ou volumes existants.
- Toute livraison : `git diff --check`, inspection du diff ciblé et
  `git status --short`.

Si une vérification n'a pas pu être lancée à cause d'un outil, d'un navigateur,
d'une base ou d'un secret manquant, le signaler clairement ; ne pas présenter
la validation comme réussie.

## Git

- Ne pas écraser, restaurer, mettre en stash ou inclure les changements locaux
  d'un autre travail.
- Ajouter explicitement les seuls fichiers de la tâche ; éviter `git add -A`
  dans un arbre de travail partagé ou sale.
- Ne pas versionner `.env*`, fichiers d'IDE, logs, sorties de build ou données
  exportées.
- Ne créer ni commit, ni branche, ni push, ni pull request sans demande
  explicite.
