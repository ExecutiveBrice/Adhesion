# Campagnes newsletter de la page mailing

Le contenu et les contacts sont préparés dans Brevo. Dans Mailing, l’onglet « Campagnes Brevo » affiche les campagnes classiques en brouillon destinées exclusivement à la liste **Newsletter ALOD**. L'identifiant vient de `BREVO_NEWSLETTER_LIST_ID` et le serveur vérifie le nom exact de la liste. Les campagnes avec d'autres listes, des segments ou des listes d'exclusion ne sont pas proposées. Brevo reste responsable des désinscriptions et de la diffusion. Le nombre de contacts affiché n'est pas un nombre de messages livrés.

La clé `BREVO_APIKEY_PRIVATE` reste côté serveur. Les rôles autorisés restent SECRETAIRE, BUREAU, MEMBRECA et ADMIN pour consulter, lancer et vérifier les campagnes. `Envoi_Mails=false` interdit tout lancement, mais permet de consulter l'historique et les statuts. Les mails manuels, les activités et le template de convocation n°6 conservent leur parcours. L'ancien endpoint d'envoi du template n°2 répond désormais 410.

L'objet, le pré-en-tête, l'aperçu HTML isolé et la liste destinataire précèdent une confirmation récapitulative. Le serveur relit la campagne avant le lancement et refuse une confirmation périmée si la campagne a été modifiée dans Brevo. Les brouillons sont paginés avant filtrage.

Chaque campagne reçoit au maximum une requête `sendNow` depuis l'application : un historique unique par identifiant de campagne est enregistré et validé en base avant l'appel externe, y compris en présence de plusieurs instances ou après un redémarrage. Les mises à jour de schéma Hibernate sont activées dans la configuration existante. Si elles sont désactivées au déploiement, appliquer `app/sql/migration-newsletter-campaigns.sql` avant de démarrer cette version.

| État local | Signification |
| --- | --- |
| REQUESTED | Demande enregistrée avant l'appel Brevo ; résultat pas encore connu |
| ACCEPTED | Brevo a répondu 204 ou rapporte queued/inProcess/in_process/in_review ; diffusion pas terminée |
| COMPLETED | Brevo rapporte sent ; cela ne garantit pas la livraison de chaque email |
| UNKNOWN | Réponse ambiguë ou panne réseau ; aucun renvoi automatique |
| REJECTED | Refus explicite Brevo (400/402/404) ; aucun envoi réussi comptabilisé |

Après une réponse ambiguë, le serveur consulte immédiatement le statut. Si cette consultation échoue, l'historique reste UNKNOWN. Le navigateur conserve aussi l'identifiant en attente à travers un rechargement. « Vérifier le statut » ne lance jamais d'envoi. Un statut draft après une panne ne prouve pas un refus : le même identifiant reste bloqué. Même après un refus explicite, cette première version exige de préparer une nouvelle campagne dans Brevo pour une nouvelle demande. Aucun mécanisme de réinitialisation du verrou n'est exposé.

Les compteurs de réussite/échec des mails transactionnels ne sont pas utilisés pour les campagnes. Leur identifiant et leurs états local/Brevo figurent dans l'historique. Le suivi est actualisé à la demande ; il n'y a pas de polling permanent.

Tests sans envoi réel :

```sh
cd app
mvn -o -q -Dtest=NewsletterCampaignServiceTest,NewsletterCampaignControllerTest,NewsletterHistoryRepositoryTest,NewsletterContactServiceTest test
cd ../client
npm test -- --watch=false --browsers=ChromeHeadless --include=src/app/page/mailling/mailling.component.spec.ts
npm run build
```

Le service est testé avec des réponses HTTP et pannes simulées sous le SDK Brevo réel. Les autres tests vérifient les permissions, la contrainte d'unicité en base, les parcours existants et la confirmation côté interface.

Les appels newsletter portent `ngsw-bypass=true` pour éviter leur interception par le service worker Angular. Leur attente est limitée à 30 secondes. Un dépassement pendant un lancement déclenche la vérification de statut, sans renvoi automatique. Le chargement des brouillons se termine aussi en cas de délai dépassé ou d’erreur réseau.

Les erreurs de consultation sont renvoyées directement au format ProblemDetail, avec le code HTTP du backend et une description de l’opération Brevo. Un refus de la clé Brevo (401/403) produit un 502 applicatif et ne doit pas déclencher une expiration de session. Les logs donnent l’étape et le code HTTP sans clé API ni réponse brute. Pour un brouillon exclu, ils indiquent son identifiant et la raison du filtrage.

La page Mailing comporte deux onglets : « E-mails individuels » conserve la sélection des destinataires, le contenu manuel ou la convocation et l’historique, avec une disposition empilée sur mobile. « Campagnes Brevo » affiche le bouton d’actualisation et des cartes dépliables pour les brouillons, avec aperçu, confirmation et résultat dans chaque carte, sans tableau d’historique. Une demande incertaine conserve une carte de suivi permettant de vérifier son statut. Le texte et les destinataires d’un mail sont conservés lors du changement d’onglet.
