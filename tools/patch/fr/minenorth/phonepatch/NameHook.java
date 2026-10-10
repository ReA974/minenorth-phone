package fr.minenorth.phonepatch;

import com.mojang.authlib.GameProfile;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Remplace GameProfile.getName() dans les classes serveur de MikasRevs Phone 2 (voir PatchPhone) : renvoie le nom RP du joueur
 * (« Prénom Nom », MineNorth API) et, à défaut d'identité, son pseudo Minecraft. Ne dépend que du JDK et d'authlib : lit MineNorth et Forge par
 * réflexion, donc le téléphone patché fonctionne aussi sans MineNorth (pseudo Minecraft) et côté client (pas de serveur : pseudo).
 */
public final class NameHook {
    private static boolean tried;
    private static Method serverGetter, displayName;

    private NameHook() {}

    private static synchronized void init() {
        if (tried) return;
        tried = true;
        try {
            ClassLoader cl = NameHook.class.getClassLoader();
            serverGetter = Class.forName("net.minecraftforge.server.ServerLifecycleHooks", false, cl).getMethod("getCurrentServer");
            for (Method m : Class.forName("fr.minenorth.api.MineNorth", false, cl).getMethods()) {
                if (m.getName().equals("displayName") && m.getParameterCount() == 2 && m.getParameterTypes()[1] == UUID.class) displayName = m;
            }
        } catch (Throwable t) {
            displayName = null;
        }
    }

    public static String name(GameProfile profile) {
        String mc = profile.getName();
        UUID id = profile.getId();
        if (id == null) return mc;
        try {
            init();
            if (displayName == null) return mc;
            Object server = serverGetter.invoke(null);
            if (server == null) return mc;
            Object n = displayName.invoke(null, server, id);
            return n instanceof String s && !s.isBlank() && !s.equals("Inconnu") ? s : mc;
        } catch (Throwable t) {
            return mc;
        }
    }
}
