
#Installation des dépendances node
npm install 

#Démarage local
npm start

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