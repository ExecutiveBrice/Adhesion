# Équipes et événements

Toutes les activités utilisent des équipes, sans paramètre d’activation et indépendamment du type de section. La section reste un regroupement administratif et un périmètre de droits.

## Équipes et inscriptions

Un clic sur une carte d’activité ouvre sa page dédiée `/activites/:activiteId`, accessible aussi directement par URL. L’onglet de l’**équipe principale** est sélectionné par défaut. L’onglet **Options** contient les paramètres généraux. Un onglet par équipe donne accès à son effectif et à ses événements. Le dernier onglet **Ajouter une équipe** ouvre le formulaire de création et sélectionne la nouvelle équipe après enregistrement. Les paramètres et le brouillon d’événement sont conservés lors du passage par Options. La création d’une activité se fait sur `/activites/nouvelle`, dans Options ; après enregistrement, sa page s’ouvre sur l’équipe principale. Le lien **Retour aux activités** ramène à la liste.

À la création d’une activité, une première équipe « Équipe principale » est créée automatiquement. Son nom, ses encadrants et ses référents sont modifiables depuis la fiche activité. La première équipe est celle créée en premier parmi les équipes actives, et non la première dans l’ordre alphabétique ; elle ne peut pas être archivée.

Les adhérents déjà inscrits sont associés à cette équipe lors de la migration. Chaque nouvelle inscription rejoint ensuite la première équipe, y compris dans l’activité de surclassement. Les inscriptions en attente ou annulées restent associées, mais leur statut les exclut des participants proposés et des accès d’adhérent.

Les responsables peuvent créer d’autres équipes et répartir les effectifs manuellement. Un adhérent peut appartenir à plusieurs équipes. Une modification ultérieure du dossier ou du statut ne rétablit pas une affectation supprimée manuellement. Les affectations prennent effet à la date du changement et conservent leur historique.

## Événements

Tous les adhérents peuvent consulter tous les événements, même sans inscription à l’activité, affectation à une équipe ou convocation publiée. Chaque événement dispose d’une page `/evenements/:id`, accessible depuis l’icône de redirection de chaque événement dans les activités et les équipes, ou directement au clic dans l’Agenda, le calendrier ou la liste d’accueil. Les événements Google conservent leur fenêtre de détail. Ses trois onglets présentent les informations complètes (activité, équipes, type, dates, rendez-vous, lieu, adresse, adversaire, état et informations pratiques), les participants et responsabilités, puis le chat propre à l’événement. Dans la fiche événement, les participants sont présentés dans un tableau en lecture seule : nom complet et réponse à l’invitation alignée à droite. Chaque état possède une icône ; sur petit écran, seule l’icône est visible. La recherche et les boutons de publication et de relance sont absents de cet onglet. La gestion détaillée des convocations, des réponses et du pointage reste disponible depuis les équipes. Les responsabilités restent modifiables selon les droits existants. Les personnes non gestionnaires ne consultent que les convocations auxquelles elles ont déjà accès.

`GET /teams/events/{id}` fournit le détail à tout utilisateur authentifié, et `GET /teams/equipes/{id}/events` liste tous les événements de l’équipe. La visibilité ne donne aucun droit supplémentaire de modification, de publication, de relance, de réponse ou de pointage. Le champ `canChat` indique si le chat est accessible ; cet onglet est masqué dans les autres cas, y compris lors d’un accès direct par URL. Le chat utilise `GET` et `POST /teams/events/{id}/messages` ; sa lecture et son écriture restent réservées aux gestionnaires, encadrants, référents, membres des équipes concernées et destinataires de convocations sélectionnées et publiées (ou leurs représentants). Ces droits sont contrôlés séparément côté serveur. Les auteurs sont déterminés côté serveur. Les messages, limités à 2 000 caractères, sont conservés dans `event_chat_messages` (migration v21), y compris après annulation de l’événement. L’historique est paginé par 50 messages (`beforeId` ou `afterId`) et le client actualise toutes les cinq secondes lorsque le chat est affiché. Le brouillon est conservé entre les onglets et en cas d’erreur d’envoi.

La fiche activité et la page **Mes équipes** donnent accès aux événements. Un événement possède un type (activité, rencontre, tournoi, autre), un début, une fin, une salle ou un lieu extérieur, un rendez-vous éventuel, des informations pratiques et, pour une rencontre, un adversaire. Le rendez-vous ne peut pas être postérieur au début.

Un événement peut concerner plusieurs équipes d’une même activité : ses participants sont dédupliqués et se gèrent uniquement depuis l’effectif des équipes. La sélection manuelle propre à un événement et ses routes API sont supprimées. Il est possible de publier les convocations, de répondre pour soi ou une personne représentée, de relancer les personnes sans réponse et de pointer les présences réelles. Une réponse à une convocation reste distincte du pointage.

La réponse à l’invitation propose **Présent**, **Absent** ou **Ne sais pas encore**, sans choix présélectionné. Les gestionnaires de l’événement peuvent saisir ces réponses pour chaque participant sélectionné dont l’inscription est éligible, y compris avant publication ou après le début de l’événement, sauf annulation. Les adhérents et représentants répondent uniquement à leurs convocations publiées pour un événement à venir. La présence réelle propose uniquement **Présent** ou **Absent**, également sans présélection. Répondre ne renseigne jamais la présence réelle ; pointer ne change jamais la réponse. « Ne sais pas encore » est une réponse enregistrée, distincte de « Sans réponse » et exclue des relances aux personnes sans réponse.

L’API de réponse accepte `{ "value": true }`, `{ "value": false }` ou `{ "value": null }` pour l’indécision explicite. Un champ `value` absent est refusé. Le stockage existant distingue l’indécision de l’absence de réponse grâce à `dateReponse` : une réponse nulle datée signifie « Ne sais pas encore », une réponse nulle non datée signifie « Sans réponse ». Le pointage refuse une valeur nulle. Aucun changement de schéma n’est nécessaire.

Le bouton **Responsabilités** de l’événement ouvre une modale avec deux sélecteurs par ligne : la responsabilité configurée dans l’administration et l’équipe qui la prend en charge. Toutes les équipes actives des activités de la même section sont proposées, même si elles ne participent pas à l’événement ou ne sont pas gérées par l’utilisateur.

À l’enregistrement, le serveur attribue chaque responsabilité au premier référent de l’équipe, ou à son premier adhérent actif si elle n’a aucun référent. Le premier est celui dont l’identifiant d’adhérent est le plus petit, afin de garantir un ordre stable. Les inscriptions annulées ou en attente et les affectations terminées ou futures sont exclues du repli sur l’effectif. Une équipe sans référent ni adhérent éligible ne peut pas prendre de responsabilité. L’équipe et la personne attribuée sont conservées et affichées dans l’événement et les calendriers. L’attribution est recalculée lors d’un nouvel enregistrement des responsabilités.

Une modification d’horaire ou de lieu demande une nouvelle confirmation aux personnes convoquées. Une annulation conserve l’historique. Toute modification d’un effectif actualise automatiquement les événements futurs non annulés concernés, même après publication des convocations, réponse ou pointage. Les nouveaux membres sont ajoutés en brouillon ; les départs sont retirés de la sélection et une notification de retrait est mise en file si la convocation avait été publiée. Les participants inchangés conservent leurs réponses et pointages ; les retraits conservent également leur historique. Les sélections manuelles propres à l’événement sont préservées pour les personnes dont l’appartenance à l’effectif ne change pas. Les événements passés, annulés ou sans lien avec l’équipe restent inchangés. La case d’actualisation et le paramètre `applyToFuture` sont supprimés.

L’agenda, l’accueil et les exports Google Agenda utilisent ces mêmes événements. **Pointage** et **Pointage secrétariat** offrent une vue quotidienne pour les accès existants, sans créer de calendrier distinct. Les anciennes adresses de ces pages redirigent vers les nouvelles.

Il n’existe plus de catégories, de créneaux hebdomadaires ni de génération automatique d’occurrences.

## Droits et emails

Les administrateurs et les responsables des sections configurent les équipes et leurs effectifs dans leur périmètre. Les encadrants et référents affectés à une équipe gèrent ses événements. La page **Mes équipes** (`GET /teams/mine`) liste uniquement les équipes dont l’utilisateur connecté est personnellement référent ou encadrant, quel que soit son rôle global. Une inscription, une convocation ou la représentation d’un membre ne suffit pas à faire apparaître une équipe dans cette page. Tous les adhérents ont accès aux fiches événements depuis les calendriers. Les adhérents et représentants conservent l’accès à leurs seules convocations publiées. Les droits sont contrôlés côté serveur.

La publication place les emails dans une file, avec regroupement par adresse de destination, et ne modifie pas les abonnements newsletter. Elle utilise `BREVO_APIKEY_PRIVATE`, le paramètre d’envoi existant et `ADHESION_FRONTEND_BASE_URL`. Sans clé ou lorsque les envois sont désactivés, les messages restent en attente. La migration n’envoie aucun email.

La publication est idempotente ; une relance utilise un identifiant de commande pour éviter les doublons en cas de répétition réseau. Le suivi affiché distingue attente, envoi engagé, acceptation par le prestataire, refus et issue incertaine. Une issue incertaine ne provoque pas un nouvel envoi automatique.

## Reprise des données et démarrage

La migration Liquibase `v17-events-only`, après la création des tables d’équipes en v16 :

- reprend chaque ancienne catégorie non déjà liée comme équipe, avec son nom et ses intervenants ;
- conserve les équipes existantes et associe tous les adhérents à la première équipe ;
- rattache chaque ancienne séance aux équipes concernées et la conserve comme événement avec le même identifiant, ses dates, sa salle, ses participants, réponses, responsabilités et pointages ;
- reprend les anciennes réponses dans les convocations sans toucher aux convocations d’équipe déjà existantes ;
- supprime les tables de planification, leurs liens et les colonnes d’activation et de récurrence.

La table physique historique `seance` et certains noms Java/JSON restent des détails de compatibilité pour conserver les identifiants et relations. Ils ne représentent plus une fonction distincte. Les nouvelles créations passent uniquement par les API d’événements d’équipe.

La migration `v18-responsabilites-equipe` ajoute l’équipe aux responsabilités. Les attributions historiques à un adhérent sont conservées ; il faut leur choisir une équipe lors de leur prochaine modification.

Redémarrer le serveur et reconstruire le client pour appliquer cette version. Aucune base réelle n’a été migrée pendant le développement.

## Vérifications

La création utilise une modale commune depuis une équipe ou l’Agenda. Depuis l’Agenda, plusieurs équipes ou sections peuvent être cochées ; une section inclut toutes ses équipes actives. Les équipes sélectionnées à la fois directement et par leur section sont dédupliquées. Les événements restent rattachés à une activité : une sélection couvrant plusieurs activités crée un événement par activité, dans une seule transaction.

Le rendez-vous est proposé une heure avant le début et suit les changements du début jusqu’à sa modification manuelle. La répétition couvre 2 à 104 semaines calendaires, première semaine incluse, sans prolonger la période pour remplacer les dates exclues. Les vacances de la zone configurée par `ZONE_SCOLAIRE` et les jours fériés sont exclus. Une erreur des sources de calendrier bloque la création complète. Les occurrences gardent leurs horaires locaux, y compris au changement d’heure.

API : `GET /teams/event-options` fournit les équipes et sections autorisées ; `POST /teams/events` accepte `{ evenement: EventEdit, sections: number[], semaines: number }` et renvoie `{ nombre, evenements }`. Les créations simples depuis une équipe et les modifications conservent les routes existantes.

Client : `npm run build` et `npm run test:teams` (écrans d’équipes, événements, activité, agenda et détail). Les tests Angular généraux ont des erreurs de compilation préexistantes dans les tests de connexion, d’administration et de mailing ; la commande ciblée utilise son propre tsconfig.

Serveur : tests de migration et validation des mappings Hibernate, affectations automatiques, surclassements, redistribution, convocations, droits, pointage, responsabilités et calendriers. Les tests utilisent H2 et des services externes simulés.

## Invitations aux responsabilités

Chaque responsabilité possède une convocation et une réponse propres, même si son attributaire est aussi participant ou assure plusieurs tâches. La publication, les relances individuelles ou groupées, les réponses « Présent / Absent / Ne sais pas encore », les droits de réponse et l’historique utilisent le même fonctionnement que les participants. Une réponse à une responsabilité ne modifie pas la réponse du participant ni sa présence réelle.

Les attributions inchangées conservent leur réponse. Un retrait conserve l’historique ; une réattribution demande une nouvelle publication. Un report remet les réponses en attente et une annulation bloque les réponses. Les référents sans adhésion à l’activité peuvent recevoir et répondre à une invitation de responsabilité ; leurs convocations publiées donnent accès à l’événement et apparaissent dans leur calendrier. Les emails indiquent la responsabilité et conduisent directement à l’événement.

La migration `v22-responsibility-invitations` conserve les réponses des participants et prépare les responsabilités existantes en brouillon, sans envoyer d’email.
