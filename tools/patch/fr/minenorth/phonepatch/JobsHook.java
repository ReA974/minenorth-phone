package fr.minenorth.phonepatch;

import com.mikasrevs.newphone.client.gui.PhoneApp;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Appelé par le téléphone patché (voir PatchLivraison) : applications de métier POLICE, SECOURS, MAIRIE, ENTREPRISE de l'écran d'accueil.
 * Chaque application est fournie par son mod (classe client {@code fr.minenorth.<mod>.client.PhoneAppClient}, appelée par réflexion) :
 * {@code visible()} (état envoyé par le serveur : le joueur a le métier) et {@code openFromPhone()} (demande l'écran au serveur, qui revérifie).
 * Mod absent = icône jamais affichée. Côté client uniquement ; aucune dépendance de compilation vers ces mods.
 */
public final class JobsHook {
    /** Constante de l'énumération PhoneApp, classe client du mod. */
    private static final String[][] JOBS = {
            {"POLICE", "fr.minenorth.police.client.PhoneAppClient"},
            {"SECOURS", "fr.minenorth.secours.client.PhoneAppClient"},
            {"MAIRIE", "fr.minenorth.etat.client.PhoneAppClient"},
            {"ENTREPRISE", "fr.minenorth.entreprises.client.PhoneAppClient"},
    };
    private static final Method[] VISIBLE = new Method[JOBS.length];
    private static final Method[] OPEN = new Method[JOBS.length];
    private static final boolean[] TRIED = new boolean[JOBS.length];

    private JobsHook() {}

    private static synchronized boolean load(int i) {
        if (!TRIED[i]) {
            TRIED[i] = true;
            try {
                Class<?> c = Class.forName(JOBS[i][1], false, JobsHook.class.getClassLoader());
                VISIBLE[i] = c.getMethod("visible");
                OPEN[i] = c.getMethod("openFromPhone");
            } catch (Throwable t) {
                VISIBLE[i] = null;
                OPEN[i] = null;
            }
        }
        return OPEN[i] != null;
    }

    private static int index(PhoneApp app) {
        if (app == null) return -1;
        String n = app.name();
        for (int i = 0; i < JOBS.length; i++) if (JOBS[i][0].equals(n)) return i;
        return -1;
    }

    /**
     * Appelé par PhoneAppLayoutStore.homeSlots() sur la copie de l'agencement : place chaque application de métier visible dans la première
     * case libre (ou à la fin). Rien n'est enregistré dans le fichier d'agencement du joueur : sans métier, l'icône disparaît d'elle-même.
     */
    public static List<PhoneApp> fill(List<PhoneApp> slots) {
        try {
            for (int i = 0; i < JOBS.length; i++) {
                if (!load(i)) continue;
                PhoneApp app = Enum.valueOf(PhoneApp.class, JOBS[i][0]);
                if (slots.contains(app) || !Boolean.TRUE.equals(VISIBLE[i].invoke(null))) continue;
                int free = slots.indexOf(null);
                if (free >= 0) slots.set(free, app);
                else slots.add(app);
            }
        } catch (Throwable t) {
            // jamais bloquant pour l'écran d'accueil
        }
        return slots;
    }

    /** Clic sur une icône : true = application de métier gérée ici (le téléphone n'ouvre pas son écran générique). */
    public static boolean open(PhoneApp app) {
        int i = index(app);
        if (i < 0) return false;
        if (load(i)) {
            try {
                OPEN[i].invoke(null);
            } catch (Throwable t) {
                System.err.println("[MineNorth] Application " + app.displayName() + " du téléphone : échec : " + t);
            }
        }
        return true;
    }
}
