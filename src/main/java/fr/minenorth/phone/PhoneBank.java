package fr.minenorth.phone;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Field;

/**
 * Branche l'application Banque de MikasRevs Phone 2 sur MineNorth EuroBank.
 * Le téléphone choisit son fournisseur de monnaie une seule fois (Lightman's Currency ou Kuro's Economy, sinon « mod manquant ») et le garde dans
 * un champ privé figé de PhoneCurrencyManager. Quand aucun des deux n'est installé, on remplace ce champ par {@link EuroBankIntegration}
 * (auteur du téléphone consentant). Si le téléphone change cette partie dans une mise à jour, rien n'est modifié et un message est écrit.
 */
final class PhoneBank {
    private static final Logger LOG = LogUtils.getLogger();

    private PhoneBank() {}

    static void install() {
        if (!ModList.get().isLoaded("newphone")) return;
        try {
            Class<?> manager = Class.forName("com.mikasrevs.newphone.integration.currency.PhoneCurrencyManager");   // exécute son initialisation
            Field f = manager.getDeclaredField("INTEGRATION");
            f.setAccessible(true);
            Object current = f.get(null);
            if (current != null && !current.getClass().getSimpleName().contains("Unavailable")) {
                LOG.info("[Telephone] Banque du téléphone : un fournisseur de monnaie est déjà installé ({}), EuroBank non branché.",
                        current.getClass().getSimpleName());
                return;
            }
            Object ours = new EuroBankIntegration();
            Field uf = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            uf.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) uf.get(null);
            unsafe.putObjectVolatile(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), ours);
            LOG.info("[Telephone] Banque du téléphone branchée sur MineNorth EuroBank.");
        } catch (Throwable t) {
            LOG.warn("[Telephone] Impossible de brancher EuroBank sur la banque du téléphone (version du téléphone incompatible ?) : {}", t.toString());
        }
    }
}
