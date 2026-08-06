# AGENTS.md — Projet Adhesion

## Contexte fonctionnel

Adhesion est une application de gestion associative avec un backend Spring Boot et un frontend Angular.

## Conventions techniques

### Backend

- Préserver les routes existantes et leur sécurité ; une nouvelle route doit utiliser les mêmes mécanismes d’authentification
- Lorsqu’une relation est reçue via l’API, résoudre l’entité gérée en base avant de sauvegarder.
- Ajouter ou mettre à jour les tests de service pour les règles métier nouvelles.

### Frontend

- Vérifier que les contrôles utilisent des comparateurs adaptés pour les objets sélectionnés.
- Garder les libellés utilisateur en français.

## Vérification avant livraison

Avant de commiter :

1. Exécuter `git diff --check`.
2. Construire le frontend avec `npm run build` dans `client/`.
3. Compiler ou démarrer le backend pour détecter les erreurs de compilation.
5. Ne jamais ajouter `.DS_Store`, fichiers d’IDE, secrets ou fichiers de configuration locaux au commit.

## Git et pull requests

- Vérifier `git status -sb` avant toute opération.
- Ne pas inclure de fichiers non liés à la demande.
- Faire des commits concis et centrés sur une fonctionnalité.
- Lorsqu’une PR existe déjà, pousser sur sa branche plutôt que d’en créer une nouvelle.

# Développement local

## Prérequis

- Docker Desktop lancé ;
- Java 21+ et Maven ;
- Node.js et les dépendances déjà installées dans `client/node_modules` ;
- un fichier `.env` local, non versionné, renseignant au minimum :
  `DB_NAME`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`,
  `JWT_SECRET`, ainsi que les autres variables utilisées par `docker-compose.yml`.

Ne jamais afficher, modifier ou versionner les valeurs de `.env`.

## Configuration Docker locale

Le fichier `docker-compose.local.yml` est la surcharge à toujours utiliser en
local. Il corrige les différences avec l'environnement Ubuntu de production :

- expose l'API sur `localhost:8000` ;
- désactive le montage `/home/ubuntu/adhesion`, indisponible sous macOS ;
- publie la base Docker secondaire sur `localhost:8106` pour ne pas entrer en
  conflit avec une base existante sur `8105` ;
- raccorde l'API à la base locale existante sur le port `8105`, via
  `host.docker.internal` (et non `127.0.0.1`, qui désigne le conteneur lui-même).

Si le réseau partagé n'existe pas, le créer une seule fois :

```sh
docker network create traefik_web
```

## Démarrage

Depuis la racine du dépôt :

```sh
cd app
mvn package -DskipTests -q
cd ..
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
cd client
npm start -- --host 127.0.0.1 --port 4201
```

L'interface est disponible sur `http://localhost:4201/adhesion/`.
L'API est disponible sur `http://localhost:8000`.

Le port `4200` peut être occupé par une autre application locale : conserver
le port `4201` pour Adhesion. Une réponse HTTP `401` de l'API sans jeton de
connexion est attendue et confirme que le serveur répond.

## Vérifications

```sh
cd app && mvn package -DskipTests -q
cd ../client && npm run build
cd .. && docker compose -f docker-compose.yml -f docker-compose.local.yml ps
```

`mvn test` peut échouer sous le JDK 25 local, car Mockito ne parvient pas à
attacher son agent. Utiliser Java 21 pour exécuter la suite de tests complète.

## Arrêt

Arrêter le serveur Angular avec `Ctrl+C`, puis depuis la racine :

```sh
docker compose -f docker-compose.yml -f docker-compose.local.yml down
```

Ne pas supprimer les volumes Docker ni arrêter la base PostgreSQL existante
sur le port `8105` sans demande explicite : elle peut être partagée avec une
autre session de développement.

## GitHub : pratique fiable et économe

Avant toute publication, vérifier une seule fois le contexte local :

```sh
git status -sb
git diff --stat
gh auth status
```

- Ne jamais utiliser `git add -A` dans un répertoire de travail mêlant des
  modifications existantes et une nouvelle tâche. Ajouter explicitement les
  fichiers de la fonctionnalité concernée.
- Lire le diff des seuls fichiers à publier et lancer `git diff --check` avant
  le commit. Exécuter ensuite uniquement les vérifications pertinentes, sans
  répéter les builds déjà réussis.
- Pour mettre à jour une PR existante, identifier et pousser sa **branche
  source**, pas une branche locale de convenance. Exemple pour la PR 48 :

  ```sh
  git push origin HEAD:agent/gestion-seances-calendrier-google
  ```

- Sur macOS, exécuter `gh auth status` avec accès au trousseau système
  (hors sandbox) dès la première vérification. Une erreur d'authentification
  depuis l'environnement isolé ne prouve pas qu'un jeton est invalide : elle
  peut seulement signifier que le trousseau macOS est inaccessible.
- Ne demander `gh auth login -h github.com` à l'utilisateur qu'après l'échec
  de cette vérification avec accès au trousseau. Ne pas faire plusieurs
  tentatives identiques dans le sandbox.
- Après un push, une seule vérification ciblée suffit :

  ```sh
  git ls-remote --heads origin <branche-source>
  ```

Pour limiter la consommation de tokens, ne pas relire toute la PR, tous les
logs CI ou tout l'historique Git lorsqu'un statut, un diff ciblé et la branche
source répondent déjà à la question. Ne récupérer les commentaires de revue ou
les logs d'échec CI que si la tâche le demande explicitement.

## Diagnostic local : une passe courte avant toute relance

Les problèmes de Docker, ports, navigateur et base de données sont coûteux à
diagnostiquer lorsqu'ils sont traités par essais successifs. Appliquer cette
séquence, dans cet ordre, et s'arrêter dès qu'une preuve est obtenue.

### 1. État connu avant action

Exécuter **une seule fois** les contrôles concis suivants :

```sh
docker compose -f docker-compose.yml -f docker-compose.local.yml ps
lsof -nP -iTCP:4200 -sTCP:LISTEN
lsof -nP -iTCP:4201 -sTCP:LISTEN
lsof -nP -iTCP:8000 -sTCP:LISTEN
curl --silent --output /dev/null --write-out 'HTTP %{http_code}\n' http://127.0.0.1:8000/activite/all
```

Interprétation :

- `401` sur l'API signifie que le backend répond mais que la route exige une
  authentification ; ce n'est pas une panne.
- Un port occupé ne doit pas être libéré en arrêtant un processus inconnu.
  Utiliser le port prévu `4201` pour Angular et conserver `4200` intact.
- Ne lancer `docker compose up --build` que si le JAR ou le Dockerfile a
  changé. Sinon, utiliser `docker compose ... up -d`.

### 2. Docker et base de données

- Toujours utiliser `docker-compose.local.yml` : il neutralise le volume
  Ubuntu, publie l'API sur `8000` et évite le conflit PostgreSQL sur `8105`.
- La base de test existante est accessible depuis le conteneur uniquement via
  `host.docker.internal:8105`, jamais via `127.0.0.1:8105`.
- Ne créer le réseau `traefik_web` qu'après avoir constaté explicitement qu'il
  est absent. Ne pas recréer les volumes ou la base pour résoudre un problème
  d'authentification.
- En cas d'échec, consulter d'abord un extrait borné :

  ```sh
  docker compose -f docker-compose.yml -f docker-compose.local.yml logs --tail=80 app_adhesion
  ```

  Ne demander des logs plus longs que si les 80 dernières lignes ne montrent
  pas l'erreur de connexion ou le message de démarrage.

### 3. Interface et navigateur

- Vérifier l'URL et le serveur avant toute inspection navigateur :

  ```sh
  curl --silent http://127.0.0.1:4201/adhesion/ | head -20
  ```

  La page doit contenir le titre `ALOD`. Un autre titre indique qu'un autre
  projet utilise le port ; démarrer Adhesion sur `4201`, sans tuer ce projet.
- Ne démarrer qu'une seule instance Angular : vérifier le port avec `lsof`
  avant `npm start`.
- Utiliser le navigateur seulement si `curl` confirme que la bonne interface
  est servie mais que le rendu reste incorrect. Lire d'abord le DOM et les
  erreurs de console, sans captures d'écran, navigation répétée ni inspection
  de stockage local.


