package com.cobblemon.economy.client;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.networking.OpenShopPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;

public class CobblemonEconomyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Enregistrement des renderers
        EntityRendererRegistry.register(CobblemonEconomy.POKE_SHOPKEEPER, ShopkeeperRenderer::new);
        EntityRendererRegistry.register(CobblemonEconomy.PCO_SHOPKEEPER, ShopkeeperRenderer::new);

        // On ne définit QUE le récepteur de paquets ici (pas l'enregistrement du type)
        ClientPlayNetworking.registerGlobalReceiver(OpenShopPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft.getInstance().setScreen(new ShopScreen(payload.balance(), payload.pco(), payload.shopType()));
            });
        });
    }
}
