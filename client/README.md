
#Installation des dépendances node
npm install 

#Démarage local
npm start

## Paiement HelloAsso en local

Le Checkout HelloAsso exige des URL de retour HTTPS. Démarrer le backend sur le port 8000 avec le profil `helloasso-sandbox`, puis lancer :

```sh
npm run start:helloasso
```

Ouvrir `https://localhost:4200/adhesion/` et accepter le certificat de développement du navigateur. Ce démarrage sert Angular en HTTPS et transmet `/adhesion/api/**` au backend local. Le démarrage habituel `npm start` reste en HTTP et ne peut pas lancer un Checkout HelloAsso.

#Build des sources pour dépot docker (local ou remote)
npm run build --prod



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
