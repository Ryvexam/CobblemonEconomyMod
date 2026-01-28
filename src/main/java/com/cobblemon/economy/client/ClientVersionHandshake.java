package com.cobblemon.economy.client;

import com.cobblemon.economy.networking.VersionCheckPayload;
import com.cobblemon.economy.networking.VersionHandshake;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientVersionHandshake {
    public static void registerClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientPlayNetworking.send(new VersionCheckPayload(VersionHandshake.getModVersion()));
        });
    }
}
