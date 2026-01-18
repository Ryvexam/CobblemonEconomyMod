package com.cobblemon.economy.shop;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.networking.OpenShopPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ShopGui {
    
    public enum ShopType {
        POKE,
        PCO
    }

    public static void open(ServerPlayer player, ShopType type) {
        BigDecimal bal = CobblemonEconomy.getEconomyManager().getBalance(player.getUUID());
        BigDecimal pco = CobblemonEconomy.getEconomyManager().getPco(player.getUUID());
        
        // We use the same payload but the client screen will filter based on shop type if needed,
        // or we can pass the shop type in the payload. Let's update the payload.
        ServerPlayNetworking.send(player, new OpenShopPayload(bal, pco, type.name()));
    }
}
