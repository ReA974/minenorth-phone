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
        // COMMON (dossier config/) et non SERVER (<monde>/serverconfig/, remis à zéro à chaque redémarrage) ; serveur dédié seulement : le client ne crée aucun fichier.
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PhoneConfig.SPEC, "minenorthphone-server.toml");
        }
        fr.minenorth.api.MineNorth.provide(fr.minenorth.api.DispatchService.class, new PhoneDispatchProvider());
        MinecraftForge.EVENT_BUS.register(new Sync());
        MinecraftForge.EVENT_BUS.register(new PhoneDoctor());   // /mphonefix + renvoi de l'état du téléphone à la connexion (diagnostic des numéros vides)
    }
}
