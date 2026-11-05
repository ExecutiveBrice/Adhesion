> Document historique, remplacé par [la gestion généralisée des équipes et événements](TEAM_MANAGEMENT.md). Les options et créneaux décrits ci-dessous ne font plus partie du fonctionnement actuel.

# Gestion optionnelle des équipes par activité — proposition V1

Statut : document de conception à retravailler, accompagné d'une première implémentation.

> Une première version est désormais implémentée. Le [guide d'utilisation](TEAM_MANAGEMENT.md) décrit son périmètre réel, ses différences avec la proposition et les vérifications disponibles. Les routes et certains parcours ci-dessous restent des intentions de conception.

Ce document remplace les choix de rattachement de `TEAM_MANAGEMENT_PROPOSAL.md` : le module dépend de l'activité, sans condition sur le type de sa section. L'équipe occupe, dans la fiche activité, la place des catégories actuelles.

## 1. Principe

Chaque activité possède une option `team_management`, désactivée par défaut.

| Option | Organisation de la fiche activité |
| --- | --- |
| Désactivée | Catégories de séance actuelles : créneaux, encadrants, référents et génération de séances |
| Activée | Équipes : nom, effectif, encadrants, référents, créneaux et événements |

Le paramètre est indépendant de la section : une activité de basket, danse, randonnée ou loisir peut l'activer. Deux activités de la même section peuvent utiliser des modes différents. Le type `COMPETITION` ne conditionne aucune fonction du nouveau module.

Une équipe appartient obligatoirement à une seule activité. Sa section est déduite de cette activité. Les inscriptions, tarifs, capacités et dossiers restent gérés au niveau de l'activité.

## 2. Activation depuis la fiche activité

Ajouter un interrupteur **Gestion par équipes**, accompagné du texte : « Organiser cette activité en équipes, répartir les adhérents et gérer leurs convocations. »

L'administrateur ou le référent habilité à gérer cette activité peut activer l'option. Un encadrant ne peut pas modifier ce paramètre du seul fait de son affectation à une équipe.

### Nouvelle activité

L'utilisateur peut activer l'option puis créer une ou plusieurs équipes. Une équipe peut être enregistrée sans créneau ni membre pour permettre la préparation de l'activité. Dans ce cas, aucune séance n'est générée.

### Activité possédant des catégories

Présenter un aperçu avant enregistrement :

| Catégorie actuelle | Équipe proposée | Créneau conservé |
| --- | --- | --- |
| Adultes débutants | Adultes débutants | Mardi 19 h |
| Adultes confirmés | Adultes confirmés | Jeudi 20 h |

Une équipe est proposée par catégorie ; un descriptif absent donne un nom provisoire « Équipe 1 », « Équipe 2 », etc. L'utilisateur peut renommer les équipes et regrouper plusieurs catégories dans une équipe. Leurs créneaux, salles, encadrants et référents restent visibles.

La conversion conserve les identifiants des planifications et des séances existantes. Elle ne recrée pas de séances, ne réaffecte pas les adhérents et n'envoie aucune convocation. Elle est enregistrée en une transaction avec l'activation du paramètre.

Les anciennes séances conservent leurs participants. Le filtre par effectif s'applique aux nouvelles séances ; un outil séparé permet d'appliquer la nouvelle répartition aux séances futures existantes, avec aperçu des ajouts/retraits. Les réponses, convocations publiées et pointages déjà saisis ne sont jamais écrasés par cette opération.

## 3. Interface proposée

```text
Activité : Basket U15                         Gestion par équipes [activée]

ÉQUIPES                                             [+ Ajouter une équipe]

┌ U15 A ────────────────────────────────────────────────────────────┐
│ 12 membres · 2 encadrants · 2 créneaux                             │
│ Mardi 18 h / Vendredi 18 h                                        │
│ [Effectif] [Créneaux] [Événements] [Modifier] [Archiver]            │
└───────────────────────────────────────────────────────────────────┘

┌ U15 B ────────────────────────────────────────────────────────────┐
│ 11 membres · 1 encadrant · 1 créneau                              │
│ Jeudi 18 h                                                       │
│ [Effectif] [Créneaux] [Événements] [Modifier] [Archiver]            │
└───────────────────────────────────────────────────────────────────┘

Adhérents à répartir : 4                            [Répartir les adhérents]

CALENDRIER DE L'ACTIVITÉ
Filtre : Toutes les équipes / U15 A / U15 B
```

La fiche équipe propose quatre espaces : **Informations**, **Effectif**, **Créneaux**, **Événements**. Les créneaux reprennent le formulaire actuel des catégories. L'équipe peut en posséder plusieurs ; l'interface n'affiche pas un niveau « catégorie » supplémentaire sous les équipes.

## 4. Informations d'une équipe

- Nom obligatoire, unique parmi les équipes actives de l'activité.
- Description et couleur facultatives, couleur de section utilisée par défaut.
- Encadrants et référents, avec une liste de noms par fonction.
- Effectif cible facultatif, utilisé comme information et alerte, sans modifier la capacité de l'activité.
- Un ou plusieurs créneaux facultatifs : jour, début, durée, salle, intervenants spécifiques si nécessaires.
- État active ou archivée.

La catégorie d'âge, le niveau, l'adversaire et le résultat ne deviennent pas des champs obligatoires : le module doit aussi convenir aux activités sans compétition.

## 5. Répartition des adhérents

La fenêtre **Effectif** présente les inscrits de l'activité avec recherche, sélection multiple et filtre « Sans équipe ». La liste affiche l'année de naissance et un indicateur administratif synthétique.

Règles proposées pour la V1 :

1. Affectation manuelle par un gestionnaire habilité ; aucune affectation automatique selon l'âge ou le genre.
2. Une personne peut appartenir à plusieurs équipes de la même activité.
3. Une personne peut rester sans équipe ; elle reste inscrite à l'activité mais ne reçoit pas les convocations de toutes les équipes.
4. Les adhésions annulées ou sur liste d'attente sont exclues des nouvelles affectations. Un dossier en cours est signalé sans être automatiquement bloqué.
5. Les personnes disposant d'un surclassement vers l'activité apparaissent comme candidats identifiés. Une affiliation à la section seule ne suffit pas à intégrer l'équipe.
6. Une affectation ne crée ni adhésion ni paiement. Elle possède une date de début et une fin éventuelle.
7. Le retrait d'une équipe conserve les participations historiques. Les convocations futures déjà publiées sont signalées au gestionnaire pour maintien ou retrait explicite.

Les renforts ponctuels entre équipes d'une même activité peuvent être sélectionnés pour une rencontre sans changer leur affectation. Les renforts provenant d'une autre activité passent uniquement par les autorisations existantes de surclassement dans cette V1.

## 6. Créneaux et séances

Les créneaux actuels sont réutilisés et associés aux équipes. Un créneau peut être partagé par plusieurs équipes, par exemple un entraînement commun. Pour une activité en mode équipe, chaque créneau nouveau doit cibler au moins une équipe.

La commande **Ajouter des séances** conserve le fonctionnement actuel : nombre d'occurrences, date de départ, prise en compte des indisponibilités du calendrier existant. Elle montre les équipes concernées avant validation.

À la création d'une séance, l'effectif prévu correspond à l'union des affectations actives à la date de la séance, sans doublon. Pour une personne surclassée, sa propre adhésion reste la référence. La séance conserve son périmètre d'équipes même si le créneau est modifié ensuite.

Deux équipes peuvent s'entraîner au même horaire dans des salles différentes. La détection des doublons doit donc porter sur le créneau et la date, et non sur la seule heure à l'échelle de l'activité. Un créneau commun produit une seule séance avec plusieurs équipes ciblées.

Un événement ponctuel peut être ajouté à une équipe sans créer de créneau récurrent : séance, rencontre, tournoi ou autre. Les informations de rencontre sont facultatives et affichées uniquement lorsque pertinentes.

## 7. Convocations et présences

Chaque événement propose deux modes :

- **Effectif de l'équipe** : tous les membres concernés sont proposés, utile pour les séances habituelles.
- **Sélection manuelle** : le responsable retient les participants, utile pour une rencontre ou un événement à places limitées.

La sélection est d'abord un brouillon. **Publier les convocations** les rend visibles aux destinataires et déclenche l'email transactionnel. Une relance manuelle cible uniquement les personnes sans réponse.

| Information | États de la V1 |
| --- | --- |
| Convocation | Brouillon, publiée, retirée |
| Réponse du participant | Sans réponse, acceptée, refusée |
| Présence réelle | Non pointée, présent, absent |

Une acceptation ne pointe pas automatiquement la présence. Une réponse manquante n'est jamais interprétée comme un accord. Le responsable peut ajouter un participant après publication ; seul cet ajout est notifié, sans renvoyer toutes les convocations.

Pour les nouveaux événements du module, `Convocation` est la référence de la réponse et `Presence` celle du pointage. L'ancien champ `presencePrevue` reste utilisé par les parcours existants ; le nouvel écran ne propose pas deux réponses concurrentes pour une même personne et un même événement.

L'adhérent répond depuis son agenda ou « Mes équipes ». Le représentant autorisé peut répondre pour un enfant, avec une identification explicite de l'enfant et une trace de l'auteur. Les coordonnées du représentant sont utilisées selon les paramètres de sa fiche. Un lien vers l'application peut être inclus dans l'email ; une réponse par lien public sans authentification est hors V1.

Un report demande une nouvelle réponse tout en conservant l'historique. Une annulation retire l'événement des actions à répondre et empêche les relances.

## 8. Droits proposés

| Profil | Actions |
| --- | --- |
| Administrateur | Paramétrage du mode et gestion complète du module |
| Référent de section | Paramétrage et gestion des activités appartenant à ses sections |
| Référent d'activité explicitement habilité | Équipes et effectifs de cette activité ; l'activation du mode reste réservée aux gestionnaires actuels de l'activité |
| Encadrant/référent d'équipe | Calendrier, sélections, convocations et pointage de ses équipes |
| Encadrant d'un créneau uniquement | Séances de ce créneau, sans accès implicite aux autres équipes |
| Adhérent | Consultation de ses équipes et réponse à ses convocations |
| Représentant autorisé | Consultation et réponse pour les personnes représentées |

La section conserve un rôle de délégation et de filtre ; elle ne décide pas de la disponibilité du module. Tous les endpoints contrôlent à la fois le droit, l'activité de rattachement et la valeur de `team_management` pour les opérations propres au module.

La gestion des effectifs et les paramètres d'activité restent séparés des dossiers financiers. Les affectations d'équipe ne donnent pas accès aux autres activités de la section.

## 9. Désactivation et archivage

La désactivation est une opération distincte de la suppression :

- L'interface redevient celle des catégories de séance. Les créneaux partagés restent une seule catégorie chacun.
- Les équipes et affectations sont conservées et réutilisées lors d'une réactivation.
- Les séances existantes conservent leurs participants et leur historique ; la désactivation n'y ajoute pas tous les inscrits de l'activité.
- Les nouvelles séances du mode simple suivent les règles actuelles de l'activité.
- La désactivation est refusée tant que des convocations publiées à venir nécessitent un suivi. L'écran liste les événements à terminer ou annuler ; il ne les annule pas automatiquement.
- Les données historiques restent consultables par les profils autorisés, même option désactivée ; les commandes propres au module sont fermées.

Une équipe vide sans historique peut être supprimée. Une équipe utilisée est archivée, retirée des choix de création et consultable avec ses anciennes participations. Ses séances futures doivent être traitées avant archivage.

## 10. Implémentation technique envisagée

| Élément | Évolution |
| --- | --- |
| `Activite` | Booléen Java `teamManagement`, colonne SQL et propriété JSON `team_management`, valeur initiale `false` |
| `Equipe` | Activité obligatoire, nom, description, couleur et archivage ; section déduite |
| `AffectationEquipe` | Équipe, adhérent, adhésion source, début et fin |
| Staff d'équipe | Relations dédiées encadrants/référents avec vérification des permissions |
| `PlanificationHebdomadaire` | Lien vers une ou plusieurs équipes de la même activité |
| `Seance` | Équipes ciblées enregistrées lors de la création, type d'événement et détails facultatifs de rencontre |
| `Convocation` | Séance, adhérent, adhésion source, publication, réponse et auteur |
| `Presence` | Pointage existant, population filtrée selon le mode et le périmètre de la séance |

Un objet `Equipe` distinct permet de donner plusieurs créneaux à une équipe sans dupliquer son effectif. Il n'ajoute pas de niveau de navigation : dans la fiche, l'équipe remplace la carte catégorie, ses créneaux sont édités dans sa fenêtre.

Les routes proposées sont centrées sur l'activité :

```text
PATCH /activite/{id}/team-management
GET/POST /activite/{id}/equipes
PUT/DELETE /activite/{id}/equipes/{equipeId}
GET/PUT /activite/{id}/equipes/{equipeId}/effectif
GET/POST /activite/{id}/equipes/{equipeId}/evenements
POST /activite/{id}/seances/{seanceId}/convocations/publier
POST /activite/{id}/seances/{seanceId}/convocations/relancer
PATCH /seance/{seanceId}/convocations/{convocationId}/reponse
```

Une migration Liquibase ajoute les structures et initialise le flag à `false`, sans convertir les activités en masse. Les DTO Angular/Java, les listes d'activités, les agendas, les requêtes de présence et les contrôles des encadrants sont adaptés ensemble. Le flag est modifié par le parcours dédié ; un enregistrement générique d'activité ne peut pas contourner les règles d'activation/désactivation.

La mise à jour d'un effectif possède un aperçu pour les séances futures non publiées. Elle conserve les participants ayant déjà une réponse ou un pointage. Les opérations de publication/relance doivent résister à une double soumission sans envoyer deux fois le même message.

## 11. Périmètre de livraison proposé

La première version utilisable couvre activation/conversion, équipes et effectifs, créneaux, génération des séances, événements ponctuels, convocations par email, réponses adhérent/représentant, relance manuelle, agenda et pointage. Elle se teste sur deux activités de sections différentes, dont une non compétitive.

Les chats d'équipe, relances automatiques, indisponibilités par période, besoins bénévoles détaillés, covoiturage, imports fédéraux et statistiques sportives restent des évolutions ultérieures. Les chats d'activité et responsabilités simples déjà présents peuvent continuer à être utilisés.

Ne pas introduire de cycle de saison obligatoire propre aux équipes dans cette V1 : le rattachement suit la durée de vie de l'activité. L'historique des affectations est daté. Le mécanisme de changement d'exercice devra néanmoins préserver ou archiver les équipes avant toute suppression d'activité ; la reconduction automatique n'appartient pas à ce périmètre.

## 12. Critères d'acceptation

1. Une activité non compétitive peut activer le module ; une autre activité de la même section reste en mode simple.
2. Les catégories existantes deviennent des équipes avec leurs créneaux sans recréer les séances ni attribuer automatiquement tous les adhérents.
3. Une équipe à deux créneaux possède un seul effectif ; un créneau partagé produit une seule séance et une présence par personne.
4. Deux équipes peuvent avoir des séances simultanées distinctes dans la même activité.
5. Un membre affecté à l'équipe A ne figure pas par défaut parmi les participants de l'équipe B.
6. Une modification d'effectif ne réécrit ni les événements passés ni les réponses ou pointages déjà enregistrés.
7. Un parent autorisé répond pour son enfant et ne peut pas répondre pour un autre adulte de sa tribu sans délégation.
8. La publication et la relance sont tracées ; une double soumission ne déclenche pas deux emails identiques.
9. Les identifiants d'une autre activité sont refusés dans toutes les commandes équipe/créneau/séance.
10. Désactivation puis réactivation conservent les équipes, affectations, créneaux et historiques ; les convocations à venir empêchent une désactivation incomplète.
