package fr.minenorth.phone;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dépannage des numéros / contacts du téléphone (« Mon numéro # » vide, aucun contact) : le téléphone envoie son état au joueur à l'ouverture ;
 * si cet envoi plante côté serveur, le client reste à vide sans rien afficher. Ici on renvoie l'état de force, quelques secondes après la connexion
 * et à la demande (/mphonefix), et on écrit la vraie erreur dans le log et dans le chat des opérateurs.
 * Par réflexion : sans le téléphone, ne fait rien.
 */
public final class PhoneDoctor {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String SERVICE = "com.mikasrevs.newphone.server.PhoneServerService";

    /** Joueur -> tick serveur auquel renvoyer l'état. */
    private final Map<UUID, Long> pending = new LinkedHashMap<>();
    private Method sendState;
    private boolean resolved;

    private Method sendState() {
        if (resolved) return sendState;
        resolved = true;
        try {
            if (!ModList.get().isLoaded("newphone")) return null;
            sendState = Class.forName(SERVICE).getMethod("sendState", ServerPlayer.class, String.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            LOG.warn("[Telephone] Dépannage : PhoneServerService.sendState introuvable ({})", e.toString());
        }
        return sendState;
    }

    /** Renvoie l'état du téléphone au joueur ; null si tout va bien, sinon le message d'erreur. */
    private String push(ServerPlayer p) {
        Method m = sendState();
        if (m == null) return "Téléphone (newphone) absent ou version incompatible.";
        try {
            m.invoke(null, p, "");
            return null;
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause() == null ? e : e.getCause();
            LOG.error("[Telephone] L'envoi de l'état du téléphone à {} a échoué : {}", p.getGameProfile().getName(), t.toString(), t);
            StackTraceElement[] st = t.getStackTrace();
            return t + (st.length > 0 ? " @ " + st[0] : "");
        } catch (ReflectiveOperationException e) {
            return e.toString();
        }
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.getServer() != null) pending.put(p.getUUID(), (long) p.getServer().getTickCount() + 100);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || pending.isEmpty()) return;
        long now = e.getServer().getTickCount();
        for (Iterator<Map.Entry<UUID, Long>> it = pending.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> en = it.next();
            if (en.getValue() > now) continue;
            it.remove();
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(en.getKey());
            if (p == null) continue;
            String err = push(p);
            if (err != null) {
                for (ServerPlayer op : e.getServer().getPlayerList().getPlayers()) {
                    if (op.hasPermissions(2)) op.sendSystemMessage(Component.literal("§c[Téléphone] L'état du téléphone de " + p.getGameProfile().getName() + " n'a pas pu être envoyé : " + err));
                }
            }
        }
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("mphonefix").requires(s -> s.hasPermission(2))
                .executes(c -> fix(c.getSource(), c.getSource().getPlayerOrException()))
                .then(Commands.argument("joueur", EntityArgument.player()).executes(c -> fix(c.getSource(), EntityArgument.getPlayer(c, "joueur")))));
    }

    private int fix(CommandSourceStack src, ServerPlayer p) {
        String err = push(p);
        if (err == null) {
            src.sendSuccess(() -> Component.literal("§aÉtat du téléphone renvoyé à " + p.getGameProfile().getName() + " : rouvre le téléphone."), false);
            return 1;
        }
        src.sendFailure(Component.literal("Échec de l'envoi de l'état du téléphone : " + err + " (détails dans le log du serveur)."));
        return 0;
    }
}
