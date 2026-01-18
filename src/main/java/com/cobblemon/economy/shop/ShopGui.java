package com.cobblemon.economy.shop;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.networking.OpenShopPayload;
import com.cobblemon.economy.storage.EconomyConfig;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ShopGui {
    public static void open(ServerPlayer player, String shopId) {
        EconomyConfig.ShopDefinition shop = CobblemonEconomy.getConfig().shops.get(shopId);
        
        if (shop == null) {
            // Fallback to default if not found
            shop = CobblemonEconomy.getConfig().shops.get("default_poke");
        }

        if (shop == null) return;

        BigDecimal bal = CobblemonEconomy.getEconomyManager().getBalance(player.getUUID());
        BigDecimal pco = CobblemonEconomy.getEconomyManager().getPco(player.getUUID());
        
        List<OpenShopPayload.ShopItemData> itemData = shop.items.stream()
            .map(item -> new OpenShopPayload.ShopItemData(item.id, item.name, item.price))
            .collect(Collectors.toList());

        ServerPlayNetworking.send(player, new OpenShopPayload(
            bal, 
            pco, 
            shop.title, 
            shop.currency, 
            itemData
        ));
    }
}
