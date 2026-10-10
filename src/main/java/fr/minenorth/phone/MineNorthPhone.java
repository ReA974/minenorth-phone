package fr.minenorth.phone;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Pont entre MikasRevs Phone 2 (modid « newphone ») et les mods Police / Secours MineNorth.
 * Mod serveur : aucune classe client, les joueurs n'ont pas besoin du jar. Sans le téléphone, il ne fait rien.
 */
@Mod(MineNorthPhone.MOD_ID)
public class MineNorthPhone {
    public static final String MOD_ID = "minenorthphone";

    public MineNorthPhone() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, PhoneConfig.SPEC);
        MinecraftForge.EVENT_BUS.register(new Sync());
    }
}
