> Document historique, remplacé par [la gestion généralisée des équipes et événements](TEAM_MANAGEMENT.md). Les options et créneaux décrits ci-dessous ne font plus partie du fonctionnement actuel.

# Proposition de gestion des équipes sportives

Date : 3 novembre 2026. Statut : proposition fonctionnelle et technique, sans implémentation.

> Révision : la [proposition V1 par activité](TEAM_MANAGEMENT_V1.md) remplace les choix de rattachement et de périmètre de ce document. Le module est désormais optionnel par activité via `team_management`, indépendamment de la section ; les équipes remplacent les catégories dans l'interface de l'activité. Le présent document conserve l'analyse comparative initiale.

## 1. Orientation

Ajouter une gestion des équipes au sein des sections, en conservant les activités comme support des inscriptions, tarifs et dossiers administratifs. Une équipe représente un collectif sportif pendant une saison ; son effectif et les personnes convoquées à une rencontre sont deux ensembles distincts.

L'objectif prioritaire est de permettre au référent de section de répartir ses adhérents, à l'entraîneur de préparer sa semaine et aux familles de répondre facilement aux convocations. Les statistiques sportives détaillées viennent après ces usages.

## 2. Enseignements du marché

Analyse des fonctionnalités publiées par les éditeurs, consultées le 3 novembre 2026. Il ne s'agit pas d'un test de leurs applications authentifiées ; les différences ci-dessous décrivent les accents de leurs offres, sans conclure à l'absence d'une fonction chez l'autre éditeur.

| Domaine | Kalisport | SportEasy | Choix proposé pour Adhésions |
| --- | --- | --- | --- |
| Organisation | Gestion sportive intégrée à la gestion associative | Espaces de groupes et équipes avec gestion déléguée | Une base adhérents commune et des responsabilités limitées à chaque section/équipe |
| Calendrier | Événements, rendez-vous, reports et annulations | Planning partagé du club et des équipes | Enrichir les séances et l'agenda existants |
| Mobilisation | Convocations, disponibilités et suivi des présences | Convocations, relances et réponses des parents | Distinguer disponibilité, sélection, réponse et présence réelle |
| Organisation des rencontres | Postes paramétrables : arbitres, table de marque, buvette ; remplacements | Gestion des tâches au sein des équipes | Étendre les tâches de séance existantes avec besoins et confirmations |
| Suivi sportif | Assiduité et performances | Assiduité, compositions et statistiques | Assiduité et résultats simples avant les statistiques propres à chaque sport |

Sources : [gestion sportive Kalisport](https://www.kalisport.com/fonctionnalites/gestion-sportive), [aide événements et imports Kalisport](https://www.kalisport.com/ressources/aide/127-/), [groupes et catégories SportEasy](https://www.sporteasy.net/fr/clubs/features/group-management/), [convocations et présences SportEasy](https://www.sporteasy.net/fr/teams/features/invitations-and-attendance/), [fonctionnalités club SportEasy](https://www.sporteasy.net/fr/clubs/features/).

La proposition suivante est un choix de conception pour Adhésions. Elle reprend ces usages et les adapte au modèle déjà présent.

## 3. Ce que possède déjà Adhésions

| Élément vérifié dans le dépôt | Conséquence |
| --- | --- |
| `Section` contient un type, une couleur et des référents | Conserver la section comme périmètre de pilotage et de délégation |
| `Activite` porte tarif, capacité, âge, genre, inscriptions, encadrants et référents | Conserver cette fonction administrative ; plusieurs équipes peuvent utiliser une même activité |
| `Adhesion` relie une personne à une activité et possède un surclassement | Réutiliser le dossier existant pour justifier la participation sportive |
| `PlanificationHebdomadaire` définit des créneaux, salles et encadrants | Conserver ces créneaux pour les entraînements communs ou propres à une équipe |
| `Seance` possède dates, salle, état, commentaire et responsabilités | Étendre ce calendrier plutôt que créer un planning parallèle |
| `Presence` distingue `presencePrevue` et `presence` | Préserver la distinction entre intention et constat ; la sélection n'est pas représentée aujourd'hui |
| Les présences prévues sont actuellement réservées aux sections `COMPETITION` | Prévoir une activation explicite par section si des groupes de loisir utilisent le module |
| La page `/profs`, intitulée « Mes équipes », liste les activités des encadrants | Faire évoluer cette navigation lors de l'introduction des vraies équipes |
| `TacheSeance` et `ResponsabiliteSeance` associent déjà des tâches aux adhérents de la section | Réutiliser ce catalogue et ajouter les besoins, réponses et remplacements |
| Les chats ciblent une association, une section ou une activité | Ajouter une cible équipe avec ses propres règles d'accès |
| Les tribus, représentants et coordonnées du représentant existent | Construire un accès familial explicite aux événements et réponses |

Points à vérifier avant développement : organisation réelle du changement d'exercice, conservation des données entre saisons et correspondance entre les activités existantes et les catégories sportives. Les entités `Activite` et `Adhesion` examinées ne portent pas de relation explicite vers une entité saison. La présence d'un champ `saison` dans `AdhesionLite` ne suffit pas à définir ce cycle.

## 4. Modèle fonctionnel recommandé

```text
Association
  └─ Section : Basket
      ├─ Activité : Basket U15 — inscription, tarif, dossier
      │   └─ Créneaux et séances d'entraînement
      ├─ Équipe : U15 A — saison 2026–2027
      └─ Équipe : U15 B — saison 2026–2027

Adhérent → Adhésion à l'activité → Affectation à une ou plusieurs équipes
Séance → Équipe(s) concernée(s) → Convocations individuelles
```

L'activité et l'équipe restent liées, mais ne sont pas interchangeables. Une section peut aussi fonctionner sans équipe, notamment pour ses cours de loisir. Le terme « groupe » peut remplacer « équipe » dans l'interface selon la pratique.

### Identité d'une équipe

- Section, saison, nom, catégorie sportive, pratique compétition/loisir, niveau et couleur optionnelle héritant de la section.
- Une activité source pour le premier périmètre : elle fournit le vivier habituel de pratiquants. Plusieurs équipes peuvent partager cette activité. Les activités et équipes liées appartiennent à la même section.
- Effectif cible, état brouillon/active/archivée et staff.
- Catégorie sportive distincte de la catégorie de séance : U15 décrit des pratiquants ; une planification hebdomadaire décrit un créneau. La catégorie peut d'abord être un champ de l'équipe, sans nouvel écran de référentiel.

### Effectifs

- Affectation explicite depuis les adhérents inscrits, avec recherche et sélection groupée.
- Personne identifiée par son `Adherent`, avec lien vers l'adhésion source pour les pratiquants.
- Dates de début et de fin d'affectation ; une modification d'effectif ne réécrit pas les anciennes rencontres.
- Plusieurs équipes possibles, avec une équipe principale facultative par section et saison.
- Visibilité des membres sans équipe depuis le tableau de section.
- Exclusion des adhésions annulées et sur liste d'attente ; dossier en cours signalé séparément.
- Vivier élargi aux surclassements déjà autorisés. Un surclassement administratif n'affecte pas automatiquement une personne à une équipe et ne vaut pas autorisation fédérale pour toutes les compétitions.
- Une convocation ponctuelle comme renfort conserve l'équipe habituelle et l'adhésion source ; elle ne crée ni nouvelle adhésion ni paiement.

### Staff

Fonctions locales : entraîneur, entraîneur adjoint, responsable d'équipe et accompagnateur. Une même personne peut exercer plusieurs fonctions et intervenir sur plusieurs équipes. L'appartenance au staff n'exige pas une adhésion payante à l'activité.

La fonction et la permission sont distinctes : un accompagnateur ne reçoit pas automatiquement le droit de gérer l'effectif. Les affectations des encadrants d'activité existants servent de propositions à confirmer lors de la création des équipes.

## 5. Parcours principaux

### Référent de section

Une entrée « Équipes » propose un filtre section/saison et, pour chaque équipe : effectif, staff, prochaine rencontre et alertes. Une vue « Répartir les adhérents » montre les non-affectés et permet les affectations groupées.

Le référent crée les équipes, attribue les responsables, organise les passages entre équipes et consulte le planning de section. Les noms identiques entre deux saisons restent possibles ; une archive est consultable en lecture seule par les personnes autorisées.

### Entraîneur

La fiche d'équipe comporte cinq onglets : **Vue d'ensemble**, **Effectif**, **Calendrier**, **Messages**, **Bilan**. Les actions principales sont « Préparer la prochaine rencontre », « Convoquer », « Relancer les non-répondants » et « Faire l'appel ».

Le tableau de bord privilégie les situations utiles : réponses manquantes, effectif insuffisant, créneau en conflit et tâche non pourvue. Un entraînement commun apparaît une seule fois dans l'agenda, même s'il concerne deux équipes du même adhérent.

### Adhérent et représentant

La page « Mes équipes » et l'agenda montrent les équipes et événements accessibles. La convocation affiche l'enfant concerné, l'équipe, les heures de rendez-vous et de début, l'adresse et les actions « Disponible » / « Indisponible ».

Un espace familial permet de répondre pour chaque enfant autorisé sans changer de compte. Une appartenance à la même tribu ne doit pas, à elle seule, donner pouvoir de répondre pour tous les adultes de cette tribu : utiliser la relation de représentation ou une délégation explicite. Conserver l'identité du compte ayant répondu.

Les messages destinés à un mineur suivent les coordonnées du représentant configurées dans la fiche. Dédupliquer l'envoi à une même adresse, tout en conservant des réponses distinctes pour chaque enfant et chaque rencontre.

## 6. Séances, convocations et présences

Étendre `Seance` avec un type : entraînement, rencontre, tournoi, autre. Une séance peut concerner plusieurs équipes ; son activité reste le rattachement existant. Les entraînements d'une activité sans équipe continuent à fonctionner.

Les rencontres ajoutent adversaire, domicile/extérieur/terrain neutre, lieu extérieur, heure et lieu de rendez-vous distincts du début, compétition facultative et résultat simple. Un détail spécifique de rencontre évite d'alourdir tous les cours avec ces champs.

| Notion | États proposés | Qui la renseigne ? |
| --- | --- | --- |
| Disponibilité | Inconnue, disponible, indisponible | Pratiquant ou représentant autorisé |
| Sélection | Non sélectionné, retenu, réserve | Entraîneur |
| Convocation | Brouillon, publiée, retirée | Responsable habilité |
| Réponse | Sans réponse, acceptée, refusée | Destinataire ou représentant autorisé |
| Présence réelle | Non pointée, présent, absent ; retard/excuse ultérieurement | Encadrant habilité |

La disponibilité préalable reste facultative. Pour le premier lot, elle peut se limiter aux indisponibilités déclarées pour un événement ; les plages d'indisponibilité viennent ensuite. Aucune réponse ne signifie jamais une acceptation.

Une sélection en brouillon n'envoie aucun message. La publication fige la liste initiale des destinataires et déclenche les notifications. Les ajouts/retraits suivants sont tracés et notifiés aux personnes concernées. Un entraînement peut convoquer tout son effectif ; une rencontre part d'une sélection explicite, avec seuils d'effectif configurables.

Les convocations publiées restent conservées lors d'un transfert d'équipe. Une annulation d'adhésion retire les droits futurs et signale les convocations à revoir, sans effacer l'historique. Une annulation de rencontre arrête les relances ; un report conserve les réponses dans l'historique et demande une nouvelle confirmation pour le nouvel horaire.

Après l'événement, le pointage réel alimente l'assiduité. Ne pas convertir automatiquement une acceptation en présence. Le bilan distingue entraînements et rencontres et exclut les événements annulés, non pointés et les périodes hors affectation.

### Réutilisation des présences existantes

La création actuelle des présences couvre tous les inscrits à l'activité, sauf annulation/liste d'attente. Elle doit évoluer pour respecter le périmètre d'une séance ciblant une équipe ; une séance commune déduplique les personnes.

Conserver `Presence` comme source du pointage des pratiquants et `Convocation` comme source de sélection, publication et réponse. Pour les nouveaux événements avec convocation, la réponse devient la référence de la présence annoncée : ne pas maintenir deux valeurs modifiables indépendamment avec `presencePrevue`. Les anciennes séances conservent leur comportement pendant la migration.

Un renfort conserve le lien vers sa propre adhésion, même si elle diffère de l'activité d'accueil. Les services doivent contrôler section, saison et autorisation du renfort plutôt que supposer que toute présence provient de l'activité de la séance.

## 7. Organisation des rencontres

Étendre les responsabilités existantes avec : nombre de postes requis, personne affectée, réponse, réalisation et historique des remplacements. Les bénévoles ou représentants sans inscription sportive peuvent être proposés par leur fiche adhérent, avec accord explicite ; ils n'ont pas besoin de devenir joueurs.

Exemple pour un match de basket : deux arbitres, deux personnes à la table et un responsable de salle. Les fonctions restent configurables pour les autres sports. La vue de section affiche les rencontres et les postes restant à pourvoir.

Un remplaçant proposé doit accepter avant que le poste soit considéré comme couvert. La confirmation à une tâche reste distincte de la participation sportive. Le covoiturage détaillé et la répartition automatique équitable des tâches peuvent attendre un second lot.

## 8. Accès et communication

| Profil | Périmètre proposé |
| --- | --- |
| Administrateur | Paramétrage et gestion des équipes selon les droits explicitement définis pour ce module |
| Référent de section | Création, effectifs et supervision des équipes de ses sections |
| Responsable d'équipe habilité | Gestion sportive de ses équipes, sans modification des paiements ou dossiers administratifs |
| Encadrant affecté à un créneau | Séances de ce créneau ; cette affectation ne donne pas automatiquement accès à toute l'équipe |
| Joueur | Ses équipes, ses événements et ses réponses |
| Représentant autorisé | Équipes et convocations des personnes représentées |
| Bénévole convoqué | Informations nécessaires à sa mission, sans accès à tout l'effectif |
| Secrétariat | Dossiers administratifs ; extension éventuelle à une assistance sportive explicitement accordée |

Tous les contrôles sont effectués côté serveur à chaque requête, y compris exports et pièces jointes éventuelles. Les coachs voient un indicateur de dossier à régulariser, sans détail financier ou document médical. Cet indicateur administratif ne certifie pas l'éligibilité fédérale.

Ajouter la cible `EQUIPE` au chat et adapter ses contraintes de rattachement. La lecture vient des affectations autorisées ; l'écriture dispose d'une règle explicite, car les liens actuels aux chats n'accordent que la lecture. La révocation d'une affectation retire l'accès aux échanges privés sauf autre droit explicite.

Les permissions globales par rôle existantes sont additives : recopier une permission `ROLE_USER` dans un chat d'équipe l'ouvrirait à tous ces comptes. Le nouveau chat doit proposer un réglage propre à son audience, sans ajouter automatiquement une permission globale ni changer les droits des anciens chats.

Les informations pratiques et convocations restent structurées dans les événements. Les chats servent aux échanges. Les notifications sportives utilisent les mécanismes transactionnels existants avec préférences adaptées ; elles n'inscrivent personne à la newsletter Brevo. Relances uniquement pour les convocations publiées sans réponse, arrêtées après échéance, annulation ou retrait. Notifications dans l'application et email en premier ; push selon les capacités effectivement disponibles.

## 9. Évolutions techniques proposées

| Objet | Rôle |
| --- | --- |
| `SaisonSportive` | Début, fin, libellé, état ; articuler ce cycle avec le changement d'exercice existant avant migration |
| `Equipe` | Section, saison, activité source, identité et état |
| `AffectationEquipe` | Adhérent, équipe, adhésion source pour un pratiquant, dates, fonction et permissions locales |
| Lien séance/équipes | Ciblage d'une ou plusieurs équipes et entraînements communs |
| Détail de rencontre | Adversaire, localisation, rendez-vous et résultat |
| `Convocation` | Séance, destinataire, rôles, sélection, publication, réponse et auteur de réponse |
| Évolution des responsabilités | Besoins, affectations, confirmations, remplacements et réalisation |
| Évolution du chat | Cible équipe, audience et écriture explicites |

Une personne peut avoir plusieurs fonctions mais ne reçoit qu'une convocation sportive par séance. Les tâches gardent leurs confirmations propres. Les règles d'unicité, de chevauchement d'affectations et de cohérence de section/saison sont validées en base et dans les services.

Les endpoints peuvent suivre les périmètres existants : `/section-management/sections/{id}/equipes` pour la gestion de section, puis ressources d'équipe et de convocation contrôlées par affectation. Les DTO exposent les informations nécessaires à chaque profil ; ne pas sérialiser directement un graphe complet adhérent/adhésion.

Le changement de saison archive les équipes et affectations, puis propose une copie de structure. Il demande une nouvelle sélection des inscrits et du staff : aucune reconduction automatique des convocations, paiements, droits ou règles d'âge. L'historique nécessite de préserver les liens utiles si le mécanisme existant remplace ou supprime les activités d'un exercice.

## 10. Déploiement progressif

### Lot 1 — Usage quotidien complet

Équipes saisonnières, répartition des effectifs, staff et permissions, séances ciblées, rencontres simples, convocations manuelles, réponses personnelles/familiales, relance manuelle, appel réel, agenda commun et messages d'équipe. Réutiliser les responsabilités simples déjà disponibles. Livrer l'ensemble sur une section pilote, par exemple le basket, avant généralisation.

Critère de réussite : préparer une rencontre, obtenir les réponses des familles et pointer les présents sans recréer de fiche ou d'adhésion.

### Lot 2 — Coordination de section

Relances automatiques avec suivi des envois, plages d'indisponibilité, besoins et confirmations des tâches, remplacement accepté, alertes de conflits de personnes/salles, covoiturage simple, bilan d'assiduité et passage à la saison suivante.

### Lot 3 — Intégrations et suivi sportif

Imports fédéraux, export calendrier, résultats consolidés, compositions et statistiques adaptées aux sports réellement pratiqués. Les documentations Kalisport mentionnent plusieurs imports fédéraux ; cela ne garantit ni une API ouverte ni un accès équivalent pour Adhésions. Vérifier chaque fédération, les droits d'accès et la gestion des doublons avant engagement.

## 11. Scénarios de validation pour l'implémentation

1. Deux équipes issues d'une activité partagent un entraînement : un événement et un pointage par personne, sans doublon dans l'agenda.
2. Une rencontre de U15 A convoque un joueur de U15 B : aucune mutation de son équipe principale, aucune seconde adhésion.
3. Un renfort provenant d'une autre activité autorisée de la section utilise son adhésion source ; un renfort non autorisé est refusé.
4. Un parent répond pour deux enfants : réponses distinctes et auteur identifié ; un autre adulte de la tribu sans délégation ne peut pas répondre.
5. Un entraîneur perd son affectation : il ne peut plus lire ou modifier l'équipe par appel direct à l'API.
6. Une personne accepte une convocation et ne vient pas : la réponse reste acceptée, le pointage indique absent.
7. Une rencontre est reportée ou annulée : notifications pertinentes, réponses historisées, aucune relance devenue inutile.
8. Une personne change d'équipe : les rencontres passées et leurs participants restent inchangés.
9. Un bénévole accepte de tenir la table : il accède aux informations pratiques de sa mission, sans accéder aux dossiers des joueurs.
10. La saison suivante est préparée : les équipes précédentes restent consultables, aucun droit du staff n'est reconduit implicitement.

## 12. Décisions proposées

- Adopter l'entité équipe distincte de l'activité, avec rattachement à une section et à une saison.
- Limiter le premier périmètre à une activité source par équipe ; élargir seulement si les usages le demandent.
- Prévoir dès le premier lot les entraînements communs, renforts ponctuels et réponses des représentants.
- Conserver les paiements et dossiers dans le parcours d'adhésion.
- Commencer par une section pilote et mesurer : membres non affectés, convocations sans réponse à l'échéance, événements effectivement pointés et temps nécessaire pour préparer une rencontre.
- Mesurer les valeurs initiales avec la section pilote avant de fixer des objectifs chiffrés ou un calendrier de développement.
