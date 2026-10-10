package fr.minenorth.phone;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;

/** Pont (par réflexion) vers l'API serveur du mod carte minenorth_map. Sans la carte, tous les appels sont sans effet. */
public final class MapBridge {
    private static boolean tried;
    private static Method set, remove;

    private MapBridge() {}

    private static boolean init() {
        if (!tried) {
            tried = true;
            try {
                if (ModList.get().isLoaded("minenorth_map")) {
                    Class<?> api = Class.forName("fr.minenorth.map.api.MapApi");
                    set = api.getMethod("setMarker", ServerPlayer.class, String.class, String.class, int.class, int.class, int.class, int.class, boolean.class);
                    remove = api.getMethod("removeMarker", ServerPlayer.class, String.class);
                }
            } catch (Throwable t) {
                set = remove = null;
            }
        }
        return set != null;
    }

    public static void set(ServerPlayer p, String name, String dim, int x, int y, int z, int color) {
        if (!init()) return;
        try { set.invoke(null, p, name, dim, x, y, z, color, false); } catch (Throwable ignored) {}
    }

    public static void remove(ServerPlayer p, String name) {
        if (!init()) return;
        try { remove.invoke(null, p, name); } catch (Throwable ignored) {}
    }
}
