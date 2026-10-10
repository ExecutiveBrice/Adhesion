# Activités, séances et agenda

Acteurs : visiteur du calendrier, adhérent, encadrant, référent, secrétaire, administrateur.

### ACTIVITY-001 — La gestion d'un référent reste dans ses sections

Statut : Documentée.
Les listes et modifications d'activités sont limitées aux sections affectées. Une modification
ne déplace pas une activité depuis ou vers une autre section. Les opérations de séance vérifient
son appartenance à l'activité demandée.
Exemple : connaître un identifiant de séance d'une autre section ne donne pas le droit de la modifier.
Source : [gestion des sections](../../app/SECTION_MANAGEMENT.md).

### ACTIVITY-002 — Modifier une activité conserve ses séances

Statut : Observée.
Une modification des informations d'activité ne remplace pas ses séances existantes.
Exemple : changer le nom d'une activité conserve les dates et les identifiants de ses séances.

### ACTIVITY-003 — La génération respecte le calendrier et évite les doublons

Statut : Observée.
La génération hebdomadaire part du début d'activité et ignore les vacances et jours fériés
selon la configuration. L'ajout de séances ignore les dates déjà présentes.
Exemple : relancer l'ajout ne duplique pas une séance existante.

### ACTIVITY-004 — Une présence prévue ne modifie pas la présence constatée

Statut : Observée.
L'adhérent ne modifie que sa présence prévue, pour une séance et une inscription éligibles.
Les séances annulées, terminées ou passées refusent cette modification.
Exemple : répondre « absent prévu » ne supprime pas un constat de présence saisi par un encadrant.

### ACTIVITY-005 — Le calendrier public omet les commentaires internes

Statut : Observée.
La réponse destinée au calendrier public n'expose pas les commentaires internes des séances.
Exemple : une remarque interne d'encadrant n'apparaît pas sur la page de connexion publique.

## Questions ouvertes

- Compléter les critères métier d'éligibilité par type de section et les règles de changement d'heure.
- Les appels réels aux agendas et jeux de données externes restent hors des tests navigateur simulés.
