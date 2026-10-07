# Confirmation des paiements boutique

Avec le fournisseur `helloasso` (profils `helloasso-sandbox` ou
`helloasso-production`, ou `SHOP_PAYMENT_PROVIDER=helloasso`), la confirmation ne
dépend plus du retour de l’adhérent sur le site :

- **Notifications** : `POST /shop/payments/helloasso/notifications`.
- **Rattrapage** : vérification périodique des tentatives HelloAsso encore en
  attente, pour les commandes en attente de paiement.
- **Responsable boutique** : bouton **Vérifier le paiement** dans le détail d’une
  commande en attente, via `POST /shop/admin/orders/{orderNumber}/payment/verify`.

Chaque chemin relit le paiement via l’API HelloAsso authentifiée et vérifie son
identifiant, sa devise, son état et son montant. Le total HelloAsso doit couvrir
au moins celui de la commande ; il peut inclure une contribution supplémentaire.
Le contenu d’une notification, les
métadonnées et les paramètres de retour du navigateur ne constituent jamais une
preuve de paiement. Un verrou en base sur le paiement sérialise les confirmations
concurrentes ; une répétition ne déduit pas à nouveau le stock.

## Activation des notifications

Après déploiement, renseigner dans HelloAsso **Mon compte → Intégrations et API**
l’URL HTTPS publique du **serveur API**, en tenant compte de son éventuel préfixe
de reverse proxy :

```text
https://<hôte-public-api>/<préfixe-éventuel>/shop/payments/helloasso/notifications
```

Ne pas utiliser l’URL de la page Angular de retour de paiement. Tester séparément
les environnements sandbox et production. Cette modification du code ne configure
pas le compte HelloAsso à distance.

Les événements `Order` et `Payment` sont acceptés. Le checkout est retrouvé grâce
à `checkoutIntentId`, ou aux métadonnées `shopOrderId` + `idempotencyKey` envoyées
lors de sa création si un événement `Payment` omet cet identifiant. Les événements
d’autres campagnes ou d’autres types sont ignorés avec une réponse **200**, sans
appel au prestataire ni divulgation de données. Une erreur de vérification renvoie
une erreur HTTP afin de permettre une nouvelle livraison. Une notification qui
arrive avant l’enregistrement du checkout peut être rejouée ; le rattrapage reste
disponible si elle n’a pas pu être corrélée immédiatement.

Si le compte partenaire fournit une clé de signature, définir
`HELLOASSO_WEBHOOK_SIGNATURE_KEY` : l’en-tête `x-ha-signature` devient obligatoire
et son HMAC SHA-256 est vérifié sur les octets exacts du corps reçu. Les comptes
association sans clé utilisent la relecture systématique du checkout connu via
l’API authentifiée. Le reverse proxy peut également filtrer les IP HelloAsso et
limiter le débit sur cette seule route publique. Aucune session adhérent n’est
requise pour le webhook ; toutes les routes de gestion conservent leurs droits.

Références : [configuration des notifications](https://dev.helloasso.com/docs/notifications-webhook),
[validation des paiements](https://dev.helloasso.com/docs/validation-de-vos-paiements),
[signature des notifications](https://dev.helloasso.com/docs/secure-webhook).

## Rattrapage périodique

| Variable | Valeur par défaut | Effet |
| --- | --- | --- |
| `SHOP_PAYMENT_RECONCILIATION_ENABLED` | `true` | Active le rattrapage avec le fournisseur HelloAsso. |
| `SHOP_PAYMENT_RECONCILIATION_DELAY_MS` | `120000` | Délai entre la fin d’un passage et le suivant. |
| `SHOP_PAYMENT_RECONCILIATION_INITIAL_DELAY_MS` | `30000` | Délai avant le premier passage après démarrage. |
| `SHOP_PAYMENT_RECONCILIATION_BATCH_SIZE` | `50` | Maximum de tentatives par passage, entre 1 et 500. |

Le parcours avance par identifiant et revient au début après le dernier lot : les
anciennes commandes impayées ne monopolisent pas les premiers résultats. Chaque
tentative est traitée dans une transaction distincte. Une erreur n’annule pas les
autres confirmations et la tentative est revue au prochain tour. Les identifiants
en échec sont journalisés sans données personnelles ni secrets.

Le délai de confirmation de secours dépend donc du volume, du temps de réponse
HelloAsso et du nombre de lots. Les commandes sans session de paiement ne sont pas
interrogées.

## Expiration des commandes impayées

Une commande doit être payée dans les **24 heures suivant sa création**. Le serveur
calcule et renvoie `paymentExpiresAt` à partir de `createdAt`. Un compte à rebours
est affiché sur les commandes des adhérents et dans leur détail, ainsi que dans
le détail côté responsable. Après l'échéance, il n'est plus possible d'ouvrir une
nouvelle session de paiement.

Un traitement parcourt les commandes en attente toutes les minutes par lots de 50.
Il vérifie les tentatives en attente auprès du prestataire avant de passer la
commande à `EXPIRED` et de libérer les réservations de stock. Une erreur de
vérification reporte l'expiration sans libérer le stock. Selon la
[documentation HelloAsso](https://dev.helloasso.com/docs/validation-de-vos-paiements),
un checkout sans paiement peut être considéré abandonné après **45 minutes** :
si une tentative a été lancée juste avant la limite de 24 heures, sa vérification
peut donc reporter la libération du stock pendant cette période. L'interface
indique alors qu'une vérification finale est en cours. Le verrou du paiement reste
détenu jusqu'à la libération du stock pour éviter la concurrence avec le webhook
et le rattrapage.

`SHOP_ORDER_EXPIRY_SCAN_DELAY_MS` vaut `60000` par défaut et
`SHOP_ORDER_EXPIRY_INITIAL_DELAY_MS` vaut `30000`. Les commandes historiques encore
en attente sont soumises à la même règle au premier passage après déploiement.
Une confirmation anormale après clôture est journalisée pour traitement manuel.

## Cohérence des statuts de commande et d'article

La confirmation du paiement laisse les articles « À traiter ». Quand le responsable
passe la commande « En préparation », les articles encore à traiter suivent ce
statut ; les articles déjà terminés le restent. Passer la commande « Terminée »
termine tous ses articles. À l'inverse, les lignes modifiées individuellement
restent indépendantes : la commande passe « En préparation » lorsque toutes ont
quitté « À traiter », puis « Terminée » lorsque toutes sont terminées.

L'annulation ou l'expiration d'une commande impayée annule ses articles et libère
les réservations de stock. L'annulation d'un article isolé n'est possible avant
paiement que si c'est le seul article de la commande : elle annule alors la
commande entière. Une annulation partielle changerait le montant du paiement en
cours et n'est pas disponible. Après paiement, le statut « Annulée » ne peut pas
être choisi manuellement pour une ligne : un remboursement doit d'abord être
effectué et confirmé par le fournisseur. Lorsque le remboursement intégral est
confirmé, la commande et ses articles passent respectivement à « Remboursée » et
« Annulée ». Le stock physique n'est pas recrédité automatiquement après
remboursement, car le retour effectif des articles n'est pas connu ; le responsable
doit le vérifier et ajuster le stock si nécessaire.

## Vérification après déploiement

1. Dans le sandbox, créer une commande et effectuer le paiement, puis fermer le
   navigateur avant le retour sur le site.
2. Vérifier que la commande passe à « Payée » après notification ou rattrapage.
3. Rejouer la notification : le stock doit rester identique.
4. Sur une autre commande en attente, utiliser **Vérifier le paiement** avec un
   compte responsable boutique. L’action affiche le résultat confirmé, l’attente
   de confirmation ou une erreur explicite ; elle ne permet pas de forcer « Payée ».

Les tests automatisés couvrent notamment les droits, la signature, la corrélation
des événements, la vérification sans navigateur, les erreurs et la concurrence
entre webhook, responsable et rattrapage. Les tests de concurrence utilisent H2 ;
le parcours réel HelloAsso et le déploiement restent à valider dans le sandbox.
