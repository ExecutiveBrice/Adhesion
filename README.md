http://localhost:8000/swagger-ui/index.html

## Matériel et emprunts

La page **Matériel** est accessible aux membres du CA (`ROLE_MEMBRECA`), aux administrateurs (`ROLE_ADMIN`) et à la Logistique (`ROLE_LOGISTIQUE`). Elle liste chaque objet, son zonage coloré dans le stock et sa disponibilité. Les filtres permettent de rechercher un objet, un zonage ou un emprunteur.

Les administrateurs et la Logistique configurent les emplacements depuis le bouton **Gérer les zonages** de la page **Matériel**. La modale permet d’ajouter, de modifier et de supprimer les zonages, avec un nom et une couleur. À sa fermeture, la liste du matériel et les zonages sont actualisés. Les objets s’ajoutent depuis le bouton **Ajouter du matériel**, en choisissant leur zonage.

Pour un membre du CA, le bouton **Emprunter** enregistre l’utilisateur connecté. Pour un administrateur, il ouvre une modale de sélection parmi les membres du conseil d’administration, avec son propre nom présélectionné s’il appartient au CA. Le matériel apparaît alors **Emprunté** avec le nom de l’emprunteur et la date. L’emprunteur ou n’importe quel administrateur peut le **Restituer**. Deux utilisateurs ne peuvent pas emprunter simultanément le même objet. Un zonage contenant du matériel ne peut pas être supprimé.

Chaque matériel possède un **Journal d’emprunt**, accessible au CA, aux administrateurs et à la Logistique, avec les emprunteurs, les dates d’emprunt et de retour, et les personnes ayant enregistré ces opérations. Les noms restent conservés même après la suppression d’un compte. Les emprunts encore en cours avant l’ajout du journal sont repris ; les anciens emprunts déjà terminés ne peuvent pas être reconstitués.

Le booléen **Hors service (HS)** est renseigné à l’ajout ou modifié par un administrateur dans la liste avec un bouton **HS**, gris lorsque le matériel est en service (`false`) et rouge lorsqu’il est hors service (`true`). Sa couleur est le seul indicateur visuel de cet état ; les membres du CA voient le bouton sans pouvoir le modifier. Le matériel reste **Disponible** ou **Emprunté**, indépendamment de HS, et peut toujours être emprunté ou restitué. Une restitution conserve le statut HS. Le zonage et le groupe état/bouton HS restent chacun sur une seule ligne.

Chaque changement effectué avec le bouton **HS** ajoute au journal une entrée **Déclaré hors service** ou **Remis en service**, avec la date et le nom de l’administrateur. Un changement HS conserve l’emprunt en cours. Une requête répétant le même état ne crée pas de doublon.

Les tables `zonage`, `materiel` et `emprunt_materiel` sont créées au démarrage par la configuration Hibernate existante (`ddl-auto: update`). Les matériels existants sont en service par défaut. Les endpoints `/materiel` appliquent les mêmes restrictions de rôles côté serveur. Sur une base possédant déjà le journal, exécuter `app/sql/migration-journal-hs.sql` avant le déploiement pour autoriser les entrées HS sans emprunteur.

Le rôle **Logistique** se gère depuis l’administration ou la fiche adhérent. Les administrateurs et la Logistique disposent du bouton **Modifier** sur chaque matériel : changement du nom ou du zonage, et suppression après confirmation. La suppression est refusée tant que le matériel est emprunté et supprime également son journal d’emprunt. Les droits d’emprunt, de restitution et du bouton HS restent inchangés.

Avant de déployer ce rôle sur une base existante, exécuter `app/sql/migration-logistique.sql` pour mettre à jour la contrainte des rôles (après `migration-roles.sql` si la base utilise encore les anciennes tables).
