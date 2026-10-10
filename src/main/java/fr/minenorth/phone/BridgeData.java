package fr.minenorth.phone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Rôles du téléphone donnés PAR le pont (joueur -> services) : eux seuls sont retirés à la fin du service, jamais ceux donnés à la main. */
public class BridgeData extends SavedData {
    private static final String NAME = "minenorth_phone_bridge";

    public final Map<UUID, Set<String>> granted = new HashMap<>();

    public static BridgeData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(BridgeData::load, BridgeData::new, NAME);
    }

    public static BridgeData load(CompoundTag tag) {
        BridgeData d = new BridgeData();
        ListTag list = tag.getList("Granted", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            if (!t.hasUUID("Id")) continue;
            Set<String> s = new LinkedHashSet<>();
            for (Tag e : t.getList("Services", Tag.TAG_STRING)) s.add(e.getAsString());
            if (!s.isEmpty()) d.granted.put(t.getUUID("Id"), s);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Set<String>> en : granted.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", en.getKey());
            ListTag s = new ListTag();
            for (String n : en.getValue()) s.add(StringTag.valueOf(n));
            t.put("Services", s);
            list.add(t);
        }
        tag.put("Granted", list);
        return tag;
    }
}
