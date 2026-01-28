package com.cobblemon.economy.client;

import com.cobblemon.economy.networking.VersionCheckPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class ClientVersionHandshake {
    public static void registerClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String version = FabricLoader.getInstance()
                    .getModContainer("cobblemon-economy")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("unknown");
            CobblemonEconomyClient.CLIENT_LOGGER.info("Sending version check to server: '{}'", version);
            ClientPlayNetworking.send(new VersionCheckPayload(version));
        });
    }
}
