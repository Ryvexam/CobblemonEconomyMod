package com.cobblemon.economy.networking;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VersionHandshake {
    private static final int VERSION_TIMEOUT_TICKS = 60;
    private static final Map<UUID, Integer> PENDING_VERSION_CHECK = new ConcurrentHashMap<>();

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(VersionCheckPayload.TYPE, (payload, context) -> {
            String clientVersion = payload.version();
            String serverVersion = getModVersion();
            PENDING_VERSION_CHECK.remove(context.player().getUUID());
            if (!clientVersion.equals(serverVersion)) {
                context.server().execute(() -> context.player().connection.disconnect(
                        Component.literal("Cobblemon Economy version mismatch. Server: " + serverVersion + " Client: " + clientVersion)
                ));
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            PENDING_VERSION_CHECK.put(handler.getPlayer().getUUID(), 0);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PENDING_VERSION_CHECK.remove(handler.getPlayer().getUUID());
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PENDING_VERSION_CHECK.isEmpty()) {
                return;
            }

            Iterator<Map.Entry<UUID, Integer>> iterator = PENDING_VERSION_CHECK.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Integer> entry = iterator.next();
                int ticks = entry.getValue() + 1;
                if (ticks >= VERSION_TIMEOUT_TICKS) {
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                    if (player != null) {
                        player.connection.disconnect(Component.literal("Cobblemon Economy mod missing or incompatible."));
                    }
                    iterator.remove();
                } else {
                    entry.setValue(ticks);
                }
            }
        });
    }

    public static String getModVersion() {
        return FabricLoader.getInstance()
                .getModContainer(CobblemonEconomy.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
