# MineNorth Téléphone (`minenorthphone`) — pont MikasRevs Phone 2

Mod **serveur** (Forge 1.20.1) : les joueurs n'ont pas besoin du jar. Sans *MikasRevs Phone 2* (`newphone`), il ne fait rien.
Dépend de `minenorthapi` ; utilise `minenorth_map` si présent (repères).

Le téléphone a déjà ses appels d'urgence (police / EMS, catégories, tablette de répondant). Ce pont le branche sur les mods Police et Secours :

- **Rôles automatiques** : un policier **en service** devient répondant `police`, un secouriste en service devient `ems` (les deux s'il est les deux).
  À la fin du service (ou à la déconnexion) le rôle est retiré. Les rôles donnés à la main par un op (`/mphone role set …`) ne sont jamais touchés.
  La tablette de répondant (police / secours) est remise si besoin.
- **Alertes** : quand un joueur appelle depuis son téléphone, les policiers (urgence 17) ou secouristes (urgence 18) **en service** reçoivent un message
  dans le chat (catégorie, appelant, coordonnées, description) et un repère sur la carte, retiré quand l'appel est clos ou annulé.

## Configuration
`<monde>/serverconfig/minenorthphone-server.toml` : `actif`, `verificationSecondes`, `rolesAutomatiques`, `donnerTablette`, `alerteChat`, `alerteCarte`.

## Compatibilité
Le pont lit les classes internes du téléphone par réflexion (aucune dépendance de compilation) : si une mise à jour du téléphone les renomme, il se désactive
avec un message dans le log (`[Telephone] … pont désactivé`) au lieu de faire planter le serveur. Testé sur la version 0.9.3 bêta.

## Compilation
Compiler `minenorth-api` d'abord (jar dans `../minenorth-api/build/libs`), puis `./gradlew build` → `build/libs/minenorth-phone-1.0.0.jar`.

## Licence
Tous droits réservés - MineNorthRP. Voir `LICENSE`.
