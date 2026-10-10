# Identité, session et droits

Acteurs : visiteur, adhérent connecté, secrétaire, administrateur, référent affecté à une section,
responsable boutique. Les rôles globaux et les affectations ne sont pas interchangeables.

### ACCESS-001 — Chaque route possède une politique d'accès

Statut : Observée.
Une route HTTP est protégée par la sécurité de méthode ou figure dans l'inventaire
`PublicApiEndpoints` avec sa justification. Une garde Angular ne remplace pas cette protection.
Exemple : ajouter une route boutique sans `@PreAuthorize` fait échouer le test d'architecture.
La présence de l'annotation ne prouve pas à elle seule que son expression est suffisamment restrictive.

### ACCESS-002 — Les consultations d'un référent sont limitées à ses sections

Statut : Documentée.
Les listes gérées d'adhésions et d'adhérents appliquent en base les sections affectées au compte
à chaque requête. Pour les adhérents, le périmètre inclut les membres des tribus ayant une adhésion
dans ces sections, quel que soit son statut. Sans affectation, la consultation gérée est refusée.
Exemple : un référent de Yoga ne voit pas les adhésions d'une autre section en modifiant un filtre.
Source : [gestion des sections](../../app/SECTION_MANAGEMENT.md).

### ACCESS-003 — La révocation de l'affectation retire les droits associés

Statut : Documentée.
La gestion des chats de section vérifie l'affectation et le rattachement demandé à chaque lecture
et modification. Un référent ne peut pas déplacer le chat vers une autre section, une activité ou l'association.
Exemple : une ancienne URL de gestion ne permet plus de modifier le chat après retrait du référent.
Source : [gestion des sections](../../app/SECTION_MANAGEMENT.md).

### ACCESS-004 — La récupération de mot de passe ne divulgue pas le compte

Statut : Observée.
La demande retourne la même réponse acceptée pour un compte connu ou inconnu, sans retourner le jeton.
Le jeton expire, est à usage unique et ne peut être consommé simultanément par deux confirmations.
Exemple : rejouer une confirmation réussie ne change pas une seconde fois le mot de passe.

### ACCESS-005 — Rechercher et ouvrir une session d'impersonnalisation

Statut : Documentée.
Évolution demandée : dans la page Administration, le sélecteur d'impersonnalisation possède
un filtre texte. La sélection d'un adhérent recharge le document après réception et enregistrement
de la nouvelle session, afin de réinitialiser les données et les droits de l'interface.
Le rechargement ouvre l'accueil, comme après connexion.

Critères d'acceptation :
- Le filtre recherche dans le nom affiché et l'identifiant, sans distinction de casse ou d'accents,
  et ignore les espaces en début et fin de saisie. Une saisie vide affiche tous les utilisateurs.
- Le menu reste ouvert pendant la saisie et indique lorsqu'aucun utilisateur ne correspond.
- Un clic lance l'impersonnalisation ; le jeton et le profil reçus sont enregistrés avant le
  rechargement complet. Les sélections supplémentaires sont bloquées pendant la requête.
- Un refus conserve la session courante, affiche une erreur et permet de réessayer sans rechargement.

Autorisation existante observée : la route Angular `/admin` et l'endpoint
`POST /auth/impersonate/{username}` sont réservés au rôle administrateur (ACCESS-001).
Le filtre ne modifie pas le périmètre de la liste serveur. Aucun changement de données adhérent
ou d'autorisation serveur n'est introduit ; les autres domaines retrouvent la session ciblée au rechargement.
Exemple : rechercher « ELODIE » retrouve « Élodie Martin » ; sélectionner cette personne ouvre
l'accueil avec ses droits et retire les liens d'administration si elle n'est pas administratrice.
Les parcours navigateur utilisent une API simulée et ne prouvent pas l'autorisation serveur.

### ACCESS-006 — Le menu donne accès à l'inscription ouverte

Statut : Documentée.
Correction demandée : sur la page de connexion, le bandeau propose le bouton
« Inscription » aux visiteurs lorsque le paramètre booléen `Inscription` vaut `true`.
Le bouton ouvre le formulaire de création de compte dans la même page.
Le formulaire et son enchaînement création de compte puis connexion existaient avant
la refonte de la page (`e283600`) ; ils sont rétablis avec la navigation actuelle vers l'accueil.

Critères d'acceptation :
- Le bouton apparaît après chargement du paramètre activé ; il est masqué si le paramètre
  est désactivé, absent, en cours de chargement ou si son chargement échoue.
- Le formulaire reste accessible depuis la récupération de mot de passe ; « Connexion »
  permet de revenir au formulaire de connexion, y compris sur mobile.
- La création utilise `POST /auth/signup` avec l'e-mail et le mot de passe, après validation
  du formulaire et acceptation du texte RGPD affiché par le bouton de soumission existant.
- Après création, la connexion enregistre la session avant le rechargement vers l'accueil.
  Un refus de création conserve le formulaire et permet de réessayer sans lancer de connexion.
- Si la connexion échoue après création, l'utilisateur peut revenir à « Connexion »
  sans tenter de recréer le compte. Les doubles soumissions sont bloquées pendant la requête.

Autorisations préservées (ACCESS-001) : `/auth/signup` est déjà publique ; les routes
de profil et d'adhésion restent protégées. Le paramètre pilote ici l'interface, comme
dans l'ancien parcours ; cette correction n'ajoute pas de contrôle serveur de fermeture
des inscriptions. Aucun rôle, périmètre de données ou schéma n'est modifié.
Les parcours navigateur simulent l'API et ne prouvent pas l'envoi du mail de confirmation.

## Matrice de droits à préserver

| Opération | Condition | Périmètre |
| --- | --- | --- |
| Consultation gérée d'adhésions | Affectation de référent de section | Sections affectées |
| Consultation gérée d'adhérents | Affectation de référent de section | Tribus liées aux sections |
| Gestion de chats par un référent | Affectation + cible SECTION | Chats de ses sections |
| Lecture et écriture de messages | Règles CHAT-001 à CHAT-003 | Chat demandé |
| Gestion boutique | Rôle responsable boutique ou administrateur selon les endpoints | Routes d'administration boutique |
| Impersonnalisation | Rôle administrateur | Compte sélectionné dans la liste d'administration |
| Routes sans session | Inventaire PublicApiEndpoints | Méthode et chemin explicitement recensés |

Cette matrice est partielle. Pour une nouvelle opération, ajouter le rôle, le périmètre et un cas de refus.

## Questions ouvertes

- Compléter progressivement la matrice des endpoints historiques : profil, exports, reporting et séances.
- Vérifier les parcours PWA réels sur mobile en complément des tests de rotation des jetons.
