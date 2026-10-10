package fr.minenorth.phone;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Seul point de contact avec MikasRevs Phone 2 (modid « newphone »), par réflexion : aucune dépendance de compilation, et si une mise à jour
 * du téléphone renomme ses classes internes, le pont s'arrête (message dans le log) au lieu de faire planter le serveur.
 * Thread serveur uniquement.
 */
public final class PhoneBridge {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String PKG = "com.mikasrevs.newphone.";

    /** Un appel d'urgence du téléphone. service = « POLICE » ou « EMS », category = nom de la catégorie (ex. ARMED_THREAT). */
    public record Call(long id, String service, String category, String caller, String dim, int x, int y, int z, String description, long createdAt) {}

    private static boolean tried, ok;
    private static Class<?> serviceEnum;
    private static Method rolesGet, rolesServices, rolesSet, managerGet, revalidate;
    private static Field dispatchesField;
    private static Field fId, fCategory, fCaller, fDim, fX, fY, fZ, fDesc, fStatus, fCreated;
    private static Method categoryService, statusActive;
    private static Field policeTablet, emsTablet;

    private PhoneBridge() {}

    public static boolean available() {
        if (tried) return ok;
        tried = true;
        try {
            if (!ModList.get().isLoaded("newphone")) return false;
            serviceEnum = Class.forName(PKG + "emergency.EmergencyService");
            Class<?> roles = Class.forName(PKG + "server.data.EmergencyRoleSavedData");
            rolesGet = roles.getMethod("get", MinecraftServer.class);
            rolesServices = roles.getMethod("services", UUID.class);
            rolesSet = roles.getMethod("setServices", UUID.class, EnumSet.class);
            Class<?> manager = Class.forName(PKG + "server.emergency.EmergencyDispatchManager");
            managerGet = manager.getMethod("get", MinecraftServer.class);
            revalidate = manager.getMethod("revalidateRespondersAndSync");
            dispatchesField = manager.getDeclaredField("dispatches");
            dispatchesField.setAccessible(true);
            Class<?> d = Class.forName(PKG + "server.emergency.EmergencyDispatchManager$EmergencyDispatch");
            fId = field(d, "id");
            fCategory = field(d, "category");
            fCaller = field(d, "callerName");
            fDim = field(d, "dimension");
            fX = field(d, "x");
            fY = field(d, "y");
            fZ = field(d, "z");
            fDesc = field(d, "description");
            fStatus = field(d, "status");
            fCreated = field(d, "createdAt");
            categoryService = Class.forName(PKG + "emergency.EmergencyCategory").getMethod("service");
            statusActive = Class.forName(PKG + "emergency.EmergencyStatus").getMethod("active");
            Class<?> main = Class.forName(PKG + "NewPhone");
            policeTablet = main.getField("POLICE_TABLET");
            emsTablet = main.getField("EMS_TABLET");
            ok = true;
            LOG.info("[Telephone] MikasRevs Phone 2 détecté : pont police / secours actif.");
        } catch (Throwable t) {
            ok = false;
            LOG.warn("[Telephone] MikasRevs Phone 2 présent mais illisible (version incompatible ?) : pont désactivé. {}", t.toString());
        }
        return ok;
    }

    private static Field field(Class<?> c, String name) throws NoSuchFieldException {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    // ------------------------------------------------------------------ rôles de répondant

    /** Services (POLICE, EMS) que le téléphone reconnaît actuellement à ce joueur. */
    @SuppressWarnings("rawtypes")
    public static Set<String> services(MinecraftServer s, UUID id) {
        Set<String> out = new LinkedHashSet<>();
        if (!available()) return out;
        try {
            Object roles = rolesGet.invoke(null, s);
            for (Object e : (EnumSet) rolesServices.invoke(roles, id)) out.add(((Enum) e).name());
        } catch (Throwable t) {
            LOG.warn("[Telephone] Lecture des rôles impossible : {}", t.toString());
        }
        return out;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void setServices(MinecraftServer s, UUID id, Set<String> names) {
        if (!available()) return;
        try {
            EnumSet set = EnumSet.noneOf((Class<Enum>) serviceEnum);
            for (String n : names) set.add(Enum.valueOf((Class<Enum>) serviceEnum, n));
            rolesSet.invoke(rolesGet.invoke(null, s), id, set);
        } catch (Throwable t) {
            LOG.warn("[Telephone] Écriture des rôles impossible : {}", t.toString());
        }
    }

    /** Fait recalculer au téléphone la liste de ses répondants et renvoie les écrans. */
    public static void revalidate(MinecraftServer s) {
        if (!available()) return;
        try {
            revalidate.invoke(managerGet.invoke(null, s));
        } catch (Throwable t) {
            LOG.warn("[Telephone] Mise à jour des répondants impossible : {}", t.toString());
        }
    }

    /** Donne la tablette de répondant du service (POLICE / EMS) si le joueur n'en a pas déjà une. */
    @SuppressWarnings("unchecked")
    public static void giveTablet(ServerPlayer p, String service) {
        if (!available()) return;
        try {
            Field f = service.equals("POLICE") ? policeTablet : emsTablet;
            Item item = ((RegistryObject<Item>) f.get(null)).get();
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).is(item)) return;
            }
            ItemStack tablet = new ItemStack(item);
            if (!p.getInventory().add(tablet)) p.drop(tablet, false);
        } catch (Throwable t) {
            LOG.warn("[Telephone] Remise de la tablette impossible : {}", t.toString());
        }
    }

    // ------------------------------------------------------------------ appels

    /** Appels d'urgence en cours (en attente, acceptés, en route ou sur place). */
    @SuppressWarnings("unchecked")
    public static List<Call> activeCalls(MinecraftServer s) {
        List<Call> out = new ArrayList<>();
        if (!available()) return out;
        try {
            Object manager = managerGet.invoke(null, s);
            Map<Long, Object> map;
            synchronized (manager) {   // le téléphone synchronise ses méthodes sur le gestionnaire
                map = new java.util.HashMap<>((Map<Long, Object>) dispatchesField.get(manager));
            }
            for (Object d : map.values()) {
                if (!(Boolean) statusActive.invoke(fStatus.get(d))) continue;
                Object cat = fCategory.get(d);
                out.add(new Call(fId.getLong(d), ((Enum<?>) categoryService.invoke(cat)).name(), ((Enum<?>) cat).name(),
                        String.valueOf(fCaller.get(d)), String.valueOf(fDim.get(d)), fX.getInt(d), fY.getInt(d), fZ.getInt(d),
                        fDesc.get(d) == null ? "" : String.valueOf(fDesc.get(d)), fCreated.getLong(d)));
            }
        } catch (Throwable t) {
            LOG.warn("[Telephone] Lecture des appels impossible : {}", t.toString());
        }
        return out;
    }
}
