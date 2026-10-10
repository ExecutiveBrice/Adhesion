# Boutique, commandes, paiements et stock

Acteurs : client connecté, responsable boutique, prestataire de paiement, traitements périodiques.
Source métier des paiements : [SHOP_PAYMENTS.md](../../app/SHOP_PAYMENTS.md).

### SHOP-001 — Une commande conserve son prix et sa clé de création

Statut : Observée.
La commande conserve les prix au moment de sa création. Rejouer la même clé de checkout
retourne la commande existante sans nouvelle réservation de stock.
Exemple : changer le prix catalogue après achat ne modifie pas la commande précédente.

### SHOP-002 — Les réservations et annulations restent cohérentes

Statut : Documentée.
Annuler ou expirer une commande impayée annule les articles et libère leurs réservations.
L'annulation partielle avant paiement est refusée ; annuler l'unique article annule la commande entière.
Exemple : une commande de deux articles impayée ne peut pas être réduite à un article pendant son paiement.

### SHOP-003 — Le serveur confirme le paiement auprès du prestataire

Statut : Documentée.
Le retour navigateur et le corps d'une notification ne prouvent pas un paiement. Chaque chemin
relit la tentative connue via l'API authentifiée et vérifie identifiant, état, devise et montant.
Une contribution supplémentaire HelloAsso est compatible avec le montant de la commande.
Exemple : `?status=PAID` dans l'URL ne suffit pas à afficher une confirmation.

### SHOP-004 — Une confirmation répétée ne consomme pas deux fois le stock

Statut : Documentée.
Webhook, rattrapage et vérification manuelle sérialisent les confirmations par verrou en base.
Le paiement peut être confirmé sans retour du navigateur.
Exemple : deux confirmations concurrentes produisent une seule consommation de stock.
Limite : les tests de concurrence actuels utilisent H2 ; le comportement sur PostgreSQL reste à compléter.

### SHOP-005 — L'expiration tient compte des paiements en cours

Statut : Documentée.
L'échéance est de 24 heures après création. Après échéance, aucune nouvelle session de paiement
n'est ouverte. Une vérification finale en erreur reporte l'expiration et ne libère pas le stock.
Exemple : une indisponibilité du prestataire ne transforme pas automatiquement une tentative inconnue en échec.

### SHOP-006 — Le remboursement confirmé ne réintègre pas automatiquement le stock physique

Statut : Documentée.
Une ligne payée ne peut pas être annulée manuellement. Un remboursement intégral confirmé
met la commande à REFUNDED et ses articles à CANCELLED, sans présumer du retour des produits.
Exemple : après remboursement, le responsable vérifie le retour réel avant d'ajuster le stock.

## États

Le parcours nominal est PENDING_PAYMENT → PAID → PROCESSING → COMPLETED.
Les commandes impayées peuvent être CANCELLED ou EXPIRED. La confirmation du remboursement
conduit à REFUNDED. Les endpoints administratifs ne doivent pas permettre de forcer un statut de paiement.

## Questions ouvertes et validation externe

- Le socle navigateur couvre catalogue → panier → commande → attente de vérification → confirmation,
  avec une API simulée. Il ne valide ni un débit réel ni les droits serveur.
- Effectuer le parcours sandbox indiqué dans SHOP_PAYMENTS.md avant une livraison touchant HelloAsso.
- La concurrence des confirmations sur PostgreSQL et une chaîne navigateur + serveur complète restent à ajouter.
