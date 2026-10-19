package fr.minenorth.phonepatch;

import com.mikasrevs.newphone.client.gui.PhoneApp;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Appelé par le téléphone patché (voir PatchLivraison) : application « Mon gang » de l'écran d'accueil.
 * Le mod MineNorth Portes (fr.minenorth.portes.client.GangClient.openFromPhone) est appelé par réflexion : s'il est absent,
 * l'icône n'est jamais ajoutée. Côté client uniquement ; aucune dépendance de compilation vers ce mod.
 */
public final class GangHook {
    private static boolean tried;
    private static Method open;

    private GangHook() {}

    private static synchronized Method open() {
        if (!tried) {
            tried = true;
            try {
                Class<?> c = Class.forName("fr.minenorth.portes.client.GangClient", false, GangHook.class.getClassLoader());
                open = c.getMethod("openFromPhone");
            } catch (Throwable t) {
                open = null;
            }
        }
        return open;
    }

    /** Ajoute l'application « Mon gang » aux applications par défaut de l'écran d'accueil si le mod Portes est présent. */
    public static List<PhoneApp> home(List<PhoneApp> base) {
        if (open() == null) return base;
        try {
            List<PhoneApp> l = new ArrayList<>(base);
            PhoneApp a = Enum.valueOf(PhoneApp.class, "GANGS");
            if (!l.contains(a)) l.add(a);
            return Collections.unmodifiableList(l);
        } catch (Throwable t) {
            return base;
        }
    }

    /** Clic sur l'icône : true = gérée ici (le téléphone n'ouvre pas son écran générique). */
    public static boolean open(PhoneApp app) {
        if (app == null || !"GANGS".equals(app.name())) return false;
        Method m = open();
        if (m != null) {
            try {
                m.invoke(null);
            } catch (Throwable t) {
                System.err.println("[MineNorth] Application Mon gang du téléphone : échec : " + t);
            }
        }
        return true;
    }
}
