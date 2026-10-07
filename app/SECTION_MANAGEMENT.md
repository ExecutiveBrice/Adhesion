# Espace « Mes sections »

Les administrateurs attribuent les référents dans **Administration → Sections**. Un compte voit ensuite **Mes sections** dans la navigation s'il est référent d'au moins une section. L'affectation à une section, et non le rôle global `ROLE_REFERENT_SECTION`, détermine les droits sur chaque requête.

L'interface `/mes-sections` propose trois vues par section :

- **Chats** : création, modification des permissions de lecture et d'écriture, rattachement à la section ou à l'une de ses activités, suppression avec confirmation. Les permissions par rôle du système de Chat restent additives et globales pour ce rôle ; l'interface l'indique avant enregistrement.
- **Activités** : liste avec occupation, création d'une activité avec un créneau hebdomadaire, modification du nom, du tarif, de la capacité, du lien et des indicateurs « complète » et « réinscription ». La création passe par le service d'activité existant pour générer les séances. La gestion détaillée des créneaux, salles et intervenants reste dans l'interface administrative.
- **Adhésions** : liste paginée, recherche par nom ou activité, filtre de statut et changement de statut. Le changement utilise le traitement métier existant, avec ses notifications, attestations et effets associés. Le suivi de paiement et de documents est affiché, sans pouvoir valider ces deux volets depuis cet écran.

Les endpoints `/section-management/sections/{sectionId}/...` revérifient l'affectation du compte à la section. Une activité, une adhésion ou un Chat doit appartenir à cette section avant toute modification ; un Chat ne peut pas être déplacé hors de ce périmètre. Les anciens endpoints globaux de modification de l'activité et du statut d'adhésion sont réservés au secrétariat et à l'administration.
