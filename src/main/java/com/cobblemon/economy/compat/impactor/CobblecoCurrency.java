package com.cobblemon.economy.compat.impactor;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.impactdev.impactor.api.economy.currency.CurrencyProvider;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CobblemonEconomy's PokéDollars currency exposed as an Impactor Currency.
 */
public final class CobblecoCurrency implements Currency {

    public static final Key CURRENCY_KEY = Key.key("cobblemon-economy", "pokedollars");
    public static final CobblecoCurrency INSTANCE = new CobblecoCurrency();

    private CobblecoCurrency() {}

    @Override
    public Key key() {
        return CURRENCY_KEY;
    }

    @Override
    public Component singular() {
        return Component.text("PokéDollar");
    }

    @Override
    public Component plural() {
        return Component.text("PokéDollars");
    }

    @Override
    public Component symbol() {
        return Component.text("$");
    }

    @Override
    public CurrencyFormatting formatting() {
        return new CurrencyFormatting("${amount}", "{amount} PokéDollars");
    }

    @Override
    public BigDecimal defaultAccountBalance() {
        if (CobblemonEconomy.getConfig() != null) {
            return CobblemonEconomy.getConfig().startingBalance;
        }
        return new BigDecimal(1000);
    }

    @Override
    public int decimals() {
        return 0;
    }

    @Override
    public boolean primary() {
        return true;
    }

    @Override
    public TriState transferable() {
        return TriState.TRUE;
    }

    @Override
    public Component format(@NotNull BigDecimal amount, boolean condensed, @NotNull Locale locale) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        String formatted = formatter.format(amount.setScale(0, RoundingMode.DOWN));

        if (condensed) {
            return Component.text("$" + formatted);
        } else {
            boolean isSingular = amount.compareTo(BigDecimal.ONE) == 0;
            String name = isSingular ? "PokéDollar" : "PokéDollars";
            return Component.text(formatted + " " + name);
        }
    }

    /**
     * A simple CurrencyProvider that only knows about our PokéDollars currency.
     */
    public static final class Provider implements CurrencyProvider {

        public static final Provider INSTANCE = new Provider();
        private final Set<Currency> registered = ConcurrentHashMap.newKeySet();

        private Provider() {
            registered.add(CobblecoCurrency.INSTANCE);
        }

        @Override
        public @NotNull Currency primary() {
            return CobblecoCurrency.INSTANCE;
        }

        @Override
        public Optional<Currency> currency(Key key) {
            return registered.stream()
                    .filter(c -> c.key().equals(key))
                    .findFirst();
        }

        @Override
        public Set<Currency> registered() {
            return Set.copyOf(registered);
        }

        @Override
        public CompletableFuture<Boolean> register(Currency currency) {
            return CompletableFuture.completedFuture(registered.add(currency));
        }
    }
}
