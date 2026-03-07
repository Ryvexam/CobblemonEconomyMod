package com.cobblemon.economy.compat.impactor;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.impactdev.impactor.api.economy.transactions.EconomyTransaction;
import net.impactdev.impactor.api.economy.transactions.EconomyTransferTransaction;
import net.impactdev.impactor.api.economy.transactions.details.EconomyResultType;
import net.impactdev.impactor.api.economy.transactions.details.EconomyTransactionType;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * An Impactor Account backed by CobblemonEconomy's EconomyManager.
 * All balance operations delegate to the CobblemonEconomy database.
 */
public final class CobblecoAccount implements Account {

    private final Currency currency;
    private final UUID owner;
    private final boolean virtual;

    public CobblecoAccount(Currency currency, UUID owner, boolean virtual) {
        this.currency = currency;
        this.owner = owner;
        this.virtual = virtual;
    }

    public CobblecoAccount(UUID owner) {
        this(CobblecoCurrency.INSTANCE, owner, false);
    }

    @Override
    public @NotNull Currency currency() {
        return currency;
    }

    @Override
    public @NotNull UUID owner() {
        return owner;
    }

    @Override
    public boolean virtual() {
        return virtual;
    }

    @Override
    public @NotNull BigDecimal balance() {
        return CobblemonEconomy.getEconomyManager().getBalance(owner);
    }

    @Override
    public @NotNull EconomyTransaction set(BigDecimal amount) {
        BigDecimal clamped = amount.max(BigDecimal.ZERO);
        CobblemonEconomy.getEconomyManager().setBalance(owner, clamped);
        return new CobblecoTransaction(this, clamped, EconomyTransactionType.SET, EconomyResultType.SUCCESS);
    }

    @Override
    public @NotNull EconomyTransaction withdraw(BigDecimal amount) {
        BigDecimal clamped = amount.max(BigDecimal.ZERO);
        boolean success = CobblemonEconomy.getEconomyManager().subtractBalance(owner, clamped);
        EconomyResultType result = success ? EconomyResultType.SUCCESS : EconomyResultType.NOT_ENOUGH_FUNDS;
        return new CobblecoTransaction(this, clamped, EconomyTransactionType.WITHDRAW, result);
    }

    @Override
    public @NotNull EconomyTransaction deposit(BigDecimal amount) {
        BigDecimal clamped = amount.max(BigDecimal.ZERO);
        CobblemonEconomy.getEconomyManager().addBalance(owner, clamped);
        return new CobblecoTransaction(this, clamped, EconomyTransactionType.DEPOSIT, EconomyResultType.SUCCESS);
    }

    @Override
    public @NotNull EconomyTransferTransaction transfer(Account to, BigDecimal amount) {
        BigDecimal clamped = amount.max(BigDecimal.ZERO);
        boolean success = CobblemonEconomy.getEconomyManager().subtractBalance(owner, clamped);
        if (success) {
            CobblemonEconomy.getEconomyManager().addBalance(to.owner(), clamped);
        }
        EconomyResultType result = success ? EconomyResultType.SUCCESS : EconomyResultType.NOT_ENOUGH_FUNDS;
        return new CobblecoTransferTransaction(this, to, clamped, result);
    }

    @Override
    public @NotNull EconomyTransaction reset() {
        BigDecimal startingBalance = currency.defaultAccountBalance();
        CobblemonEconomy.getEconomyManager().setBalance(owner, startingBalance);
        return new CobblecoTransaction(this, startingBalance, EconomyTransactionType.RESET, EconomyResultType.SUCCESS);
    }

    // ---- Transaction record implementations ----

    private record CobblecoTransaction(
            Account account,
            BigDecimal amount,
            EconomyTransactionType type,
            EconomyResultType result
    ) implements EconomyTransaction {

        @Override
        public Currency currency() {
            return account.currency();
        }

        @Override
        public Supplier<Component> message() {
            return null;
        }

        @Override
        public @NotNull Instant timestamp() {
            return Instant.now();
        }
    }

    private record CobblecoTransferTransaction(
            Account from,
            Account to,
            BigDecimal amount,
            EconomyResultType result
    ) implements EconomyTransferTransaction {

        @Override
        public Currency currency() {
            return from.currency();
        }

        @Override
        public Supplier<Component> message() {
            return null;
        }
    }
}
