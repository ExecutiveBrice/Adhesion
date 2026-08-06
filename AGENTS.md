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
