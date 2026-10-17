package fr.minenorth.phonepatch;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Appelé par le téléphone patché (voir PatchCalls) : redirige un appel vers les contacts « Police » / « Pompiers » vers le policier / secouriste
 * de service qui répond sur son téléphone, et marque ces contacts « en ligne ». Passe par le mod pont MineNorth Téléphone (fr.minenorth.phone.CallRouter)
 * par réflexion : sans lui, ou en cas d'erreur, ne change rien.
 */
public final class CallHook {
    private static boolean tried;
    private static Method redirect, online;

    private CallHook() {}

    private static synchronized void init() {
        if (tried) return;
        tried = true;
        try {
            Class<?> c = Class.forName("fr.minenorth.phone.CallRouter", false, CallHook.class.getClassLoader());
            redirect = c.getMethod("redirect", UUID.class, UUID.class);
            online = c.getMethod("online", boolean.class, UUID.class);
        } catch (Throwable t) {
            redirect = null;
            online = null;
        }
    }

    public static UUID redirect(UUID caller, UUID target) {
        try {
            init();
            if (redirect == null) return target;
            Object r = redirect.invoke(null, caller, target);
            return r instanceof UUID u ? u : target;
        } catch (Throwable t) {
            return target;
        }
    }

    public static boolean online(boolean isOnline, UUID id) {
        try {
            init();
            if (online == null) return isOnline;
            return (boolean) online.invoke(null, isOnline, id);
        } catch (Throwable t) {
            return isOnline;
        }
    }
}
