package fr.minenorth.phone;

import com.mojang.logging.LogUtils;
import fr.minenorth.api.MineNorth;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 1) Rôles : un policier / secouriste EN SERVICE (mods Police / Secours) devient répondant « police » / « ems » du téléphone.
 * 2) Alertes : un appel d'urgence passé depuis le téléphone prévient les policiers / secouristes en service (chat + repère sur la carte).
 */
public final class Sync {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String POLICE = "POLICE", EMS = "EMS";
    private static final int POLICE_COLOR = 0x3949AB, EMS_COLOR = 0xE53935;

    /** Appel -> joueurs déjà prévenus (pour ne prévenir qu'une fois, et retirer leur repère à la clôture). */
    private final Map<Long, Set<UUID>> notified = new HashMap<>();
    private int ticks;

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !PhoneConfig.ENABLED.get()) return;
        if (++ticks < PhoneConfig.CHECK_SECONDS.get() * 20) return;
        ticks = 0;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.overworld() == null || !PhoneBridge.available()) return;
        try {
            if (PhoneConfig.AUTO_ROLES.get()) syncRoles(server);
            if (PhoneConfig.ALERT_CHAT.get() || PhoneConfig.ALERT_MAP.get()) alerts(server);
        } catch (RuntimeException ex) {
            LOG.warn("[Telephone] Erreur du pont : {}", ex.toString());
        }
    }

    // ------------------------------------------------------------------ rôles

    private static boolean onDuty(MinecraftServer s, UUID id, String service) {
        return service.equals(POLICE) ? MineNorth.police().onDuty(s, id) : MineNorth.secours().onDuty(s, id);
    }

    private void syncRoles(MinecraftServer server) {
        BridgeData data = BridgeData.get(server);
        Set<UUID> ids = new LinkedHashSet<>(data.granted.keySet());
        for (ServerPlayer p : server.getPlayerList().getPlayers()) ids.add(p.getUUID());

        boolean changed = false;
        for (UUID id : ids) {
            Set<String> want = new LinkedHashSet<>();
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {   // hors ligne : plus en service
                if (onDuty(server, id, POLICE)) want.add(POLICE);
                if (onDuty(server, id, EMS)) want.add(EMS);
            }
            Set<String> mine = data.granted.getOrDefault(id, Set.of());
            if (want.equals(mine)) continue;

            // rôles actuels, sans ceux que le pont avait donnés, plus ceux voulus maintenant
            Set<String> next = new LinkedHashSet<>(PhoneBridge.services(server, id));
            next.removeAll(mine);
            next.addAll(want);
            PhoneBridge.setServices(server, id, next);
            if (want.isEmpty()) data.granted.remove(id); else data.granted.put(id, new LinkedHashSet<>(want));
            data.setDirty();
            changed = true;

            if (p != null) {
                Set<String> added = new HashSet<>(want);
                added.removeAll(mine);
                for (String s : added) {
                    if (PhoneConfig.GIVE_TABLET.get()) PhoneBridge.giveTablet(p, s);
                    p.sendSystemMessage(Component.literal("§b[Téléphone] §7Vous recevez les appels d'urgence "
                            + (s.equals(POLICE) ? "police (17)" : "secours (18)") + " tant que vous êtes en service."));
                }
            }
        }
        if (changed) PhoneBridge.revalidate(server);
    }

    // ------------------------------------------------------------------ alertes

    private static String label(String category) {
        return switch (category) {
            case "SUSPICIOUS_ACTIVITY" -> "Activité suspecte";
            case "FIGHT_ASSAULT" -> "Agression / bagarre";
            case "VEHICLE_INCIDENT" -> "Incident de véhicule";
            case "ARMED_THREAT" -> "Menace armée";
            case "ROBBERY" -> "Vol / braquage";
            case "POLICE_OTHER" -> "Autre (police)";
            case "INJURY" -> "Blessé";
            case "TRAFFIC_ACCIDENT" -> "Accident de la route";
            case "GUNSHOT_WOUND" -> "Blessure par balle";
            case "SEVERE_BLEEDING" -> "Hémorragie grave";
            case "UNCONSCIOUS_PERSON" -> "Personne inconsciente";
            case "EMS_OTHER" -> "Autre (secours)";
            default -> category;
        };
    }

    private static String markerName(long id) { return "Appel #" + id; }

    private void alerts(MinecraftServer server) {
        List<PhoneBridge.Call> calls = PhoneBridge.activeCalls(server);
        Set<Long> activeIds = new HashSet<>();
        for (PhoneBridge.Call c : calls) {
            activeIds.add(c.id());
            Set<UUID> done = notified.computeIfAbsent(c.id(), k -> new HashSet<>());
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (done.contains(p.getUUID()) || !onDuty(server, p.getUUID(), c.service())) continue;
                done.add(p.getUUID());
                boolean police = c.service().equals(POLICE);
                if (PhoneConfig.ALERT_CHAT.get()) {
                    p.sendSystemMessage(Component.literal((police ? "§9[Urgence 17] " : "§c[Urgence 18] ") + "§f" + label(c.category())
                            + " §7- " + c.caller() + " en §f" + c.x() + " " + c.y() + " " + c.z()
                            + (c.description().isBlank() ? "" : " §7: " + c.description())));
                }
                if (PhoneConfig.ALERT_MAP.get()) MapBridge.set(p, markerName(c.id()), c.dim(), c.x(), c.y(), c.z(), police ? POLICE_COLOR : EMS_COLOR);
            }
        }
        // appels clos ou annulés : on retire les repères
        for (var it = notified.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            if (activeIds.contains(en.getKey())) continue;
            for (UUID id : en.getValue()) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) MapBridge.remove(p, markerName(en.getKey()));
            }
            it.remove();
        }
    }
}
