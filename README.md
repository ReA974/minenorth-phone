# MineNorth Téléphone (`minenorthphone`) — pont MikasRevs Phone 2

Mod **serveur** (Forge 1.20.1) : les joueurs n'ont pas besoin du jar. Sans *MikasRevs Phone 2* (`newphone`), il ne fait rien.
Dépend de `minenorthapi` ; utilise `minenorth_map` si présent (repères).

Le téléphone a déjà ses appels d'urgence (police / EMS, catégories, tablette de répondant). Ce pont le branche sur les mods Police et Secours :

- **Rôles automatiques** : un policier **en service** devient répondant `police`, un secouriste en service devient `ems` (les deux s'il est les deux).
  À la fin du service (ou à la déconnexion) le rôle est retiré. Les rôles donnés à la main par un op (`/mphone role set …`) ne sont jamais touchés.
  La tablette de répondant du téléphone n'est plus donnée par défaut (`donnerTablette = false`).
- **Alertes** : quand un joueur appelle depuis son téléphone, les policiers (urgence 17) ou secouristes (urgence 18) **en service** reçoivent un message
  dans le chat (catégorie, appelant, coordonnées, description) et un repère sur la carte, retiré quand l'appel est clos ou annulé.

- **Tablettes MineNorth** : le pont fournit les appels d'urgence en cours à MineNorth API (`MineNorth.calls()`). La tablette **police** a un onglet
  **ALERTES** (urgence 17) et la tablette **secours** les affiche dans son onglet ALERTES (urgence 18), avec ancienneté, lieu, distance, description et
  un bouton **GUIDER** (repère + guidage sur la carte). Elles se rechargent toutes seules toutes les 5 s.
- **Banque du téléphone sur EuroBank** : l'application Banque affiche le compte EuroBank du joueur (solde en €, montants rapides de 1 à 500 €) et fait les
  virements vers les autres joueurs (avec historique « Virement depuis le téléphone »). Le téléphone n'accepte normalement que Lightman's Currency ou
  Kuro's Economy : tant qu'aucun des deux n'est installé, le pont remplace au démarrage le fournisseur « mod manquant » par EuroBank
  (avec l'accord de l'auteur du téléphone). Un joueur sans compte voit « compte indisponible ».

## Numéros 17 / 18 dans le téléphone
- Chaque joueur reçoit par défaut deux **contacts « Police (17) » et « Pompiers (18) »** (ajoutés à la connexion ; contacts virtuels à UUID fixes).
- Un appel vers l'un d'eux **sonne sur le téléphone personnel** d'un policier (resp. secouriste) **en service** qui a activé le bouton **« TÉLÉPHONE 17 / 18 : OUI »** de sa tablette (police ou secours ; même bouton pour arrêter ; remis à zéro à la déconnexion et à la fin du service). Le premier répondant libre (pas déjà en communication) est appelé. Personne de disponible : message « Personne ne peut répondre… » et l'application Urgences reste utilisable.
- Nécessite le **téléphone patché** (`tools/patch`, `PatchCalls` : redirection dans `PhoneServerService.startCall` et drapeau « en ligne » des contacts virtuels dans `addPlayerEntry`), jar patché installé serveur ET clients. Le pont (`CallRouter`) fait le reste par réflexion. Limite connue : l'historique d'appels de l'appelant affiche le nom du répondant qui a décroché.
- Réglages : `[numerosUrgence]` contactsParDefaut, numeroPolice, numeroPompiers.

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
javac --release 17 -cp authlib.jar -d out tools/patch/fr/minenorth/phonepatch/NameHook.java
javac --release 17 -cp asm-9.6.jar -d out tools/patch/PatchPhone.java
java -cp "asm-9.6.jar;out" PatchPhone "MRPhone 2 0.9.4 beta.jar" "MRPhone 2 0.9.4 beta (patché).jar" out/fr/minenorth/phonepatch/NameHook.class
```
Le même patch ajoute, côté **client**, deux onglets **Police** / **Pompiers** en bas de l'écran d'accueil de l'application Urgences du téléphone (`tools/patch/PatchTabs.java`) :
visibles seulement pour un joueur reconnu répondant (en service), ils ouvrent l'écran de la tablette de répondant sans l'objet tablette. Le jar patché doit donc aussi être installé chez les joueurs.
À refaire à chaque nouvelle version du téléphone. Les noms déjà mémorisés par le téléphone pour des joueurs hors ligne se mettent à jour à leur prochaine connexion.

## Application « Livraison » dans le téléphone (jar patché)
`tools/patch/PatchLivraison.java` ajoute une application **Livraison** (icône orange scooter + sac, `make_livraison_icon.py` → `livraison.png` 64×64) à l'écran d'accueil du téléphone :
nouvelle constante `PhoneApp.LIVRAISON` (dernier ordinal), icône raster, `PhoneAppLayoutStore.DEFAULT_HOME` via `LivraisonHook.home`, clic via `ZPhoneClient.openSharedApp` → `LivraisonHook.open`.
Le hook appelle par réflexion `fr.minenorth.livraison.client.ClientHooks.openFromPhone` (mod MineNorth Livraison, qui envoie `/livraison commander`) : sans ce mod, l'icône n'est pas ajoutée. Les joueurs qui ont déjà un agencement enregistré la reçoivent en dernière position.
Commande complète (ajouter `CallHook.class`, `LivraisonHook.class` — compilé avec le jar d'origine dans le classpath — et `livraison.png` après `NameHook.class`) :
```
java -cp "asm-9.6.jar;out" PatchPhone "MRPhone 2 0.9.4 beta.jar" "patched.jar" out/fr/minenorth/phonepatch/NameHook.class out/fr/minenorth/phonepatch/CallHook.class out/fr/minenorth/phonepatch/LivraisonHook.class tools/patch/livraison.png
```

## Dépannage : numéro vide / aucun contact
Si le téléphone affiche « Mon numéro # » vide et aucun contact, le serveur n'a pas pu envoyer l'état du téléphone au joueur. Le pont renvoie cet état 5 s après chaque connexion et à la demande avec **/mphonefix [joueur]** (OP) ; en cas d'échec, la vraie erreur est écrite dans le log du serveur (`[Telephone] L'envoi de l'état du téléphone … a échoué`) et dans le chat des opérateurs.

**Important : toujours compiler avec --release 17.** Avec un JDK plus récent (Java 27 = classe version 71), NameHook.class ne se charge pas sur le serveur (« Unsupported class file major version 71 ») et le téléphone n'envoie plus son état : numéro vide, aucun contact.

## Application « Mon gang » (jar patché)
`PatchLivraison` ajoute désormais deux applications (tableau `APPS`) : **Livraison** puis **Mon gang** (`GANGS`, icône violette `gangs.png`, `make_gangs_icon.py`), en ordinaux successifs. `GangHook` (chaîné depuis `LivraisonHook.home/open`) appelle par réflexion `fr.minenorth.portes.client.GangClient.openFromPhone` (mod MineNorth Portes) : sans ce mod, l'icône n'est pas ajoutée. Commande : celle ci-dessus plus `GangHook.class gangs.png` en fin de ligne (compiler les hooks avec le jar d'origine `libs/` dans le classpath). Vérifié par chargement de toutes les classes avec `-Xverify:all` (0 VerifyError ; GANGS = ordinal 34).
