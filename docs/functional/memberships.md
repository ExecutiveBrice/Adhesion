# Adhérents et adhésions

Acteurs : adhérent et membres de sa tribu, secrétaire, administrateur, référent en consultation.
Une adhésion rattache un adhérent à une activité et porte ses accords, statut et validations.

### MEMBER-001 — La modification de l'e-mail est réservée

Statut : Observée.
La modification d'une adresse existante est autorisée au secrétaire ou à l'administrateur.
Les autres rôles peuvent modifier les champs permis sans changer l'e-mail ; un utilisateur
omis dans une mise à jour ne doit pas effacer l'adresse existante.
Exemple : une mise à jour du nom par un compte ordinaire conserve son e-mail.

### MEMBER-002 — La création d'un adhérent exige une tribu

Statut : Observée.
Créer un adhérent sans tribu est refusé. Des comptes de connexion distincts sont créés pour
les membres sans e-mail ; l'adresse fournie lors d'une création est conservée.
Exemple : deux membres sans adresse ne partagent pas le même identifiant de connexion.

### MEMBER-003 — La création d'une adhésion dépend des places disponibles

Statut : Observée.
Le service compte les adhésions en cours ou validées. Lorsque la capacité est atteinte,
la nouvelle adhésion passe sur liste d'attente avec une position ; sinon elle attend la validation adhérent.
Le tarif vient de l'activité, avec une majoration de 30 si les conditions de réinscription et majoration sont réunies.
Source technique : `AdhesionServices.save`.
Exemple : une activité sans place disponible produit une adhésion en attente, pas une adhésion validée.
Lacune : absence de test dédié à ce calcul et à deux inscriptions simultanées.

### MEMBER-004 — Les présences suivent les adhésions éligibles

Statut : Observée.
Une adhésion en liste d'attente ou annulée ne crée pas de présence. L'ajout d'une séance crée
les présences des adhésions éligibles sans doublon ; la validation d'une adhésion complète ses séances planifiées.
Exemple : relancer la génération ne crée pas deux présences pour le même participant et la même séance.

## États et questions ouvertes

Les libellés de `Status` et les statuts historiques de la base sont exposés à l'interface.
Ne pas renommer ces valeurs sans migration et vérification des filtres, du chat, des présences et des exports.

- La matrice complète des transitions et effets de `choisirStatut` reste à valider avec le métier.
- MEMBER-003 est une observation, pas une validation du montant de majoration ou du traitement de concurrence.
- Les parcours d'adhésion depuis le profil restent à automatiser avec un serveur et des données jetables.
