package com.cobblemon.economy.compat.impactor;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.storage.EconomyConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.impactdev.impactor.api.Impactor;
import net.impactdev.impactor.api.events.ImpactorEventBus;
import net.impactdev.impactor.api.economy.EconomyService;
import net.impactdev.impactor.api.economy.accounts.Account;
import net.impactdev.impactor.api.economy.events.SuggestEconomyServiceEvent;
import net.impactdev.impactor.api.platform.plugins.PluginMetadata;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Handles integration with the Impactor economy API.
 *
 * <p>When {@code mainCurrency} is {@code "cobeco"} or {@code "cobbledollars"}, this class registers
 * CobblemonEconomy as the Impactor EconomyService provider via {@link SuggestEconomyServiceEvent},
 * making all Impactor-compatible mods use the chosen backend transparently.</p>
 *
 * <p>When {@code mainCurrency} is {@code "impactor"}, CobblemonEconomy does NOT register
 * its own service and instead defers to whatever EconomyService Impactor provides,
 * reading/writing balances through it.</p>
 */
public final class ImpactorIntegration {

    private static boolean registered = false;
    /** True when we successfully registered our CobblecoEconomyService as the Impactor provider. */
    private static boolean ownsService = false;

    private ImpactorIntegration() {}

    /**
     * Called during mod init. If Impactor is loaded, subscribes to the
     * SuggestEconomyServiceEvent on Impactor's event bus.
     *
     * @return true if Impactor is present
     */
    public static boolean register() {
        boolean loaded = FabricLoader.getInstance().isModLoaded("impactor");
        if (!loaded) {
            return false;
        }

        try {
            // Subscribe to the shared event bus directly. Impactor's API service is
            // registered by its own Fabric entrypoint, whose initialization order is
            // not guaranteed relative to this mod. Calling Impactor.instance() here
            // can therefore throw before Impactor has finished bootstrapping, even
            // though the shared event bus is already available.
            ImpactorEventBus.bus().subscribe(SuggestEconomyServiceEvent.class, event -> {
                // Check mainCurrency config. At event fire time, our SERVER_STARTING handler
                // may or may not have loaded the config yet. If config is not loaded, leave
                // Impactor's default service in place; completeRegistration() will apply the
                // configured provider after the server has finished starting.
                EconomyConfig config = CobblemonEconomy.getConfig();
                if (config == null) {
                    CobblemonEconomy.LOGGER.debug("Impactor economy selection deferred until Cobblemon Economy config is loaded.");
                    return;
                }
                String mainCurrency = config.mainCurrency;
                if (mainCurrency == null) mainCurrency = "cobeco";

                if ("impactor".equalsIgnoreCase(mainCurrency.trim())) {
                    // User wants Impactor's own economy as the backend — don't replace it
                    CobblemonEconomy.LOGGER.info("mainCurrency=impactor — NOT registering CobblemonEconomy as Impactor EconomyService provider.");
                    return;
                }

                // For both "cobeco" and "cobbledollars", register our service so that all
                // Impactor-compatible mods route through EconomyManager (which delegates
                // to the correct backend based on mainCurrency).

                PluginMetadata metadata = PluginMetadata.builder()
                        .id("cobblemon-economy")
                        .name("Cobblemon Economy")
                        .version("1.0.0")
                        .build();

                event.suggest(metadata, CobblecoEconomyService::new, 10);
                ownsService = true;
                CobblemonEconomy.LOGGER.info("Registered CobblemonEconomy as Impactor EconomyService provider (priority 10)");
            });

            registered = true;
            CobblemonEconomy.LOGGER.info("Impactor compatibility enabled (event-based, no mixin).");
        } catch (Exception e) {
            CobblemonEconomy.LOGGER.error("Failed to register Impactor event listener", e);
            return false;
        }

        return true;
    }

    /**
     * Applies the configured provider after the server configuration and economy manager are
     * initialized. This is the fallback for loaders where Impactor posts its suggestion event
     * before Cobblemon Economy's per-world config is loaded.
     */
    public static void completeRegistration() {
        if (!registered || ownsService) {
            return;
        }

        EconomyConfig config = CobblemonEconomy.getConfig();
        String mainCurrency = config != null && config.mainCurrency != null
                ? config.mainCurrency
                : "cobeco";
        if ("impactor".equalsIgnoreCase(mainCurrency.trim())) {
            CobblemonEconomy.LOGGER.info("mainCurrency=impactor — keeping Impactor's EconomyService provider.");
            return;
        }

        try {
            Impactor.instance().services().register(EconomyService.class, new CobblecoEconomyService());
            ownsService = true;
            CobblemonEconomy.LOGGER.info("Registered CobblemonEconomy as Impactor EconomyService provider after config load (priority 10)");
        } catch (Exception e) {
            CobblemonEconomy.LOGGER.error("Failed to apply configured Impactor economy provider", e);
        }
    }

    /**
     * Whether we own the Impactor EconomyService (i.e. we registered CobblecoEconomyService).
     * When true, external mods calling Impactor's API already go through our service,
     * so there is nothing to "sync" to Impactor — it IS us.
     */
    public static boolean ownsService() {
        return ownsService;
    }

    // ---- Direct access to Impactor's own economy (used when mainCurrency = "impactor") ----
    // These methods are only called when mainCurrency=impactor, meaning we did NOT register
    // our service, so EconomyService.instance() returns Impactor's own implementation.

    public static boolean canAccess(UUID uuid) {
        return uuid != null && registered;
    }

    /**
     * Gets a balance from Impactor's EconomyService directly.
     * Used when mainCurrency is "impactor".
     */
    public static BigDecimal getBalance(UUID uuid) {
        if (uuid == null) return null;
        try {
            EconomyService service = EconomyService.instance();
            Account account = service.account(uuid).get(2, TimeUnit.SECONDS);
            return account.balance();
        } catch (Exception e) {
            CobblemonEconomy.LOGGER.debug("Failed to get Impactor balance for {}", uuid, e);
        }
        return null;
    }

    /**
     * Withdraws from Impactor's EconomyService directly.
     */
    public static boolean withdraw(UUID uuid, BigDecimal amount) {
        if (uuid == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        try {
            EconomyService service = EconomyService.instance();
            Account account = service.account(uuid).get(2, TimeUnit.SECONDS);
            return account.withdraw(amount).successful();
        } catch (Exception e) {
            CobblemonEconomy.LOGGER.debug("Failed to withdraw from Impactor for {}", uuid, e);
        }
        return false;
    }

    /**
     * Deposits into Impactor's EconomyService directly.
     */
    public static boolean deposit(UUID uuid, BigDecimal amount) {
        if (uuid == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return false;
        try {
            EconomyService service = EconomyService.instance();
            Account account = service.account(uuid).get(2, TimeUnit.SECONDS);
            return account.deposit(amount).successful();
        } catch (Exception e) {
            CobblemonEconomy.LOGGER.debug("Failed to deposit to Impactor for {}", uuid, e);
        }
        return false;
    }

    /**
     * Sets balance in Impactor's EconomyService directly.
     */
    public static boolean setBalance(UUID uuid, BigDecimal amount) {
        if (uuid == null || amount == null || amount.compareTo(BigDecimal.ZERO) < 0) return false;
        try {
            EconomyService service = EconomyService.instance();
            Account account = service.account(uuid).get(2, TimeUnit.SECONDS);
            return account.set(amount).successful();
        } catch (Exception e) {
            CobblemonEconomy.LOGGER.debug("Failed to set Impactor balance for {}", uuid, e);
        }
        return false;
    }
}
