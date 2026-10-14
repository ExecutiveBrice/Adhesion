http://localhost:8000/swagger-ui/index.html

## Contacts newsletter Brevo

`NewsletterContactService` est un service serveur distinct d’`EmailService`, qui continue à envoyer les emails transactionnels avec le SDK Brevo existant. Son client API est indépendant du client global transactionnel. Il ne crée aucun compte, rôle ou adhérent, notamment pour les personnes extérieures. Aucun état d’abonnement n’est enregistré localement : Brevo reste la référence.

Configurer uniquement sur le serveur (variables transmises au conteneur par `docker-compose.yml`) :

| Variable | Usage |
| --- | --- |
| `BREVO_APIKEY_PRIVATE` | Clé API existante, jamais transmise au navigateur |
| `BREVO_NEWSLETTER_LIST_ID` | Identifiant numérique de la liste **Newsletter ALOD**, à relever dans Brevo |

L’application démarre sans configuration newsletter ; seules ses opérations échouent avec une erreur de configuration. La liste doit déjà exister : le service ne la crée pas à partir de son nom. Les personnes qui demandent l’inscription ont déjà validé leur adresse email dans le parcours appelant : aucun email de confirmation supplémentaire n’est envoyé par ce service.

Les opérations Java disponibles sont :

- `getStatus(email)` lit Brevo et distingue contact absent, non inscrit à cette liste, abonné, désabonné de cette liste et bloqué globalement. Le blocage et `listUnsubscribed` priment sur l’appartenance à la liste.
- `createContact(email)` crée uniquement un contact absent, sans liste et sans modifier les flags de blocage. Un contact existant est conservé tel quel. En cas de création concurrente, son statut est relu, sans utiliser `updateEnabled=true`.
- `addToNewsletter(email)` suit une **demande explicite de la personne avec une adresse déjà validée** : il crée le contact si nécessaire et l’ajoute directement à la liste, sans double opt-in. La mise à jour contient uniquement `listIds` : les autres listes et flags restent conservés. Le statut est relu dans Brevo avant de renvoyer `SUBSCRIBED`. Un abonné existant renvoie `ALREADY_SUBSCRIBED`. Un contact bloqué ou désabonné de cette liste renvoie `RESUBSCRIPTION_REQUIRED` sans mutation : sa réinscription reste un parcours distinct, explicitement confirmé, qui doit être traité dans Brevo. La validation préalable de l’email ne constitue pas une autorisation de lever un désabonnement.
- `synchronize(emails)` déduplique les adresses après suppression des espaces extérieurs et passage en minuscules, puis crée les contacts manquants sans les abonner. Fournir les adresses de destination déjà résolues (adresse du représentant lorsque applicable) : un parent représentant plusieurs enfants n’entraîne qu’une opération. Tous les états d’abonnement existants restent conservés, y compris une personne retirée de la liste sans flag de désabonnement. La synchronisation n’inscrit ni ne réinscrit automatiquement les personnes.

Les parcours `createContact`, `synchronize` et `addToNewsletter` conservent les désabonnements existants. `getStatus` permet de consulter les changements d’abonnement effectués dans Brevo.

La page Administration propose dans « Contacts Brevo » le bouton « Remplir la liste et réabonner les contacts », réservé au rôle ADMIN. Après confirmation, `POST /newsletter/contacts/sync` lance un traitement en arrière-plan ; `GET /newsletter/contacts/sync` permet d’en suivre le bilan, également après retour sur la page. Toutes les adresses des adhérents sont prises en compte, sans filtre d’année, avec l’adresse du représentant quand `emailRepresentant` est activé. Les adresses sont normalisées et dédupliquées ; les adresses absentes ou invalides sont ignorées. Aucun e-mail n’est envoyé par cette opération.

Ce parcours administratif appelle `subscribeFromAdministration` : il crée les contacts absents, ajoute la liste configurée et remet `emailBlacklisted` à `false`. **Ce déblocage e-mail est global dans Brevo**, même si seule la liste Newsletter ALOD est ajoutée. Les autres listes et les paramètres SMS/transactionnels ne sont pas modifiés. Le statut est relu après chaque mise à jour : un désabonnement propre à la liste qui reste présent est signalé dans le bilan et doit être vérifié dans Brevo. Le lancement est consigné dans le journal d’audit.

Un seul lot tourne par instance serveur. En cas d’erreur Brevo, le lot s’arrête et conserve le bilan partiel ; une relance explicite reprend en vérifiant chaque statut. Le suivi est conservé en mémoire jusqu’au prochain lot ou au redémarrage de l’application ; un redémarrage interrompt le traitement. En déploiement multi-instance, le suivi nécessite de rester sur la même instance.

La modale d’un adhérent enregistré utilisant sa propre adresse affiche l’abonnement à Newsletter ALOD, lu directement dans Brevo avec `GET /adherent/{id}/newsletter`. Le cadre newsletter est masqué lorsque la fiche utilise l’adresse du représentant (`emailRepresentant = true` avec un représentant associé). L’abonnement de cette adresse doit être géré depuis la fiche du représentant ; la modification depuis la fiche représentée est refusée côté serveur, y compris pour le secrétariat et les administrateurs. Un adhérent qui possède un représentant mais utilise sa propre adresse conserve le cadre et la gestion de son abonnement. L’adresse est résolue côté serveur depuis la fiche enregistrée. Le titulaire du compte associé à cette adresse, le secrétariat ou un administrateur peut modifier l’abonnement avec `PUT /adherent/{id}/newsletter` (`subscribed`, `expectedEmail`). L’inscription par le titulaire exige une adresse validée ; la désinscription reste possible sans cette validation. Le secrétariat et les administrateurs disposent également du droit d’inscrire une adresse non validée, comme dans le parcours administratif de remplissage. Le serveur contrôle la propriété de l’adresse ou le rôle de gestion et refuse une modification si elle a changé depuis la consultation. Les autres membres du foyer et les autres rôles autorisés à consulter les fiches peuvent lire le statut ; le bouton reste visible mais désactivé quand ils ne peuvent pas modifier l’abonnement de cette adresse.

L’inscription et le réabonnement explicites utilisent `subscribeByOwner`, qui vérifie le résultat Brevo et peut lever le blocage global e-mail après confirmation dans la modale. La désinscription utilise `unlinkListIds` pour retirer uniquement la liste configurée, sans supprimer le contact ni bloquer ses autres communications. Les fiches partageant une adresse partagent cet abonnement. Une réponse incertaine impose une actualisation du statut avant une autre modification ; un désabonnement propre à la liste qui persiste est signalé. Le bouton administratif de remplissage décrit ci-dessus peut réinscrire une adresse retirée par son titulaire lors d’une nouvelle synchronisation explicite.

Les appels contacts sont sérialisés et espacés sous la limite générale de 10 requêtes/s par instance. Les délais de connexion/lecture/écriture sont bornés. `NewsletterApiException` expose le code HTTP, le caractère réessayable et `retryAfter`, sans conserver la réponse Brevo ni ses données personnelles. Une réponse 429 ou un quota restant nul suspend les appels jusqu’au délai indiqué par `Retry-After`/`x-sib-ratelimit-reset` (60 s par défaut). Le lot s’arrête à la première erreur ; il peut être relancé après ce délai, les créations étant idempotentes par consultation préalable. Aucune requête POST n’est rejouée automatiquement en cas de panne ou de réponse incertaine. En cas de plusieurs instances ou de consommateurs partageant la clé, les quotas sont communs : les réponses Brevo restent déterminantes.

Documentation officielle vérifiée le 14 octobre 2026 : [détails d’un contact](https://developers.brevo.com/reference/get-contact-info), [création](https://developers.brevo.com/reference/create-contact), [ajout à une liste par mise à jour du contact](https://developers.brevo.com/reference/update-contact), [limites](https://developers.brevo.com/docs/api-limits) et [en-têtes de quotas](https://developers.brevo.com/docs/limit-headers).

Tests ciblés avec réponses HTTP simulées sous le SDK réel, sans requête réseau à Brevo :

```sh
cd app
mvn -o -q -Dtest=NewsletterContactServiceTest,ParamBooleanServicesTest,AdherentEmailAuthorizationMvcTest,PasswordResetMockMvcTest test
```

## Matériel et emprunts

La page **Matériel** est accessible aux membres du CA (`ROLE_MEMBRECA`), aux administrateurs (`ROLE_ADMIN`) et à la Logistique (`ROLE_LOGISTIQUE`). Elle liste chaque objet, son zonage coloré dans le stock et sa disponibilité. Les filtres permettent de rechercher un objet, un zonage ou un emprunteur.

Les administrateurs et la Logistique configurent les emplacements depuis le bouton **Gérer les zonages** de la page **Matériel**. La modale permet d’ajouter, de modifier et de supprimer les zonages, avec un nom et une couleur. À sa fermeture, la liste du matériel et les zonages sont actualisés. Les objets s’ajoutent depuis le bouton **Ajouter du matériel**, en choisissant leur zonage.

Pour un membre du CA, le bouton **Emprunter** enregistre l’utilisateur connecté. Pour un administrateur, il ouvre une modale de sélection parmi les membres du conseil d’administration, avec son propre nom présélectionné s’il appartient au CA. Le matériel apparaît alors **Emprunté** avec le nom de l’emprunteur et la date. L’emprunteur ou n’importe quel administrateur peut le **Restituer**. Deux utilisateurs ne peuvent pas emprunter simultanément le même objet. Un zonage contenant du matériel ne peut pas être supprimé.

Chaque matériel possède un **Journal d’emprunt**, accessible au CA, aux administrateurs et à la Logistique, avec les emprunteurs, les dates d’emprunt et de retour, et les personnes ayant enregistré ces opérations. Les noms restent conservés même après la suppression d’un compte. Les emprunts encore en cours avant l’ajout du journal sont repris ; les anciens emprunts déjà terminés ne peuvent pas être reconstitués.

Le booléen **Hors service (HS)** est renseigné à l’ajout ou modifié par un administrateur dans la liste avec un bouton **HS**, gris lorsque le matériel est en service (`false`) et rouge lorsqu’il est hors service (`true`). Sa couleur est le seul indicateur visuel de cet état ; les membres du CA voient le bouton sans pouvoir le modifier. Le matériel reste **Disponible** ou **Emprunté**, indépendamment de HS, et peut toujours être emprunté ou restitué. Une restitution conserve le statut HS. Le zonage et le groupe état/bouton HS restent chacun sur une seule ligne.

Chaque changement effectué avec le bouton **HS** ajoute au journal une entrée **Déclaré hors service** ou **Remis en service**, avec la date et le nom de l’administrateur. Un changement HS conserve l’emprunt en cours. Une requête répétant le même état ne crée pas de doublon.

Les tables `zonage`, `materiel` et `emprunt_materiel` sont créées au démarrage par la configuration Hibernate existante (`ddl-auto: update`). Les matériels existants sont en service par défaut. Les endpoints `/materiel` appliquent les mêmes restrictions de rôles côté serveur. Sur une base possédant déjà le journal, exécuter `app/sql/migration-journal-hs.sql` avant le déploiement pour autoriser les entrées HS sans emprunteur.

Le rôle **Logistique** se gère depuis l’administration ou la fiche adhérent. Les administrateurs et la Logistique disposent du bouton **Modifier** sur chaque matériel : changement du nom ou du zonage, et suppression après confirmation. La suppression est refusée tant que le matériel est emprunté et supprime également son journal d’emprunt. Les droits d’emprunt, de restitution et du bouton HS restent inchangés.

Avant de déployer ce rôle sur une base existante, exécuter `app/sql/migration-logistique.sql` pour mettre à jour la contrainte des rôles (après `migration-roles.sql` si la base utilise encore les anciennes tables).
