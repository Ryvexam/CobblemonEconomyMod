package com.cobblemon.economy.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.math.BigDecimal;
import java.util.UUID;

public class CompatHandler {
    private static boolean isYawpLoaded = false;
    private static boolean hasCobbleDollarsCompat = false;
    private static boolean hasImpactorCompat = false;

    public static void init() {
        if (FabricLoader.getInstance().isModLoaded("yawp")) {
            isYawpLoaded = true;
            try {
                com.cobblemon.economy.compat.yawp.YawpIntegration.register();
            } catch (Exception e) {
                System.err.println("[CobblemonEconomy] Failed to register YAWP integration: " + e.getMessage());
            }
        }

        try {
            com.cobblemon.economy.compat.placeholder.PlaceholderApiIntegration.register();
        } catch (Exception e) {
            System.err.println("[CobblemonEconomy] Failed to register Placeholder API integration: " + e.getMessage());
        }

        try {
            hasCobbleDollarsCompat = com.cobblemon.economy.compat.cobbledollars.CobbleDollarsIntegration.register();
        } catch (Exception e) {
            hasCobbleDollarsCompat = false;
            System.err.println("[CobblemonEconomy] Failed to register CobbleDollars integration: " + e.getMessage());
        }

        try {
            hasImpactorCompat = com.cobblemon.economy.compat.impactor.ImpactorIntegration.register();
        } catch (Exception e) {
            hasImpactorCompat = false;
            System.err.println("[CobblemonEconomy] Failed to register Impactor integration: " + e.getMessage());
        }
    }

    /**
     * Checks if attack is allowed by YAWP.
     * @return TRUE (Allowed/Vulnerable), FALSE (Denied/Invulnerable), or NULL (Undefined/No YAWP).
     */
    public static Boolean checkYawpAttack(Entity target, Entity attacker) {
        if (isYawpLoaded) {
            try {
                return com.cobblemon.economy.compat.yawp.YawpIntegration.checkFlag(target, attacker);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public static boolean hasCobbleDollarsCompat() {
        return hasCobbleDollarsCompat;
    }

    public static boolean hasImpactorCompat() {
        return hasImpactorCompat;
    }

    public static boolean canAccessCobbleDollars(ServerPlayer player) {
        return com.cobblemon.economy.compat.cobbledollars.CobbleDollarsIntegration.canAccess(player);
    }

    public static int getCobbleDollars(ServerPlayer player) {
        Integer balance = com.cobblemon.economy.compat.cobbledollars.CobbleDollarsIntegration.getBalance(player);
        return balance == null ? 0 : balance;
    }

    public static boolean spendCobbleDollars(ServerPlayer player, int amount) {
        return com.cobblemon.economy.compat.cobbledollars.CobbleDollarsIntegration.spend(player, amount);
    }

    public static boolean setCobbleDollars(ServerPlayer player, int amount) {
        return com.cobblemon.economy.compat.cobbledollars.CobbleDollarsIntegration.set(player, amount);
    }

    public static boolean earnCobbleDollars(ServerPlayer player, int amount) {
        return com.cobblemon.economy.compat.cobbledollars.CobbleDollarsIntegration.earn(player, amount);
    }

    public static boolean canAccessImpactor(UUID uuid) {
        return com.cobblemon.economy.compat.impactor.ImpactorIntegration.canAccess(uuid);
    }

    public static BigDecimal getImpactorBalance(UUID uuid) {
        return com.cobblemon.economy.compat.impactor.ImpactorIntegration.getBalance(uuid);
    }

    public static boolean withdrawImpactor(UUID uuid, BigDecimal amount) {
        return com.cobblemon.economy.compat.impactor.ImpactorIntegration.withdraw(uuid, amount);
    }

    public static boolean depositImpactor(UUID uuid, BigDecimal amount) {
        return com.cobblemon.economy.compat.impactor.ImpactorIntegration.deposit(uuid, amount);
    }

    public static boolean setImpactorBalance(UUID uuid, BigDecimal amount) {
        return com.cobblemon.economy.compat.impactor.ImpactorIntegration.setBalance(uuid, amount);
    }
}
