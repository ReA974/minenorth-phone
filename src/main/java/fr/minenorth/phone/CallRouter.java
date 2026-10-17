package fr.minenorth.phone;

import com.mojang.logging.LogUtils;
import fr.minenorth.api.MineNorth;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Numéros d'urgence dans le téléphone. Chaque joueur reçoit par défaut deux contacts « Police » (17) et « Pompiers » (18) ; un appel vers l'un d'eux
 * sonne sur le téléphone PERSONNEL d'un policier / secouriste EN SERVICE qui a activé /urgencetel on. Ces contacts sont virtuels (UUID fixes) :
 * le téléphone patché (tools/patch, PatchCalls) appelle {@link #redirect} et {@link #online} par réflexion. Tout passe par réflexion sur le téléphone.
 */
public final class CallRouter {
    private static final Logger LOG = LogUtils.getLogger();
    public static final UUID POLICE_ID = UUID.nameUUIDFromBytes("minenorth:service:police".getBytes(StandardCharsets.UTF_8));
    public static final UUID EMS_ID = UUID.nameUUIDFromBytes("minenorth:service:ems".getBytes(StandardCharsets.UTF_8));
    private static final String PKG = "com.mikasrevs.newphone.";

    /** Policiers / secouristes qui répondent aux appels 17 / 18 sur leur téléphone (mémoire : remis à zéro au redémarrage et à la déconnexion). */
    private static final Set<UUID> OPTED = ConcurrentHashMap.newKeySet();
    private static Field callsField;

    private static boolean enabled() {
        try {
            return PhoneConfig.ENABLED.get() && PhoneConfig.DEFAULT_CONTACTS.get() && ModList.get().isLoaded("newphone");
        } catch (IllegalStateException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ appelés par le téléphone patché

    /** Cible réelle d'un appel : le répondant de service pour les contacts Police / Pompiers, sinon la cible inchangée. */
    public static UUID redirect(UUID caller, UUID target) {
        boolean police = POLICE_ID.equals(target);
        if (!police && !EMS_ID.equals(target)) return target;
        if (!enabled()) return target;
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null) return target;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            UUID id = p.getUUID();
            if (id.equals(caller) || !OPTED.contains(id) || !onDuty(s, id, police) || inCall(id)) continue;
            return id;
        }
        ServerPlayer c = s.getPlayerList().getPlayer(caller);
        if (c != null) c.sendSystemMessage(Component.literal("§c[" + (police ? "Police" : "Pompiers") + "] Personne ne peut répondre à votre appel pour le moment. "
                + "Utilisez l'application Urgences du téléphone."));
        return target;
    }

    /** Les contacts Police / Pompiers sont toujours « en ligne » (le bouton d'appel reste actif ; sans répondant un message l'explique). */
    public static boolean online(boolean online, UUID id) {
        return online || ((POLICE_ID.equals(id) || EMS_ID.equals(id)) && enabled());
    }

    // ------------------------------------------------------------------ service

    private static boolean onDuty(MinecraftServer s, UUID id, boolean police) {
        return police ? MineNorth.police().onDuty(s, id) : MineNorth.secours().onDuty(s, id);
    }

    private static boolean inCall(UUID id) {
        try {
            if (callsField == null) {
                callsField = Class.forName(PKG + "server.PhoneServerService").getDeclaredField("CALLS");
                callsField.setAccessible(true);
            }
            return ((Map<?, ?>) callsField.get(null)).containsKey(id);
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    /** Donne au téléphone les numéros 17 / 18 des contacts virtuels (après chargement des données du téléphone). */
    static void seedNumbers(MinecraftServer s) {
        try {
            Class<?> nd = Class.forName(PKG + "server.data.PhoneNumberSavedData");
            Object data = nd.getMethod("get", MinecraftServer.class).invoke(null, s);
            Field byPlayer = nd.getDeclaredField("numberByPlayer"), byNumber = nd.getDeclaredField("playerByNumber");
            byPlayer.setAccessible(true);
            byNumber.setAccessible(true);
            @SuppressWarnings("unchecked") Map<UUID, String> m1 = (Map<UUID, String>) byPlayer.get(data);
            @SuppressWarnings("unchecked") Map<String, UUID> m2 = (Map<String, UUID>) byNumber.get(data);
            synchronized (data) {
                for (Object[] e : new Object[][]{{POLICE_ID, PhoneConfig.POLICE_NUMBER.get()}, {EMS_ID, PhoneConfig.EMS_NUMBER.get()}}) {
                    UUID id = (UUID) e[0];
                    String number = (String) e[1];
                    UUID owner = m2.get(number);
                    if (owner != null && !owner.equals(id)) { LOG.warn("[Telephone] Le numéro {} est déjà attribué à un joueur : contact d'urgence non créé.", number); continue; }
                    m1.put(id, number);
                    m2.put(number, id);
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            LOG.warn("[Telephone] Numéros d'urgence du téléphone non posés : {}", e.toString());
        }
    }

    /** Ajoute les contacts Police / Pompiers à ce joueur s'il ne les a pas. */
    static void ensureContacts(ServerPlayer p) {
        try {
            Class<?> cd = Class.forName(PKG + "server.data.PhoneCommunicationSavedData");
            Object comm = cd.getMethod("get", MinecraftServer.class).invoke(null, p.server);
            Object snap = cd.getMethod("copySnapshot").invoke(comm);
            Class<?> sc = snap.getClass();
            @SuppressWarnings("unchecked") Map<String, List<String>> contacts = (Map<String, List<String>>) sc.getField("contacts").get(snap);
            @SuppressWarnings("unchecked") Map<String, String> names = (Map<String, String>) sc.getField("knownNames").get(snap);
            String me = p.getUUID().toString();
            List<String> list = new ArrayList<>(contacts.getOrDefault(me, List.of()));
            boolean changed = false;
            for (Object[] e : new Object[][]{{POLICE_ID, "Police (" + PhoneConfig.POLICE_NUMBER.get() + ")"}, {EMS_ID, "Pompiers (" + PhoneConfig.EMS_NUMBER.get() + ")"}}) {
                String id = ((UUID) e[0]).toString();
                if (!list.contains(id)) { list.add(id); changed = true; }
                if (!e[1].equals(names.get(id))) { names.put(id, (String) e[1]); changed = true; }
            }
            if (!changed) return;
            contacts.put(me, list);
            Method replace = cd.getMethod("replace", sc);
            replace.invoke(comm, snap);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            LOG.warn("[Telephone] Contacts d'urgence non ajoutés pour {} : {}", p.getGameProfile().getName(), e.toString());
        }
    }

    // ------------------------------------------------------------------ évènements

    @SubscribeEvent
    public void onStarted(ServerStartedEvent e) {
        if (enabled()) seedNumbers(e.getServer());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!enabled() || !(e.getEntity() instanceof ServerPlayer p)) return;
        seedNumbers(p.server);
        ensureContacts(p);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        OPTED.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("urgencetel")
                .then(Commands.literal("on").executes(c -> toggle(c.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("off").executes(c -> toggle(c.getSource().getPlayerOrException(), false)))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    p.sendSystemMessage(Component.literal("§b[Téléphone] §7Appels 17 / 18 sur votre téléphone : " + (OPTED.contains(p.getUUID()) ? "§aactivés" : "§cdésactivés")
                            + "§7. /urgencetel on ou /urgencetel off."));
                    return 1;
                }));
    }

    private static int toggle(ServerPlayer p, boolean on) {
        MinecraftServer s = p.server;
        boolean police = MineNorth.police().onDuty(s, p.getUUID()), ems = MineNorth.secours().onDuty(s, p.getUUID());
        if (on && !police && !ems) {
            p.sendSystemMessage(Component.literal("§c[Téléphone] Prenez d'abord votre service (police ou secours)."));
            return 0;
        }
        if (on) OPTED.add(p.getUUID()); else OPTED.remove(p.getUUID());
        p.sendSystemMessage(Component.literal(on ? "§a[Téléphone] Les appels au " + (police ? PhoneConfig.POLICE_NUMBER.get() : "") + (police && ems ? " et " : "")
                + (ems ? PhoneConfig.EMS_NUMBER.get() : "") + " sonneront sur votre téléphone tant que vous êtes en service."
                : "§7[Téléphone] Vous ne recevez plus les appels 17 / 18 sur votre téléphone."));
        return 1;
    }
}
