package fr.minenorth.phone;

import net.minecraftforge.common.ForgeConfigSpec;

/** <monde>/serverconfig/minenorthphone-server.toml */
public final class PhoneConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED, AUTO_ROLES, GIVE_TABLET, ALERT_CHAT, ALERT_MAP, EUROBANK;
    public static final ForgeConfigSpec.IntValue CHECK_SECONDS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("pont");
        ENABLED = b.comment("false = le pont ne fait rien.").define("actif", true);
        CHECK_SECONDS = b.comment("Toutes les combien de secondes les rôles et les appels sont vérifiés.")
                .defineInRange("verificationSecondes", 2, 1, 60);
        b.pop();
        b.push("roles");
        AUTO_ROLES = b.comment("Un policier (resp. secouriste) EN SERVICE devient répondant « police » (resp. « ems ») du téléphone, et le",
                        "redevient simple joueur à la fin de son service. Les rôles donnés à la main par un op ne sont jamais retirés.")
                .define("rolesAutomatiques", true);
        GIVE_TABLET = b.comment("Remettre la tablette de répondant du téléphone (police / secours) quand le rôle est donné, s'il ne l'a pas.",
                        "false (défaut) : rien n'est donné, les policiers / secouristes utilisent les tablettes MineNorth.")
                .define("donnerTablette", false);
        b.pop();
        b.push("alertes");
        ALERT_CHAT = b.comment("Prévenir dans le chat les policiers / secouristes en service quand quelqu'un appelle.")
                .define("alerteChat", true);
        ALERT_MAP = b.comment("Poser aussi un repère sur la carte (mod minenorth_map), retiré quand l'appel est clos.")
                .define("alerteCarte", true);
        b.pop();
        b.push("banque");
        EUROBANK = b.comment("L'application Banque du téléphone affiche et utilise les comptes MineNorth EuroBank (solde, virements entre joueurs).",
                        "Sans effet si Lightman's Currency ou Kuro's Economy fournit déjà la monnaie du téléphone.")
                .define("brancherEuroBank", true);
        b.pop();
        SPEC = b.build();
    }

    private PhoneConfig() {}
}
