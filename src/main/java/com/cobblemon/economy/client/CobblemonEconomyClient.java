package com.cobblemon.economy.client;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobblemonEconomyClient implements ClientModInitializer {
    public static final Logger CLIENT_LOGGER = LoggerFactory.getLogger("cobblemon-economy-client");

    @Override
    public void onInitializeClient() {
        CLIENT_LOGGER.info("Starting Cobblemon Economy (Client Init)...");
        EntityRendererRegistry.register(CobblemonEconomy.SHOPKEEPER, ShopkeeperRenderer::new);
        CLIENT_LOGGER.info("Cobblemon Economy (Client Init) - DONE");
    }
}
