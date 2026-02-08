package com.cobblemon.economy.compat.impactor;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.fabricmc.loader.api.FabricLoader;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class ImpactorIntegration {
    private static final String SERVICE_CLASS = "net.impactdev.impactor.api.economy.EconomyService";

    private ImpactorIntegration() {
    }

    public static boolean register() {
        boolean loaded = FabricLoader.getInstance().isModLoaded("impactor");
        if (loaded) {
            CobblemonEconomy.LOGGER.info("Impactor compatibility enabled.");
        }
        return loaded;
    }

    public static boolean canAccess(UUID uuid) {
        return uuid != null && getServiceInstance() != null;
    }

    public static BigDecimal getBalance(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        try {
            Object service = getServiceInstance();
            if (service == null) {
                return null;
            }

            Object account = getAccount(service, uuid);
            if (account == null) {
                return null;
            }

            Object balance = account.getClass().getMethod("balance").invoke(account);
            return balance instanceof BigDecimal decimal ? decimal : null;
        } catch (Exception ignored) {
        }

        return null;
    }

    public static boolean withdraw(UUID uuid, BigDecimal amount) {
        if (uuid == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        try {
            Object service = getServiceInstance();
            if (service == null) {
                return false;
            }

            Object account = getAccount(service, uuid);
            if (account == null) {
                return false;
            }

            Object transaction = account.getClass().getMethod("withdraw", BigDecimal.class).invoke(account, amount);
            if (transaction == null) {
                return false;
            }

            Object success = transaction.getClass().getMethod("successful").invoke(transaction);
            return success instanceof Boolean result && result;
        } catch (Exception ignored) {
        }

        return false;
    }

    public static boolean deposit(UUID uuid, BigDecimal amount) {
        if (uuid == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        try {
            Object service = getServiceInstance();
            if (service == null) {
                return false;
            }

            Object account = getAccount(service, uuid);
            if (account == null) {
                return false;
            }

            Object transaction = account.getClass().getMethod("deposit", BigDecimal.class).invoke(account, amount);
            if (transaction == null) {
                return false;
            }

            Object success = transaction.getClass().getMethod("successful").invoke(transaction);
            return success instanceof Boolean result && result;
        } catch (Exception ignored) {
        }

        return false;
    }

    public static boolean setBalance(UUID uuid, BigDecimal amount) {
        if (uuid == null || amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        try {
            Object service = getServiceInstance();
            if (service == null) {
                return false;
            }

            Object account = getAccount(service, uuid);
            if (account == null) {
                return false;
            }

            Object transaction = account.getClass().getMethod("set", BigDecimal.class).invoke(account, amount);
            if (transaction == null) {
                return false;
            }

            Object success = transaction.getClass().getMethod("successful").invoke(transaction);
            return success instanceof Boolean result && result;
        } catch (Exception ignored) {
        }

        return false;
    }

    private static Object getServiceInstance() {
        try {
            Class<?> serviceClass = Class.forName(SERVICE_CLASS);
            return serviceClass.getMethod("instance").invoke(null);
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Object getAccount(Object service, UUID uuid) {
        try {
            Object future = service.getClass().getMethod("account", UUID.class).invoke(service, uuid);
            if (!(future instanceof CompletableFuture<?> completableFuture)) {
                return null;
            }
            return completableFuture.get(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        return null;
    }
}
