package fr.minenorth.phone;

import fr.minenorth.api.DispatchService;
import fr.minenorth.api.EmergencyCall;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Fournit les appels d'urgence du téléphone aux tablettes police / secours (MineNorth API). Vide si le téléphone est absent. */
final class PhoneDispatchProvider implements DispatchService {
    @Override
    public List<EmergencyCall> activeCalls(MinecraftServer s, String service) {
        List<EmergencyCall> out = new ArrayList<>();
        if (!PhoneConfig.ENABLED.get() || !PhoneBridge.available()) return out;
        long now = System.currentTimeMillis();
        for (PhoneBridge.Call c : PhoneBridge.activeCalls(s)) {
            if (!c.service().equals(service)) continue;
            out.add(new EmergencyCall(c.id(), c.service(), Sync.label(c.category()), c.caller(), c.dim(), c.x(), c.y(), c.z(), c.description(),
                    (int) Math.max(0, (now - c.createdAt()) / 1000)));
        }
        out.sort(Comparator.comparingLong(EmergencyCall::id));
        return out;
    }
}
