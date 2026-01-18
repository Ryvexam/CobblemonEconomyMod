package com.cobblemon.economy.client;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.networking.OpenShopPayload;
import com.cobblemon.economy.networking.PurchasePayload;
import com.cobblemon.economy.shop.ShopScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;

public class CobblemonEconomyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(CobblemonEconomy.SHOPKEEPER, ShopkeeperRenderer::new);

        ClientPlayNetworking.registerGlobalReceiver(OpenShopPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                // Ouverture propre d'une HandledScreen
                ShopScreenHandler handler = new ShopScreenHandler(0, context.client().player.getInventory());
                Minecraft.getInstance().setScreen(new ShopScreen(handler, context.client().player.getInventory(), payload));
            });
        });
    }
}
