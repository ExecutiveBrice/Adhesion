
#Installation des dépendances node
npm install 

#Démarage local
npm start

## Déployer le front sous `/adhesion/`

Depuis la racine du dépôt, après installation des dépendances dans `client` :

```sh
npm --prefix client run build:production
```

Nginx monte `client/dist/browser` en lecture seule. Si le build est effectué sur une autre machine, transférez le contenu complet de ce dossier sur le serveur. Si le déploiement utilise déjà un autre dossier pour les fichiers compilés, définissez `ADHESION_FRONTEND_DIR` dans le `.env` de Compose avec son chemin sur le serveur.

Depuis le répertoire du `docker-compose.yml` sur le serveur, indiquez le chemin monté dans `front_dir`, vérifiez les fichiers puis relancez le front.

```sh
front_dir=./client/dist/browser
test -s "$front_dir/index.html" && \
test -s "$front_dir/ngsw-worker.js" && \
test -s "$front_dir/ngsw.json" && \
docker compose up -d --no-deps client_adhesion
```



#Exemple de demande Codex :
Corrige uniquement le problème de sérialisation dans
src/main/java/org/farmeo/telepac/service/FileService.java.

Contexte :
- Spring Boot 4.1
- Jackson 3
- ne modifie pas les autres modules
- ne mets à jour aucune dépendance

Validation :
- exécute seulement FileServiceTest
- ne lance pas toute la suite Maven
- arrête-toi après deux tentatives infructueuses
