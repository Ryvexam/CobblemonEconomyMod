package com.cobblemon.economy.quest;

import com.cobblemon.economy.entity.ShopkeeperEntity;
import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.storage.QuestConfig;
import com.cobblemon.economy.storage.QuestNpcConfig;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class QuestGui {
    private QuestGui() {
    }

    public static void open(ServerPlayer player, ShopkeeperEntity npc) {
        String npcId = npc.getQuestNpcId();
        QuestNpcConfig config = CobblemonEconomy.getQuestNpcConfig();
        if (config == null || npcId == null || npcId.isBlank()) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.npc_not_configured").withStyle(ChatFormatting.RED));
            return;
        }

        QuestNpcConfig.QuestNpcDefinition npcDefinition = config.questNpcs.get(npcId);
        if (npcDefinition == null) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.npc_not_configured").withStyle(ChatFormatting.RED));
            return;
        }

        List<QuestService.QuestSnapshot> snapshots = QuestService.getQuestSnapshots(player, npcId, npcDefinition);
        sendDialogue(player, npcDefinition, snapshots);

        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
        gui.setTitle(Component.literal("\uF804\uE000")
                .withStyle(style -> style.withFont(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "default"))));

        int slot = 10;
        for (QuestService.QuestSnapshot snapshot : snapshots) {
            if (slot > 16) {
                break;
            }
            gui.setSlot(slot, buildQuestElement(player, npcId, snapshot, npcDefinition));
            slot++;
        }

        gui.open();
    }

    private static GuiElementBuilder buildQuestElement(ServerPlayer player,
                                                       String npcId,
                                                       QuestService.QuestSnapshot snapshot,
                                                       QuestNpcConfig.QuestNpcDefinition npcDefinition) {
        ChatFormatting statusColor = statusColor(snapshot.status);
        String statusKey = "cobblemon-economy.quest.status." + snapshot.status.name().toLowerCase(Locale.ROOT);

        GuiElementBuilder builder = new GuiElementBuilder(statusIcon(snapshot.status))
                .setName(Component.literal(snapshot.definition.name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .addLoreLine(Component.translatable("cobblemon-economy.quest.status_label")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.translatable(statusKey).withStyle(statusColor)))
                .addLoreLine(Component.translatable("cobblemon-economy.quest.progress")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(QuestService.renderProgressSummary(snapshot.definition, snapshot.progress))
                                .withStyle(ChatFormatting.WHITE)));

        if (snapshot.definition.objectives != null) {
            for (int i = 0; i < snapshot.definition.objectives.size(); i++) {
                QuestConfig.CaptureObjective objective = snapshot.definition.objectives.get(i);
                if (objective == null) {
                    continue;
                }
                int current = i < snapshot.progress.size() ? snapshot.progress.get(i) : 0;
                int target = objective.count;
                builder.addLoreLine(Component.literal("- " + objectiveLabel(objective) + " " + current + "/" + target)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        builder.addLoreLine(Component.empty());
        builder.addLoreLine(Component.translatable(actionKey(snapshot.status)).withStyle(ChatFormatting.YELLOW));

        builder.setCallback((index, type, action) -> {
            if (snapshot.status == QuestService.QuestDisplayStatus.AVAILABLE) {
                QuestService.acceptQuest(player, npcId, snapshot.questId, npcDefinition);
                ShopkeeperEntity npc = resolveNpc(player, npcId);
                if (npc != null) {
                    open(player, npc);
                }
            } else if (snapshot.status == QuestService.QuestDisplayStatus.CLAIMABLE) {
                QuestService.claimQuest(player, npcId, snapshot.questId);
                ShopkeeperEntity npc = resolveNpc(player, npcId);
                if (npc != null) {
                    open(player, npc);
                }
            }
        });

        return builder;
    }

    private static String actionKey(QuestService.QuestDisplayStatus status) {
        return switch (status) {
            case AVAILABLE -> "cobblemon-economy.quest.action.accept";
            case CLAIMABLE -> "cobblemon-economy.quest.action.claim";
            case ACTIVE -> "cobblemon-economy.quest.action.in_progress";
            case ON_COOLDOWN -> "cobblemon-economy.quest.action.cooldown";
            case LOCKED -> "cobblemon-economy.quest.action.locked";
            default -> "cobblemon-economy.quest.action.done";
        };
    }

    private static ShopkeeperEntity resolveNpc(ServerPlayer player, String npcId) {
        List<ShopkeeperEntity> entities = player.serverLevel().getEntitiesOfClass(ShopkeeperEntity.class, player.getBoundingBox().inflate(16));
        for (ShopkeeperEntity candidate : entities) {
            if (npcId.equals(candidate.getQuestNpcId())) {
                return candidate;
            }
        }
        return entities.isEmpty() ? null : entities.get(0);
    }

    private static void sendDialogue(ServerPlayer player, QuestNpcConfig.QuestNpcDefinition npcDefinition, List<QuestService.QuestSnapshot> snapshots) {
        List<String> lines;
        boolean hasClaimable = snapshots.stream().anyMatch(q -> q.status == QuestService.QuestDisplayStatus.CLAIMABLE);
        boolean hasActive = snapshots.stream().anyMatch(q -> q.status == QuestService.QuestDisplayStatus.ACTIVE);
        boolean hasAvailable = snapshots.stream().anyMatch(q -> q.status == QuestService.QuestDisplayStatus.AVAILABLE);

        if (hasClaimable) {
            lines = npcDefinition.dialogues.readyToClaim;
        } else if (hasActive) {
            lines = npcDefinition.dialogues.inProgress;
        } else if (hasAvailable) {
            lines = npcDefinition.dialogues.greeting;
        } else {
            lines = npcDefinition.dialogues.completed;
            if (lines == null || lines.isEmpty()) {
                lines = npcDefinition.dialogues.cooldown;
            }
        }

        if (lines == null || lines.isEmpty()) {
            return;
        }

        String selected = lines.get(new Random().nextInt(lines.size()));
        String progress = "";
        for (QuestService.QuestSnapshot snapshot : snapshots) {
            if (snapshot.status == QuestService.QuestDisplayStatus.ACTIVE || snapshot.status == QuestService.QuestDisplayStatus.CLAIMABLE) {
                progress = snapshot.definition.name + " " + QuestService.renderProgressSummary(snapshot.definition, snapshot.progress);
                break;
            }
        }

        String rendered = selected
                .replace("%player%", player.getGameProfile().getName())
                .replace("%progress%", progress);
        player.sendSystemMessage(Component.literal(rendered).withStyle(ChatFormatting.GRAY));
    }

    private static net.minecraft.world.item.Item statusIcon(QuestService.QuestDisplayStatus status) {
        return switch (status) {
            case AVAILABLE -> Items.WRITABLE_BOOK;
            case ACTIVE -> Items.CLOCK;
            case CLAIMABLE -> Items.CHEST;
            case COMPLETED -> Items.EMERALD;
            case ON_COOLDOWN -> Items.BARRIER;
            case LOCKED -> Items.IRON_BARS;
        };
    }

    private static ChatFormatting statusColor(QuestService.QuestDisplayStatus status) {
        return switch (status) {
            case AVAILABLE -> ChatFormatting.GREEN;
            case ACTIVE -> ChatFormatting.YELLOW;
            case CLAIMABLE -> ChatFormatting.GOLD;
            case COMPLETED -> ChatFormatting.AQUA;
            case ON_COOLDOWN -> ChatFormatting.RED;
            case LOCKED -> ChatFormatting.DARK_GRAY;
        };
    }

    private static String objectiveLabel(QuestConfig.CaptureObjective objective) {
        if (objective.type != null) {
            String type = objective.type.toLowerCase(Locale.ROOT);
            if ("raid_win".equals(type)) {
                return "raid victories";
            }
            if ("battle_win".equals(type)) {
                return "battle victories";
            }
            if ("tower_win".equals(type)) {
                return "tower victories";
            }
        }

        List<String> parts = new ArrayList<>();
        if (objective.species != null && !objective.species.isEmpty()) {
            parts.add("species");
        }
        if (objective.types != null && !objective.types.isEmpty()) {
            parts.add("type");
        }
        if (objective.labels != null && !objective.labels.isEmpty()) {
            parts.add("label");
        }
        if (objective.pokeball != null && !objective.pokeball.isEmpty()) {
            parts.add("ball");
        }
        if (objective.dimension != null && !objective.dimension.isBlank()) {
            parts.add("dimension");
        }
        if (objective.shiny != null && objective.shiny) {
            parts.add("shiny");
        }
        if (parts.isEmpty()) {
            return "capture";
        }
        return String.join(", ", parts);
    }
}
