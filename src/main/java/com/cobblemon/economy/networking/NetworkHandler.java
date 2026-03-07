package com.cobblemon.economy.networking;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.questboard.QuestBoardService;
import com.cobblemon.economy.quest.QuestService;
import com.cobblemon.economy.storage.QuestNpcConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.level.storage.LevelResource;
import java.io.File;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;

public class NetworkHandler {
    private static boolean registered = false;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        PayloadTypeRegistry.playC2S().register(RequestSkinPayload.TYPE, RequestSkinPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(QuestBoardActionPayload.TYPE, QuestBoardActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ProvideSkinPayload.TYPE, ProvideSkinPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenQuestBoardPayload.TYPE, OpenQuestBoardPayload.CODEC);

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

        ServerPlayNetworking.registerGlobalReceiver(QuestBoardActionPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                    return;
                }

                String boardId = payload.boardId();
                String questId = payload.questId();
                String action = payload.action() == null ? "" : payload.action().toUpperCase();

                QuestNpcConfig config = CobblemonEconomy.getQuestNpcConfig();
                QuestNpcConfig.QuestNpcDefinition board = config != null && config.questNpcs != null ? config.questNpcs.get(boardId) : null;
                if (board == null) {
                    return;
                }

                switch (action) {
                    case "ACCEPT" -> QuestService.acceptQuest(player, boardId, questId, board);
                    case "CLAIM" -> QuestService.claimQuest(player, boardId, questId);
                    case "CANCEL" -> QuestService.cancelQuest(player, boardId, questId);
                    default -> {
                    }
                }

                QuestBoardService.openBoard(player, boardId);
            });
        });
    }
}
