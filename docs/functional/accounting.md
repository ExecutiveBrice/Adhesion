# Comptabilité

Acteurs : rôles autorisés à la comptabilité. Ce domaine concerne les paiements des adhésions ;
la boutique a son propre modèle monétaire et ses propres commandes.

### ACCOUNT-001 — Les deux bornes d'une période sont incluses

Statut : Observée.
Les paiements du premier et du dernier jour sont inclus, y compris pour une période d'un seul jour.
Une période manquante ou inversée est refusée avant lecture des données.
Exemple : pour le 1er au 31 octobre, le 30 septembre et le 1er novembre sont exclus.

### ACCOUNT-002 — Le récapitulatif conserve ses critères de calcul

Statut : Observée.
Les types de règlement sont séparés et les activités de même nom regroupées. Les adhésions
non validées et paiements incomplets ne contribuent pas au récapitulatif.
Les détails conservent les paiements non contributeurs et identifient ceux utilisés dans la synthèse.
Exemple : un échéancier reste visible dans les détails même si un seul versement appartient à la période.

### ACCOUNT-003 — Un paiement modifié appartient à l'adhésion demandée

Statut : Observée.
Les écritures incomplètes et modifications ou suppressions d'un paiement d'une autre adhésion
sont refusées sans écriture. La création conserve la date et le montant demandés.
Exemple : changer l'identifiant d'adhésion dans une requête ne déplace pas un versement d'une autre personne.

### ACCOUNT-004 — Le rapprochement ne modifie pas les paiements ni les totaux

Statut : Observée.
Les choix de rapprochement sont persistés séparément des montants et dates des paiements.
Exemple : marquer une ligne rapprochée ne change pas le total de la période.

## Questions ouvertes

- Confirmer les règles observées de regroupement et les unités monétaires avant toute évolution de calcul.
- Le contrôle navigateur d'une saisie comptable complète reste à ajouter.
