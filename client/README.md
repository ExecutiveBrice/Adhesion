
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
