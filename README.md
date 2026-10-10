# MineNorth Téléphone (`minenorthphone`) — pont MikasRevs Phone 2

Mod **serveur** (Forge 1.20.1) : les joueurs n'ont pas besoin du jar. Sans *MikasRevs Phone 2* (`newphone`), il ne fait rien.
Dépend de `minenorthapi` ; utilise `minenorth_map` si présent (repères).

Le téléphone a déjà ses appels d'urgence (police / EMS, catégories, tablette de répondant). Ce pont le branche sur les mods Police et Secours :

- **Rôles automatiques** : un policier **en service** devient répondant `police`, un secouriste en service devient `ems` (les deux s'il est les deux).
  À la fin du service (ou à la déconnexion) le rôle est retiré. Les rôles donnés à la main par un op (`/mphone role set …`) ne sont jamais touchés.
  La tablette de répondant (police / secours) est remise si besoin.
- **Alertes** : quand un joueur appelle depuis son téléphone, les policiers (urgence 17) ou secouristes (urgence 18) **en service** reçoivent un message
  dans le chat (catégorie, appelant, coordonnées, description) et un repère sur la carte, retiré quand l'appel est clos ou annulé.

- **Tablettes MineNorth** : le pont fournit les appels d'urgence en cours à MineNorth API (`MineNorth.calls()`). La tablette **police** a un onglet
  **ALERTES** (urgence 17) et la tablette **secours** les affiche dans son onglet ALERTES (urgence 18), avec ancienneté, lieu, distance, description et
  un bouton **GUIDER** (repère + guidage sur la carte). Elles se rechargent toutes seules toutes les 5 s.
- **Banque du téléphone sur EuroBank** : l'application Banque affiche le compte EuroBank du joueur (solde en €, montants rapides de 1 à 500 €) et fait les
  virements vers les autres joueurs (avec historique « Virement depuis le téléphone »). Le téléphone n'accepte normalement que Lightman's Currency ou
  Kuro's Economy : tant qu'aucun des deux n'est installé, le pont remplace au démarrage le fournisseur « mod manquant » par EuroBank
  (avec l'accord de l'auteur du téléphone). Un joueur sans compte voit « compte indisponible ».

## Configuration
`<monde>/serverconfig/minenorthphone-server.toml` : `actif`, `verificationSecondes`, `rolesAutomatiques`, `donnerTablette`, `alerteChat`, `alerteCarte`, `brancherEuroBank`.

## Compatibilité
Le pont lit les classes internes du téléphone par réflexion (aucune dépendance de compilation) : si une mise à jour du téléphone les renomme, il se désactive
avec un message dans le log (`[Telephone] … pont désactivé`) au lieu de faire planter le serveur. Écrit pour la version 0.9.4 bêta (les classes d'urgence sont identiques en 0.9.3).

## Compilation
Compiler `minenorth-api` d'abord (jar dans `../minenorth-api/build/libs`), déposer le jar `MRPhone 2 0.9.4 beta.jar` dans `libs/` (non versionné), puis `./gradlew build` → `build/libs/minenorth-phone-1.0.0.jar`.

## Licence
Tous droits réservés - MineNorthRP. Voir `LICENSE`.

## Noms RP dans le téléphone (jar patché)
Le téléphone affiche partout le pseudo Minecraft (`GameProfile.getName()`, 42 appels côté serveur : messages, contacts, appels, annonces, appels d'urgence).
`tools/patch/` produit un jar patché (avec l'accord de l'auteur du téléphone) où ces appels deviennent `fr.minenorth.phonepatch.NameHook.name(profile)` :
nom RP « Prénom Nom » (MineNorth API) et, sans identité, pseudo Minecraft. Le hook lit MineNorth et Forge par réflexion (le téléphone patché marche aussi sans MineNorth)
et ne touche que les classes serveur (`server/`, `MarketplaceManager`), jamais le client.
```
javac -cp authlib.jar -d out tools/patch/fr/minenorth/phonepatch/NameHook.java
javac -cp asm-9.6.jar -d out tools/patch/PatchPhone.java
java -cp "asm-9.6.jar;out" PatchPhone "MRPhone 2 0.9.4 beta.jar" "MRPhone 2 0.9.4 beta (patché).jar" out/fr/minenorth/phonepatch/NameHook.class
```
À refaire à chaque nouvelle version du téléphone. Les noms déjà mémorisés par le téléphone pour des joueurs hors ligne se mettent à jour à leur prochaine connexion.
