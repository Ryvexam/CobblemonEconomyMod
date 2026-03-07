package com.cobblemon.economy.compat.impactor;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.currency.Currency;
import net.impactdev.impactor.api.economy.currency.CurrencyProvider;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Impactor EconomyService implementation backed entirely by CobblemonEconomy's EconomyManager.
 * <p>
 * When registered via {@link net.impactdev.impactor.api.economy.events.SuggestEconomyServiceEvent},
 * this replaces Impactor's default economy service so that all mods using the Impactor economy API
 * transparently use CobblemonEconomy's PokéDollars.
 */
public final class CobblecoEconomyService implements EconomyService {

    private final CobblecoCurrency.Provider currencyProvider = CobblecoCurrency.Provider.INSTANCE;
    private final ConcurrentHashMap<UUID, CobblecoAccount> accountCache = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "CobblemonEconomy";
    }

    @Override
    public CurrencyProvider currencies() {
        return currencyProvider;
    }

    @Override
    public CompletableFuture<Boolean> hasAccount(Currency currency, UUID uuid) {
        // CobblemonEconomy auto-creates accounts on first access, so any known player "has" an account
        return CompletableFuture.completedFuture(uuid != null);
    }

    @Override
    public CompletableFuture<Account> account(Currency currency, UUID uuid) {
        return CompletableFuture.completedFuture(getOrCreateAccount(uuid));
    }

    @Override
    public CompletableFuture<Account> account(Currency currency, UUID uuid, Account.AccountModifier modifier) {
        // The modifier is used for things like marking accounts as virtual.
        // Our accounts are always backed by the CobblemonEconomy DB, so we ignore the modifier
        // but still return a valid account.
        return CompletableFuture.completedFuture(getOrCreateAccount(uuid));
    }

    @Override
    public CompletableFuture<Multimap<Currency, Account>> accounts() {
        // Return an empty multimap — we don't enumerate all accounts from our DB for this.
        // This is an expensive operation that most mods won't call.
        Multimap<Currency, Account> result = HashMultimap.create();
        accountCache.values().forEach(account -> result.put(CobblecoCurrency.INSTANCE, account));
        return CompletableFuture.completedFuture(ImmutableMultimap.copyOf(result));
    }

    @Override
    public CompletableFuture<Void> deleteAccount(Currency currency, UUID uuid) {
        accountCache.remove(uuid);
        // CobblemonEconomy doesn't support account deletion — reset to default instead
        if (CobblemonEconomy.getEconomyManager() != null) {
            CobblemonEconomy.getEconomyManager().setBalance(uuid, currency.defaultAccountBalance());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> save(Account account) {
        // No-op: CobblemonEconomy persists immediately on every write operation
        return CompletableFuture.completedFuture(null);
    }

    private CobblecoAccount getOrCreateAccount(UUID uuid) {
        return accountCache.computeIfAbsent(uuid, CobblecoAccount::new);
    }
}
