package com.cobblemon.economy.networking;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.level.storage.LevelResource;
import java.io.File;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;

public class NetworkHandler {
    public static void register() {
        PayloadTypeRegistry.playC2S().register(RequestSkinPayload.TYPE, RequestSkinPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ProvideSkinPayload.TYPE, ProvideSkinPayload.CODEC);

        // Server-side handler: Receive Request -> Send Data
        ServerPlayNetworking.registerGlobalReceiver(RequestSkinPayload.TYPE, (payload, context) -> {
            String skinName = payload.skinName();
            context.server().execute(() -> {
                try {
                    // Locate the file on the server
                    net.minecraft.server.MinecraftServer server = context.server();
                    
                    // 1. Try World-specific config
                    File modDir = server.getWorldPath(LevelResource.ROOT).resolve("config").resolve("cobblemon-economy").toFile();
                    File skinFile = new File(modDir, "skins/" + skinName + ".png");

                    // 1b. Check if user provided extension in name
                    if (!skinFile.exists()) {
                         File altFile = new File(modDir, "skins/" + skinName);
                         if (altFile.exists() && skinName.endsWith(".png")) {
                             skinFile = altFile;
                         }
                    }

                    // 2. Try Global config if not found
                    if (!skinFile.exists()) {
                        File globalDir = FabricLoader.getInstance().getConfigDir().resolve("cobblemon-economy").toFile();
                        skinFile = new File(globalDir, "skins/" + skinName + ".png");
                        
                        if (!skinFile.exists()) {
                            File altFile = new File(globalDir, "skins/" + skinName);
                            if (altFile.exists() && skinName.endsWith(".png")) {
                                skinFile = altFile;
                            }
                        }
                    }

                    if (skinFile.exists()) {
                        CobblemonEconomy.LOGGER.info("Found skin file at: {}", skinFile.getAbsolutePath());
                        byte[] data = Files.readAllBytes(skinFile.toPath());
                        context.responseSender().sendPacket(new ProvideSkinPayload(skinName, data));
                    } else {
                        CobblemonEconomy.LOGGER.warn("Skin file not found. Checked: {}", skinFile.getAbsolutePath());
                        context.responseSender().sendPacket(new ProvideSkinPayload(skinName, new byte[0]));
                    }
                } catch (Exception e) {
                    CobblemonEconomy.LOGGER.error("Error sending skin " + skinName, e);
                }
            });
        });
    }
}
