# Audit de sécurité — 16 novembre 2026

Audit ciblé du dépôt Adhesion, révision `3c8cb22` : API Spring, client Angular, authentification, autorisations, documents, paiements et configuration Docker. Aucun correctif applicatif ni test sur la production. Le classement privilégie l'impact et la facilité d'exploitation. Les niveaux sont qualitatifs.

| Priorité | Risque | Gravité | Validation |
| --- | --- | --- | --- |
| 1 | Accès aux dossiers et documents d'autres adhérents | Critique | Lecture/suppression de fichier reproduites via MockMvc avec la vraie configuration de sécurité ; suppression de dossier autorisée au niveau contrôleur, service inspecté |
| 2 | Statuts administratifs et paiements manipulables par un compte standard | Critique | Autorisations MVC et comportements des services reproduits séparément |
| 3 | Requêtes HTTP côté serveur par injection HTML dans une attestation PDF | Élevée | Requête vers un serveur HTTP local reproduite avec un prénom injecté |
| 4 | PostgreSQL publié hors du réseau applicatif | Élevée si cette configuration est déployée | Configuration inspectée ; exposition effective en production non mesurée |
| 5 | Protection insuffisante contre les attaques sur les comptes | Élevée | Politique de mot de passe faible confirmée ; absence de limitation signin/signup dans le dépôt |

## 1. Vérifier les droits sur chaque dossier et chaque document

**Constat.** Le rôle `USER` suffit pour modifier ou supprimer un adhérent à partir de son identifiant. `AdherentServices.update()` et `deleteAdherent()` chargent directement l'objet sans vérifier son appartenance à la famille de l'utilisateur connecté. Le contrôle spécifique à la modification d'e-mail ne protège pas les autres champs. Les opérations documentaires vérifient le format de l'identifiant et le chemin, mais jamais les droits sur l'adhérent.

**Preuves dans le code.** `app/src/main/java/com/wild/corp/adhesion/controllers/AdherentController.java:80`, `services/AdherentServices.java:140` et `:239` ; `controllers/FileUploadController.java:33`, `:58`, `:79`, `:88` (même préfixe Java).

**Reproduction locale.** Un utilisateur fictif `ROLE_USER` lit puis supprime `424242/certificat.pdf` au travers de la vraie chaîne de sécurité. Aucun service d'appartenance n'existe dans ce contrôleur. Le même rôle obtient HTTP 200 sur la suppression de l'adhérent 424242 et le service est appelé avec cet identifiant ; ce dernier a été simulé pour éviter toute suppression réelle.

**Impact.** Divulgation, altération ou suppression de documents privés ; modification des données personnelles et des consentements ; suppression de dossiers et de comptes associés. Les identifiants numériques rendent les cibles faciles à parcourir.

**Correctif prioritaire.** Centraliser une vérification d'accès : utilisateur appartenant à la tribu autorisée ou rôle administratif explicitement habilité. L'appliquer avant toute lecture, mutation, journalisation métier ou opération de fichier, y compris régénération d'attestation, création d'adhésion et relations vers un représentant. Filtrer les recherches par identité connectée. Ajouter des tests où Alice reçoit 403/404 sur chaque ressource de Bob.

## 2. Réserver les statuts et les paiements aux acteurs habilités

**Constat.** `GET /adhesion/choisirStatut` et `POST /adhesion/savePaiement` acceptent `ROLE_USER`. Le premier transmet librement le statut au service. Le statut « Licence T » valide les indicateurs de paiement et de documents et supprime les accords. Le second ajoute un règlement « HelloAsso » au tarif de l'activité, sans vérification auprès du prestataire.

**Preuves.** `controllers/AdhesionController.java:103` et `:154` ; `services/AdhesionServices.java:247` et `:454`.

**Reproduction locale.** MockMvc confirme l'accès avec un simple `USER`. Deux tests de service confirment la validation des indicateurs par « Licence T » et la création d'un paiement « HelloAsso » à 250 à partir d'un objet paiement vide. La route utilise effectivement le paramètre de requête `adhesionId`.

**Impact.** Fraude aux inscriptions, contournement de contrôles administratifs et incohérences comptables, y compris sur son propre dossier : ce risque subsiste même après correction de l'appartenance du point 1. Il concerne le parcours historique des adhésions ; ce constat ne signifie pas que le paiement de la boutique est contourné.

**Correctif prioritaire.** Réserver les transitions administratives et les écritures manuelles aux rôles habilités. Définir les transitions permises selon le rôle et l'état courant. Un paiement prestataire doit provenir d'une vérification serveur de son montant, de sa référence et de son bénéficiaire, avec idempotence. Utiliser POST/PATCH pour les changements d'état. Tester qu'un membre ne peut ni se déclarer validé ni créer un règlement confirmé.

## 3. Échapper les données injectées dans les PDF et bloquer les ressources distantes

**Constat.** Le prénom et le nom sont concaténés à du HTML dans `PdfService.generateSynthese()`, puis rendus avec `th:utext`. Le passage par HtmlCleaner conserve les balises utiles à l'injection. Le moteur PDF peut charger une image à une URL fournie dans ce contenu. La régénération est accessible à tout utilisateur authentifié.

**Preuves.** `services/PdfService.java:53` et `:111` ; `app/src/main/resources/templates/attestationTemplate.html:48` ; `controllers/AdherentController.java:134` ; `services/AdherentServices.java:483`.

**Reproduction locale.** Un objet adhérent fictif porte un prénom contenant une balise image vers `http://127.0.0.1:<port-temporaire>/audit.png`. L'appel réel à `generateSynthese()` génère un PDF et provoque une requête sur ce serveur local. Ce test valide le moteur PDF ; le parcours complet HTTP modification-du-profil puis régénération n'a pas été exécuté.

**Impact.** Requêtes vers des services accessibles depuis le serveur (SSRF), délais ou consommation de ressources, falsification du contenu des attestations. L'accès à des métadonnées cloud, à des fichiers locaux ou à des secrets n'a pas été testé ni démontré.

**Correctif prioritaire.** Séparer les données utilisateur du balisage de confiance et rendre les champs avec `th:text`, ou les échapper individuellement avant composition. Autoriser uniquement les images et polices embarquées nécessaires ; refuser les ressources réseau et les chemins arbitraires au niveau du moteur. Restreindre aussi les sorties réseau du conteneur. Tester qu'une balise fournie comme prénom reste du texte et ne déclenche aucune requête.

## 4. Isoler PostgreSQL

**Constat.** `docker-compose.yml:72` publie `8105:5432`, sans restriction à localhost. Les lignes 76–82 placent PostgreSQL sur `traefik_web` et déclarent un routeur TCP `HostSNI(*)` sur l'entrée `adhesion_db`. Les commentaires « supprimer en remote » ne changent pas la configuration exécutée.

**Impact.** Si ce Compose est utilisé sur un serveur accessible et que les règles réseau l'autorisent, PostgreSQL devient directement joignable : attaques sur les identifiants, exposition du service et accès aux données en cas de compromission. L'existence de l'entrée Traefik et les pare-feu de production restent à vérifier.

**Correctif prioritaire.** Retirer la publication du port et le routage Traefik de la configuration de production. Placer la base sur un réseau privé partagé uniquement avec l'API. Pour la maintenance, employer un tunnel ou une publication locale dédiée. Vérifier depuis l'extérieur que le port est fermé et que l'API garde accès à la base.

## 5. Renforcer la protection des comptes

**Constat.** L'inscription accepte des mots de passe de six caractères (`security/payload/request/SignupRequest.java:19`). La limitation de débit existante est spécifique à la réinitialisation ; aucun mécanisme équivalent pour `/auth/signin` ou `/auth/signup` n'apparaît dans le code ni dans les labels du proxy fournis. L'inscription révèle également si une adresse existe. Le validateur plus fort employé par la réinitialisation n'est pas appliqué à l'inscription.

**Reproduction locale.** Le test existant `SignupAuthorizationMvcTest.anonymousVisitorCanCreateAnAccount` confirme HTTP 200 pour `secret1`. Aucun essai massif de connexion n'a été lancé. Une éventuelle protection externe au dépôt reste à vérifier.

**Impact.** Tentatives automatisées de connexion, réutilisation d'identifiants compromis et création abusive de comptes. La compromission d'un compte standard donne ensuite accès aux risques 1 et 2.

**Correctif prioritaire.** Limiter les tentatives par compte et par IP, avec temporisation progressive et stockage partagé si plusieurs instances sont utilisées. Unifier la politique de mot de passe entre inscription et réinitialisation, et activer une authentification multifacteur pour l'administration. Ajouter des tests de limitation et des alertes sur les échecs répétés. Remplacer aussi les mots de passe provisoires construits avec `new Random().toString()` par un parcours d'activation sans mot de passe utilisable.

## Vérifications et limites

Exécution locale de **9 tests : 0 échec, 0 erreur**. Six sondes temporaires vérifient les constats ci-dessus ; les deux tests existants de `SignupAuthorizationMvcTest` et le test `ControllerAuthorizationArchitectureTest` complètent la vérification. Le test d'architecture valide la présence d'annotations, pas les droits sur les objets ni les transitions métier.

Commande exécutée dans `app` :

```sh
mvn -o -q '-Dtest=SecurityAuditProbeTest,SecurityAuditServiceProbeTest,SignupAuthorizationMvcTest,ControllerAuthorizationArchitectureTest' test
```

Les sondes ont été retirées de l'arbre applicatif après exécution ; leurs sources et les rapports XML sont conservés temporairement dans `/tmp/adhesion-security-audit-probes/`. Le journal est dans `/tmp/adhesion-security-audit-tests.log`. Mockito et le serveur HTTP local ont nécessité une exécution hors du bac à sable. Aucun appel n'a été effectué vers une cible interne réelle ou la production.

Points positifs observés : BCrypt, jetons de réinitialisation hachés et à usage unique, révocation des sessions par version, renouvellement des jetons persistants, contrôle du propriétaire des commandes de boutique et vérification serveur du paiement de boutique.

Cet audit ciblé ne constitue pas un pentest exhaustif ni une validation du déploiement réel. Il n'inclut pas de scan actualisé des vulnérabilités des dépendances, de revue de l'historique Git pour les secrets, ni de vérification des sauvegardes ou des règles réseau de production.
