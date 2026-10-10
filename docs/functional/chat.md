# Discussions

Acteurs : comptes liés à une activité ou section, rôles explicitement autorisés, administrateurs
et référents chargés du paramétrage. Source métier : [CHAT.md](../../app/CHAT.md).

### CHAT-001 — Un lien ouvre la lecture dans son périmètre

Statut : Documentée.
Le lien à une activité ouvre la lecture de son chat et des chats de sa section. Le référent
d'une section lit aussi ses chats lorsque la section n'a pas d'activité. Les adhésions annulées
ou en liste d'attente ne créent pas de lien ; les autres états et le surclassement le conservent.
Exemple : être lié à Yoga ne donne pas accès au chat d'une activité de Basket.

### CHAT-002 — L'écriture exige une permission explicite

Statut : Documentée.
Un rattachement seul ne permet jamais d'envoyer un message. Une permission pour l'un des rôles
du compte donne la lecture, et l'écriture seulement si son indicateur `ecriture` est activé.
Ces permissions sont additives et indépendantes du rattachement, y compris pour ROLE_USER.
Exemple : ROLE_USER autorisé en écriture permet l'envoi même hors de la section.

### CHAT-003 — Aucun rôle ne contourne les règles de lecture

Statut : Documentée.
Un rôle administratif seul ne donne pas accès aux messages. Chaque opération revérifie les liens
et permissions actuels. La révocation retire l'accès, sauf si un autre lien ou rôle autorisé le conserve.
Exemple : un administrateur sans lien ni permission ne lit pas un chat privé.

### CHAT-004 — Le repère de lecture appartient au chat et ne recule pas

Statut : Documentée.
Un accusé ne peut pas référencer un message d'un autre chat. Les non-lus ignorent les messages
du compte connecté, sont partagés entre appareils et ne réaugmentent pas avec un ancien accusé.
Exemple : après lecture du message 20, l'accusé du message 10 ne rétablit pas des non-lus.

### CHAT-005 — Les messages et la pagination respectent leurs limites

Statut : Documentée.
Un message contient de 1 à 2000 caractères, est affiché comme texte et son auteur vient de la session.
L'historique retourne au plus 50 messages en ordre chronologique. `beforeId` et `afterId` ne se combinent pas.
Exemple : du HTML dans un message reste du texte ; un brouillon est conservé après une erreur d'envoi.

## Questions ouvertes

- Le socle navigateur ne vérifie pas encore le rendu HTML comme texte et le maintien du brouillon.
- La suppression d'un chat et de ses messages reste irréversible : examiner l'impact lors d'une évolution de gestion.
