# Chats

Toutes les discussions utilisent une seule entité `Chat`. Plusieurs chats peuvent être créés : chacun est rattaché à une section, à une activité, ou à aucune des deux pour un chat global. Les messages et les repères de lecture référencent uniquement son identifiant.

## Accès

La lecture est accordée si au moins une des conditions suivantes est satisfaite :

- Le compte est lié à l’activité du chat en tant qu’adhérent, encadrant, référent d’activité ou référent de la section de cette activité.
- Pour un chat de section, le compte est référent de cette section ou lié à une activité de cette section.
- Une permission explicite est configurée pour l’un de ses rôles.

Les adhésions en liste d’attente et annulées ne créent pas de lien. Les autres états et le surclassement conservent la lecture. Les relations d’encadrement et de référent d’activité proviennent des affectations existantes. Les référents de section sont affectés dans Administration → Sections, indépendamment des rôles globaux du compte.

Les permissions par rôle sont additives et indépendantes du rattachement pour **tous les rôles**, y compris `ROLE_USER`, `ROLE_ENCADRANT`, `ROLE_REFERENT_ACTIVITE` et `ROLE_REFERENT_SECTION`. Cocher un rôle accorde la lecture à tous les comptes qui le possèdent, même hors de l’activité ou de la section. Une permission comporte en plus le droit d’écriture ; la présence d’une permission signifie le droit de lecture. Un rattachement seul ouvre la lecture, jamais l’écriture. Aucun rôle administratif ne contourne ces règles.

Un chat global utilise uniquement les permissions par rôle pour définir son audience. Pour le rendre accessible à tous les adhérents, ajouter `ROLE_USER` en lecture. Un chat rattaché peut être créé sans permission explicite ; un chat global doit avoir au moins un rôle en lecture. La visibilité et le droit d’écriture sont revérifiés à chaque requête. La révocation d’un lien retire l’accès sauf si une autre liaison ou une permission par rôle l’accorde encore.

## Administration et API

La gestion utilise `GET/POST /param/chats` et `PUT/DELETE /param/chats/{id}`. La configuration comporte `nom`, `cible` (`SECTION`, `ACTIVITE`, `ASSOCIATION` pour global), `cibleId` et `permissions` (`role`, `ecriture`). Un chat global ne porte aucun identifiant de cible. Une contrainte en base interdit tout rattachement incohérent ou simultané à une section et une activité.

L’API des comptes authentifiés utilise le même identifiant positif pour tous les chats :

- `GET /chat` : chats accessibles avec `unreadCount` et `canWrite`.
- `GET /chat/{id}/messages` : 50 derniers messages dans l’ordre chronologique ; `beforeId` ou `afterId` permettent de paginer, sans les combiner.
- `POST /chat/{id}/messages` avec `{ "content": "Bonjour" }` : message de 1 à 2000 caractères (HTTP 201). L’identité provient de la session.
- `POST /chat/{id}/read` avec `{ "lastMessageId": 123 }` : accusé de lecture pour un message de ce chat. Le repère ne recule jamais.

Les messages sont affichés comme texte, sans interprétation HTML. L’actualisation a lieu toutes les cinq secondes ; une erreur d’envoi conserve le brouillon. Les compteurs ignorent les messages du compte connecté et sont partagés entre appareils.

## Migration

La migration `09-unify_chats.xml` renomme les tables en `chats`, `chat_permissions`, `chat_messages` et `chat_reads`, en conservant les identifiants, messages, permissions et repères des chats gérés. Elle supprime les colonnes et tables des anciens canaux automatiques, déjà retirés par la migration 08. Elle crée `section_referents` pour les affectations explicites des référents de section. Les anciens changelogs restent inchangés.

Les permissions déjà enregistrées deviennent toutes indépendantes du rattachement, y compris `ROLE_USER` : un tel droit explicite donne donc désormais accès à tous les adhérents. Retirer cette permission pour limiter la lecture aux seules personnes liées au rattachement. La suppression d’un chat supprime ses messages, permissions et repères. La suppression d’une section ou activité supprime les chats directement rattachés ; les chats globaux restent conservés.
