# Définition de terminé

Une évolution est terminée lorsque les points applicables sont vérifiés et les limites restantes annoncées.

## Fonctionnement

- Les critères d'acceptation sont explicites et vérifiés par des assertions de comportement.
- Les règles affectées sont identifiées et les domaines voisins examinés.
- Les changements métier sont issus de la demande ; les comportements observés ou questions ouvertes ne sont pas validés implicitement par Codex.
- Les corrections de bug comportent un test de régression adapté.
- Les nouveaux droits ont un cas autorisé, un cas refusé et un contrôle du périmètre côté serveur.

## Maintenabilité et données

- Les décisions métier peuvent être localisées et testées ; l'interface et les appels externes ne portent pas seuls la règle.
- Les nouvelles dépendances entre domaines sont justifiées dans l'architecture ou un ADR.
- Les migrations sont additives et vérifiées sur une base jetable ; les changesets déployés sont préservés.
- Le diff ne contient ni secrets, ni données personnelles réelles, ni fichiers générés.

## Preuves

- Le vérificateur de spécifications, les tests backend, les tests frontend, le build production et les parcours navigateur passent dans la CI.
- Les modifications de schéma sont validées sur PostgreSQL et les intégrations externes concernées sur leur sandbox.
- Les fiches, exemples et références de tests sont mis à jour dans la même modification.
- Un test supprimé, ignoré ou dont l'attente métier change est expliqué par une évolution fonctionnelle explicite. Une correction de fixture ou d'assertion technique obsolète doit s'appuyer sur le comportement existant et conserver les assertions métier.
- Le compte rendu indique les validations exécutées, les contrôles indisponibles et le risque résiduel concret.

Une suite avec une API simulée ou H2 ne constitue pas une preuve d'intégration complète sur PostgreSQL
ou chez HelloAsso. Une étape non exécutée reste à effectuer avant la livraison concernée.
