
#Installation des dépendances node
npm install 

#Démarage local
npm start

## Paiement HelloAsso en local

Le Checkout HelloAsso exige des URL de retour HTTPS. Démarrer le backend sur le port 8000 avec le profil `helloasso-sandbox`, puis lancer :

```sh
npm run start:helloasso
```

Ouvrir `https://localhost:4200/` et accepter le certificat de développement du navigateur. Ce démarrage sert Angular en HTTPS et transmet `/api/**` au backend local. Le démarrage habituel `npm start` reste en HTTP et ne peut pas lancer un Checkout HelloAsso.

#Build des sources pour dépot docker (local ou remote)
npm run build:production

Le lecteur PDF est chargé à l'ouverture de la modale, avec une interface imposée
en français (`language="fr"`), quelle que soit la langue du navigateur.
`angular.json` copie uniquement ses scripts `.min.mjs` et sa traduction `fr` ;
`src/assets/locale/locale.json` référence cette seule traduction. Les polices,
CMaps, images et modules WASM restent disponibles pour lire tous les documents.
Les scripts PDF non minifiés et les variantes ES5 sont exclus : les navigateurs
ciblés dans `.browserslistrc` prennent en charge les scripts modernes. Si ces
cibles changent, revoir également les ressources PDF copiées.

Pour analyser les blocs JavaScript, lancer `npm run build:production -- --stats-json` ;
les statistiques sont écrites dans `dist/browser-stats.json`, hors du répertoire
`dist/browser` à déployer. À titre de comparaison, cette optimisation réduit les
fichiers déployés de 26,15 Mo à 11,22 Mo (tailles non compressées).

Le Compose racine sert le front sur `https://adhesion.${DNS_NAME}/` avec une base
Angular `/`. Traefik réécrit les chemins des pages Angular vers `/index.html`
avec le middleware `client_adhesion_spa`. Nginx sert les fichiers avec sa
configuration par défaut. Les ressources absentes restent en 404 et Traefik
transmet `/api/**` au backend. Lors de l'ajout d'une nouvelle racine de route
Angular, ajouter son préfixe à la règle `client_adhesion_spa` du Compose.
Les pages Angular
utilisent des URL sans fragment, par exemple `/accueil` et `/boutique/commandes`.
Les liens envoyés par le backend utilisent la même racine, configurable avec
`ADHESION_FRONTEND_BASE_URL` (par défaut `https://adhesion.${DNS_NAME}`).



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
