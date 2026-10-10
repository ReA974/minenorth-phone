package fr.minenorth.phone;

import com.mikasrevs.newphone.integration.currency.PhoneCurrencyIntegration;
import fr.minenorth.api.BankTx;
import fr.minenorth.api.MineNorth;
import fr.minenorth.api.PayResult;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Fournisseur de monnaie « EuroBank » pour l'application Banque de MikasRevs Phone 2 : solde du compte du joueur et virements entre comptes,
 * via l'API MineNorth ({@code MineNorth.bank()}). Montants en centimes (c'est l'unité de l'API bancaire).
 * Cette classe n'est chargée que si le téléphone est présent ({@link PhoneBank}).
 */
public final class EuroBankIntegration implements PhoneCurrencyIntegration {
    private static final String PROVIDER = "MineNorth EuroBank";
    public static final String CURRENCY_ID = "minenorth_eurobank:balance";

    /** Montants proposés par l'application (centimes). */
    private static final List<Unit> UNITS = List.of(
            new Unit("1 €", 100), new Unit("5 €", 500), new Unit("10 €", 1000), new Unit("20 €", 2000),
            new Unit("50 €", 5000), new Unit("100 €", 10000), new Unit("500 €", 50000));

    /** « 1 234,50 € » (centimes -> texte). */
    static String euros(long cents) {
        long abs = Math.abs(cents);
        String whole = Long.toString(abs / 100);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < whole.length(); i++) {
            if (i > 0 && (whole.length() - i) % 3 == 0) sb.append(' ');
            sb.append(whole.charAt(i));
        }
        return (cents < 0 ? "-" : "") + sb + "," + String.format("%02d", abs % 100) + " €";
    }

    @Override
    public Snapshot snapshot(ServerPlayer player) {
        MinecraftServer s = player.getServer();
        if (s == null) return new Snapshot(false, "ERROR", PROVIDER, "", List.of());
        if (!MineNorth.bank().hasAccount(s, player.getUUID())) {
            return new Snapshot(false, "ACCOUNT_UNAVAILABLE", PROVIDER, player.getGameProfile().getName(), List.of());
        }
        long cents = MineNorth.bank().balance(s, player.getUUID());
        Balance b = new Balance(CURRENCY_ID, "Compte EuroBank", euros(cents), cents, UNITS);
        return new Snapshot(true, "READY", PROVIDER, player.getGameProfile().getName(), List.of(b), 1, false);
    }

    /** Prix d'une annonce de la petite annonce (Marketplace) : valide si c'est notre monnaie et un montant positif (en centimes). */
    @Override
    public PriceQuote quote(ServerPlayer player, String currencyId, long amount) {
        if (!CURRENCY_ID.equals(currencyId) || amount <= 0) return new PriceQuote(false, "", "");
        return new PriceQuote(true, "Compte EuroBank", euros(amount));
    }

    @Override
    public TransferResult transfer(ServerPlayer player, UUID target, String targetName, long amount) {
        MinecraftServer s = player.getServer();
        if (s == null) return result(TransferStatus.FAILED, amount);
        if (amount <= 0) return result(TransferStatus.INVALID_AMOUNT, amount);
        if (target == null || target.equals(player.getUUID())) return result(TransferStatus.FAILED, amount);
        if (!MineNorth.bank().hasAccount(s, player.getUUID()) || !MineNorth.bank().hasAccount(s, target)) {
            return result(TransferStatus.ACCOUNT_UNAVAILABLE, amount);
        }
        PayResult r = MineNorth.bank().transfer(s, player.getUUID(), target, amount, BankTx.PAYMENT,
                "Virement depuis le téléphone" + (targetName == null || targetName.isBlank() ? "" : " vers " + targetName),
                player.getGameProfile().getName());
        return switch (r) {
            case OK -> result(TransferStatus.SUCCESS, amount);
            case INSUFFICIENT_FUNDS -> result(TransferStatus.INSUFFICIENT_FUNDS, amount);
            case INVALID_AMOUNT -> result(TransferStatus.INVALID_AMOUNT, amount);
            case NO_ACCOUNT -> result(TransferStatus.ACCOUNT_UNAVAILABLE, amount);
            default -> result(TransferStatus.FAILED, amount);
        };
    }

    private static TransferResult result(TransferStatus status, long amount) {
        return new TransferResult(status, euros(amount));
    }
}
