package com.cobblemon.economy.networking;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.questboard.QuestBoardService;
import com.cobblemon.economy.quest.QuestService;
import com.cobblemon.economy.storage.QuestNpcConfig;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;

public class NetworkHandler {
    private static boolean registered = false;
    private static final Pattern SAFE_QUEST_ID = Pattern.compile("[A-Za-z0-9_.:-]{1,128}");

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
                    net.minecraft.server.MinecraftServer server = context.server();
                    Path worldSkins = server.getWorldPath(LevelResource.ROOT)
                            .resolve("config").resolve("cobblemon-economy").resolve("skins");
                    Path globalSkins = FabricLoader.getInstance().getConfigDir()
                            .resolve("cobblemon-economy").resolve("skins");
                    Optional<Path> skinFile = SkinFileResolver.resolve(worldSkins, skinName);
                    if (skinFile.isEmpty()) {
                        skinFile = SkinFileResolver.resolve(globalSkins, skinName);
                    }

                    if (skinFile.isPresent()) {
                        byte[] data = Files.readAllBytes(skinFile.get());
                        context.responseSender().sendPacket(new ProvideSkinPayload(skinName, data));
                    } else {
                        CobblemonEconomy.LOGGER.warn("Rejected or missing skin request");
                        context.responseSender().sendPacket(new ProvideSkinPayload(skinName, new byte[0]));
                    }
                } catch (Exception e) {
                    CobblemonEconomy.LOGGER.error("Error serving requested skin", e);
                    context.responseSender().sendPacket(new ProvideSkinPayload(skinName, new byte[0]));
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

                if (!SAFE_QUEST_ID.matcher(boardId == null ? "" : boardId).matches()
                        || !SAFE_QUEST_ID.matcher(questId == null ? "" : questId).matches()
                        || !QuestBoardService.validateActionSession(player, boardId)) {
                    return;
                }

                QuestNpcConfig config = CobblemonEconomy.getQuestNpcConfig();
                QuestNpcConfig.QuestNpcDefinition board = config != null && config.questNpcs != null ? config.questNpcs.get(boardId) : null;
                if (board == null) {
                    return;
                }

                switch (action) {
                    case "ACCEPT" -> QuestService.acceptQuest(player, boardId, questId, board);
                    case "CLAIM" -> QuestService.claimQuest(player, boardId, questId);
                    case "CANCEL" -> QuestService.cancelQuest(player, boardId, questId);
                    case "REFRESH" -> {
                    }
                    default -> {
                    }
                }

                QuestBoardService.openBoard(player, boardId);
            });
        });
    }
}
