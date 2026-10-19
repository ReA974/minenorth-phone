package fr.minenorth.phonepatch;

import com.mikasrevs.newphone.client.gui.PhoneApp;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Appelé par le téléphone patché (voir PatchLivraison) : application « Livraison » (commande de repas) de l'écran d'accueil.
 * Le mod MineNorth Livraison (fr.minenorth.livraison.client.ClientHooks.openFromPhone) est appelé par réflexion : s'il est absent,
 * l'icône n'est jamais ajoutée au téléphone. Côté client uniquement ; aucune dépendance de compilation vers ce mod.
 */
public final class LivraisonHook {
    private static boolean tried;
    private static Method open;

    private LivraisonHook() {}

    private static synchronized Method open() {
        if (!tried) {
            tried = true;
            try {
                Class<?> c = Class.forName("fr.minenorth.livraison.client.ClientHooks", false, LivraisonHook.class.getClassLoader());
                open = c.getMethod("openFromPhone");
            } catch (Throwable t) {
                open = null;
            }
        }
        return open;
    }

    /** Applications proposées par défaut sur l'écran d'accueil (PhoneAppLayoutStore.DEFAULT_HOME) : + Livraison si le mod est présent. */
    public static List<PhoneApp> home(List<PhoneApp> base) {
        List<PhoneApp> result = base;
        if (open() != null) {
            try {
                List<PhoneApp> l = new ArrayList<>(base);
                PhoneApp a = Enum.valueOf(PhoneApp.class, "LIVRAISON");
                if (!l.contains(a)) l.add(a);
                result = Collections.unmodifiableList(l);
            } catch (Throwable t) {
                result = base;
            }
        }
        // Application « Mon gang » (mod Portes) : même mécanisme, ajoutée seulement si le mod est présent.
        return GangHook.home(result);
    }

    /** Clic sur l'icône (ZPhoneClient.openSharedApp) : true = l'application est gérée ici (le téléphone n'ouvre pas son écran générique). */
    public static boolean open(PhoneApp app) {
        if (app != null && "GANGS".equals(app.name())) return GangHook.open(app);
        if (JobsHook.open(app)) return true;   // applications de métier (Police, Secours, Mairie, Entreprise)
        if (app == null || !"LIVRAISON".equals(app.name())) return false;
        Method m = open();
        if (m != null) {
            try {
                m.invoke(null);
            } catch (Throwable t) {
                System.err.println("[MineNorth] Application Livraison du téléphone : échec : " + t);
            }
        }
        return true;
    }
}
