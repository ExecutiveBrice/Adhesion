# Chat interne par activité

Le menu **Chat** (`/#/chat`) donne accès au canal **Communication**, au canal **Référents et encadrants** pour les rôles concernés, puis à un salon collectif par activité.
Communication est visible de tous les comptes connectés, même sans adhésion validée. Les rôles `ROLE_BUREAU`, `ROLE_MEMBRECA`, `ROLE_SECRETAIRE` et `ROLE_COMMUNICATION_GLOBAL` peuvent publier dans Communication ; les trois premiers peuvent aussi publier dans tous les salons d'activité. Les autres rôles, y compris `ROLE_ADMIN` seul, disposent d'un accès limité à leurs activités. Les rôles `ROLE_REFERENT_SECTION` et `ROLE_COMMUNICATION_SECTION` peuvent être attribués aux salons de section dans leur configuration. Ces droits sont vérifiés côté serveur à chaque envoi.
Le canal **Référents et encadrants** est visible et accessible en écriture aux rôles `ROLE_REFERENT_ACTIVITE`, `ROLE_ENCADRANT`, `ROLE_BUREAU`, `ROLE_MEMBRECA` et `ROLE_SECRETAIRE`, sans dépendre d'une adhésion ou d'une activité.
Pour la visibilité des adhérents, tous les états d’adhésion ouvrent l’accès aux chats de l’activité et de sa section, y compris les inscriptions en cours, les retours ALOD et les retours Comité. Seuls « Sur liste d'attente » et « Annulée » excluent cette adhésion. La même règle s’applique au surclassement ; les professeurs et les référents conservent leur rattachement à leurs activités.
Les autres rôles accèdent aux chats selon les cases de visibilité configurées dans l’administration, indépendamment de l’état de leur adhésion. Les droits d’écriture restent configurés séparément. Chaque compte communique au nom de son propre adhérent.

Les messages texte (2000 caractères maximum) sont persistés avec le nom de l'auteur et leur date. Ils sont affichés comme texte, sans interprétation HTML. La première ouverture charge les 50 derniers messages ; le bouton « Messages précédents » remonte dans l'historique. L'actualisation automatique récupère les messages suivants toutes les cinq secondes et s'arrête lorsque l'on quitte la page. Une erreur d'envoi conserve le brouillon.

Chaque bandeau d'activité affiche le nombre de messages non lus, hors messages envoyés par le compte connecté. Une cloche dans le menu Chat signale la présence de messages non lus ; les compteurs sont actualisés toutes les cinq secondes pendant la connexion, même hors de la page Chat. L'ouverture d'un salon dans un onglet visible marque les messages comme lus jusqu'au dernier message chargé, y compris l'historique antérieur. Ce repère est conservé en base par compte et activité, donc partagé entre appareils. À la première consultation, tous les messages des autres membres sont considérés non lus.

## API authentifiée

- `GET /chat/activities` : salons du compte connecté avec `unreadCount`, `communication`, `referentEncadrant` et `canWrite`. Les identifiants `0` et `-1` désignent respectivement les canaux Communication et Référents et encadrants dans les mêmes endpoints de messages et de lecture.
- `GET /chat/activities/{id}/messages` : 50 derniers messages dans l'ordre chronologique.
- Même URL avec `beforeId` : 50 messages précédents ; avec `afterId` : 50 messages suivants. Les deux paramètres sont exclusifs.
- `POST /chat/activities/{id}/messages` avec `{ "content": "Bonjour" }` : création (HTTP 201). L'identité est déterminée par la session côté serveur.
- `POST /chat/activities/{id}/read` avec `{ "lastMessageId": 123 }` : mémorise la lecture jusqu'à ce message et renvoie `unreadCount`. Le message doit appartenir au salon autorisé ; le repère ne peut pas reculer.

Le serveur vérifie l'appartenance lors de chaque lecture et envoi, sauf pour les rôles autorisés dans tous les salons. Une annulation d'adhésion retire l'accès aux prochains appels. Les adresses e-mail, coordonnées et autres données personnelles ne sont pas exposées par l'API du chat.

## Base de données

La migration Liquibase `db/changelog/chat/01-create_activity_chat.xml`, incluse dans le changelog principal, crée la table `activity_chat_messages` et son index `(activite_id, id)` au démarrage. La suppression d'une activité supprime son historique. Le nom de l'auteur est conservé avec le message pour garder l'historique lisible après suppression de son compte.

Cette première version propose uniquement des salons collectifs et des messages texte. Elle ne comprend pas encore de messages privés, pièces jointes, notifications de lecture ou outils de modération.

La migration `db/changelog/chat/02-create_activity_chat_reads.xml` crée `activity_chat_reads`. La suppression du compte ou de l'activité supprime les repères de lecture associés.

La migration `db/changelog/chat/03-create_communication_chat.xml` permet les messages sans activité (`activite_id IS NULL`) pour Communication et crée `communication_chat_reads`. Les messages de Communication restent séparés des historiques d'activités et leurs compteurs de lecture sont conservés par compte.
