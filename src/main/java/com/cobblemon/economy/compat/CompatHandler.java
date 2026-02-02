package com.cobblemon.economy.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;

public class CompatHandler {
    private static boolean isYawpLoaded = false;

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
}
