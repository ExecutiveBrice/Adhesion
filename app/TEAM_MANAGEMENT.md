# Équipes et événements

Toutes les activités utilisent des équipes, sans paramètre d’activation et indépendamment du type de section. La section reste un regroupement administratif et un périmètre de droits.

## Équipes et inscriptions

Un clic sur une carte d’activité ouvre sa page dédiée `/activites/:activiteId`, accessible aussi directement par URL. L’onglet de l’**équipe principale** est sélectionné par défaut. L’onglet **Options** contient les paramètres généraux. Un onglet par équipe donne accès à son effectif et à ses événements. Le dernier onglet **Ajouter une équipe** ouvre le formulaire de création et sélectionne la nouvelle équipe après enregistrement. Les paramètres et le brouillon d’événement sont conservés lors du passage par Options. La création d’une activité se fait sur `/activites/nouvelle`, dans Options ; après enregistrement, sa page s’ouvre sur l’équipe principale. Le lien **Retour aux activités** ramène à la liste.

À la création d’une activité, une première équipe « Équipe principale » est créée automatiquement. Son nom, ses encadrants et ses référents sont modifiables depuis la fiche activité. La première équipe est celle créée en premier parmi les équipes actives, et non la première dans l’ordre alphabétique ; elle ne peut pas être archivée.

Les adhérents déjà inscrits sont associés à cette équipe lors de la migration. Chaque nouvelle inscription rejoint ensuite la première équipe, y compris dans l’activité de surclassement. Les inscriptions en attente ou annulées restent associées, mais leur statut les exclut des participants proposés et des accès d’adhérent.

Les responsables peuvent créer d’autres équipes et répartir les effectifs manuellement. Un adhérent peut appartenir à plusieurs équipes. Une modification ultérieure du dossier ou du statut ne rétablit pas une affectation supprimée manuellement. Les affectations prennent effet à la date du changement et conservent leur historique.

## Événements

La fiche activité et la page **Mes équipes** donnent accès aux événements. Un événement possède un type (activité, rencontre, tournoi, autre), un début, une fin, une salle ou un lieu extérieur, un rendez-vous éventuel, des informations pratiques et, pour une rencontre, un adversaire. Le rendez-vous ne peut pas être postérieur au début.

Un événement peut concerner plusieurs équipes d’une même activité : ses participants sont dédupliqués. Il est possible d’ajuster cette sélection, de publier les convocations, de répondre pour soi ou une personne représentée, de relancer les personnes sans réponse et de pointer les présences réelles. Une réponse à une convocation reste distincte du pointage.

Le bouton **Responsabilités** de l’événement ouvre une modale avec deux sélecteurs par ligne : la responsabilité configurée dans l’administration et l’équipe qui la prend en charge. Toutes les équipes actives des activités de la même section sont proposées, même si elles ne participent pas à l’événement ou ne sont pas gérées par l’utilisateur.

À l’enregistrement, le serveur attribue chaque responsabilité au premier référent de l’équipe, ou à son premier adhérent actif si elle n’a aucun référent. Le premier est celui dont l’identifiant d’adhérent est le plus petit, afin de garantir un ordre stable. Les inscriptions annulées ou en attente et les affectations terminées ou futures sont exclues du repli sur l’effectif. Une équipe sans référent ni adhérent éligible ne peut pas prendre de responsabilité. L’équipe et la personne attribuée sont conservées et affichées dans l’événement et les calendriers. L’attribution est recalculée lors d’un nouvel enregistrement des responsabilités.

Une modification d’horaire ou de lieu demande une nouvelle confirmation aux personnes convoquées. Une annulation conserve l’historique. L’actualisation d’un effectif peut être appliquée explicitement aux événements futurs sans convocation publiée, réponse ni pointage ; les autres événements conservent leurs participants.

L’agenda, l’accueil et les exports Google Agenda utilisent ces mêmes événements. **Pointage** et **Pointage secrétariat** offrent une vue quotidienne pour les accès existants, sans créer de calendrier distinct. Les anciennes adresses de ces pages redirigent vers les nouvelles.

Il n’existe plus de catégories, de créneaux hebdomadaires ni de génération automatique d’occurrences.

## Droits et emails

Les administrateurs et les responsables des sections configurent les équipes et leurs effectifs dans leur périmètre. Les encadrants et référents affectés à une équipe gèrent ses événements. Les adhérents et représentants consultent leurs équipes et répondent à leurs convocations publiées. Les droits sont contrôlés côté serveur.

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
