package com.cobblemon.economy.quest;

import com.cobblemon.economy.entity.ShopkeeperEntity;
import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.storage.QuestConfig;
import com.cobblemon.economy.storage.QuestNpcConfig;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class QuestGui {
    private static final long DIALOGUE_COOLDOWN_MS = 3000L;
    private static final long CANCEL_CONFIRM_WINDOW_MS = 5000L;
    private static final Map<String, Long> LAST_DIALOGUE_BY_PLAYER_NPC = new ConcurrentHashMap<>();
    private static final Map<String, Long> PENDING_CANCEL_CONFIRM = new ConcurrentHashMap<>();

    private QuestGui() {
    }

    public static void open(ServerPlayer player, ShopkeeperEntity npc) {
        open(player, npc, true);
    }

    private static void open(ServerPlayer player, ShopkeeperEntity npc, boolean allowDialogue) {
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
        if (allowDialogue) {
            sendDialogue(player, npcId, npcDefinition, snapshots);
        }
        int activeCount = (int) snapshots.stream().filter(s -> s.status == QuestService.QuestDisplayStatus.ACTIVE).count();

        SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x6, player, false);

        String negativeSpace = "\uF804".repeat(21);
        gui.setTitle(Component.literal("\uF804\uE000")
                .withStyle(style -> style.withFont(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "default")).withColor(0xFFFFFF))
                .append(Component.literal(negativeSpace)
                        .withStyle(style -> style.withFont(ResourceLocation.fromNamespaceAndPath(CobblemonEconomy.MOD_ID, "default"))))
                .append(Component.literal("Quest Board")
                        .withStyle(style -> style.withFont(ResourceLocation.withDefaultNamespace("default")))));

        for (int i = 0; i < 54; i++) {
            gui.setSlot(i, new GuiElementBuilder(Items.AIR));
        }

        gui.setSlot(4, new GuiElementBuilder(Items.BOOK)
                .setName(Component.translatable("cobblemon-economy.quest.gui.npc", npcDefinition.displayName).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .addLoreLine(Component.translatable("cobblemon-economy.quest.gui.daily_refresh", formatDuration(QuestService.getMillisUntilNextRotation(npcDefinition))).withStyle(ChatFormatting.AQUA))
                .addLoreLine(Component.translatable("cobblemon-economy.quest.gui.active_slots", activeCount, npcDefinition.maxActive).withStyle(ChatFormatting.GRAY))
                .addLoreLine(Component.translatable("cobblemon-economy.quest.gui.tip").withStyle(ChatFormatting.DARK_GRAY))
        );

        gui.setSlot(49, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setSkullOwner(player.getGameProfile(), player.server)
                .setName(Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                .addLoreLine(Component.translatable("cobblemon-economy.quest.gui.daily_limit", Math.max(1, npcDefinition.visibleQuests)).withStyle(ChatFormatting.GRAY))
                .addLoreLine(Component.translatable("cobblemon-economy.quest.gui.cancel_fee", QuestService.CANCEL_FEE.toPlainString()).withStyle(ChatFormatting.DARK_GRAY))
        );

        int[] questSlots = {20, 21, 23, 24};
        for (int i = 0; i < questSlots.length && i < snapshots.size(); i++) {
            gui.setSlot(questSlots[i], buildQuestElement(player, npcId, snapshots.get(i), npcDefinition));
        }

        for (int i = snapshots.size(); i < questSlots.length; i++) {
            gui.setSlot(questSlots[i], new GuiElementBuilder(Items.GRAY_STAINED_GLASS_PANE)
                    .setName(Component.translatable("cobblemon-economy.quest.gui.empty_slot").withStyle(ChatFormatting.DARK_GRAY))
                    .addLoreLine(Component.translatable("cobblemon-economy.quest.gui.empty_slot_lore", formatDuration(QuestService.getMillisUntilNextRotation(npcDefinition))).withStyle(ChatFormatting.GRAY))
            );
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
                .addLoreLine(Component.translatable("cobblemon-economy.quest.gui.id", snapshot.questId).withStyle(ChatFormatting.DARK_GRAY))
                .addLoreLine(Component.translatable("cobblemon-economy.quest.status_label")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.translatable(statusKey).withStyle(statusColor)))
                .addLoreLine(Component.translatable("cobblemon-economy.quest.progress")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(QuestService.renderProgressSummary(snapshot.definition, snapshot.progress))
                                .withStyle(ChatFormatting.WHITE)));

        if (snapshot.definition.timeLimitMinutes != null && snapshot.definition.timeLimitMinutes > 0) {
            long hours = Math.max(1, snapshot.definition.timeLimitMinutes / 60L);
            builder.addLoreLine(Component.translatable("cobblemon-economy.quest.time_limit", hours).withStyle(ChatFormatting.GRAY));
        }

        if (snapshot.status == QuestService.QuestDisplayStatus.ACTIVE && snapshot.state != null) {
            long remaining = QuestService.getRemainingActiveMillis(snapshot.definition, snapshot.state);
            builder.addLoreLine(Component.translatable("cobblemon-economy.quest.time_remaining", formatDuration(remaining)).withStyle(ChatFormatting.RED));
        }
        if (snapshot.status == QuestService.QuestDisplayStatus.ON_COOLDOWN && snapshot.state != null) {
            long remaining = QuestService.getCooldownRemainingMillis(snapshot.state);
            builder.addLoreLine(Component.translatable("cobblemon-economy.quest.cooldown_remaining", formatDuration(remaining)).withStyle(ChatFormatting.RED));
        }

        if (snapshot.definition.objectives != null) {
            for (int i = 0; i < snapshot.definition.objectives.size(); i++) {
                QuestConfig.CaptureObjective objective = snapshot.definition.objectives.get(i);
                if (objective == null) {
                    continue;
                }
                int current = i < snapshot.progress.size() ? snapshot.progress.get(i) : 0;
                int target = objective.count;
                builder.addLoreLine(Component.literal("- ")
                        .append(describeObjective(objective))
                        .append(Component.literal(" " + current + "/" + target))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        if (snapshot.definition.rewards != null) {
            builder.addLoreLine(Component.empty());
            builder.addLoreLine(Component.translatable("cobblemon-economy.quest.rewards_title").withStyle(ChatFormatting.AQUA));
            if (snapshot.definition.rewards.pokedollars != null && snapshot.definition.rewards.pokedollars.signum() > 0) {
                builder.addLoreLine(Component.translatable("cobblemon-economy.quest.reward.pokedollars", snapshot.definition.rewards.pokedollars).withStyle(ChatFormatting.GRAY));
            }
            if (snapshot.definition.rewards.pco != null && snapshot.definition.rewards.pco.signum() > 0) {
                builder.addLoreLine(Component.translatable("cobblemon-economy.quest.reward.pco", snapshot.definition.rewards.pco).withStyle(ChatFormatting.GRAY));
            }
            if (snapshot.definition.rewards.commands != null && !snapshot.definition.rewards.commands.isEmpty()) {
                builder.addLoreLine(Component.translatable("cobblemon-economy.quest.reward.commands").withStyle(ChatFormatting.GRAY));
            }
        }

        builder.addLoreLine(Component.empty());
        builder.addLoreLine(Component.translatable(actionKey(snapshot.status)).withStyle(ChatFormatting.YELLOW));
        if (snapshot.status == QuestService.QuestDisplayStatus.ACTIVE) {
            builder.addLoreLine(Component.translatable("cobblemon-economy.quest.action.cancel_paid", QuestService.CANCEL_FEE.toPlainString()).withStyle(ChatFormatting.RED));
        }

        builder.setCallback((index, type, action) -> {
            if (snapshot.status == QuestService.QuestDisplayStatus.AVAILABLE) {
                QuestService.acceptQuest(player, npcId, snapshot.questId, npcDefinition);
                ShopkeeperEntity npc = resolveNpc(player, npcId);
                if (npc != null) {
                    open(player, npc, false);
                }
            } else if (snapshot.status == QuestService.QuestDisplayStatus.CLAIMABLE) {
                QuestService.claimQuest(player, npcId, snapshot.questId);
                ShopkeeperEntity npc = resolveNpc(player, npcId);
                if (npc != null) {
                    open(player, npc, false);
                }
            } else if (snapshot.status == QuestService.QuestDisplayStatus.ACTIVE) {
                if (type.isRight) {
                    if (isCancelConfirmed(player.getUUID(), npcId, snapshot.questId)) {
                        QuestService.cancelQuestWithFee(player, npcId, snapshot.questId);
                        ShopkeeperEntity npc = resolveNpc(player, npcId);
                        if (npc != null) {
                            open(player, npc, false);
                        }
                    } else {
                        armCancelConfirm(player.getUUID(), npcId, snapshot.questId);
                        player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.cancel_confirm", QuestService.CANCEL_FEE.toPlainString()).withStyle(ChatFormatting.RED));
                    }
                } else {
                    player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.cannot_cancel_free").withStyle(ChatFormatting.RED));
                }
            } else {
                player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.not_available").withStyle(ChatFormatting.RED));
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

    private static void sendDialogue(ServerPlayer player, String npcId, QuestNpcConfig.QuestNpcDefinition npcDefinition, List<QuestService.QuestSnapshot> snapshots) {
        String key = player.getUUID() + ":" + npcId;
        long now = System.currentTimeMillis();
        Long last = LAST_DIALOGUE_BY_PLAYER_NPC.get(key);
        if (last != null && now - last < DIALOGUE_COOLDOWN_MS) {
            return;
        }
        LAST_DIALOGUE_BY_PLAYER_NPC.put(key, now);

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

    private static Component describeObjective(QuestConfig.CaptureObjective objective) {
        if (objective.type != null) {
            String type = objective.type.toLowerCase(Locale.ROOT);
            if ("raid_win".equals(type)) {
                return Component.translatable("cobblemon-economy.quest.objective.raid_win");
            }
            if ("battle_win".equals(type)) {
                return Component.translatable("cobblemon-economy.quest.objective.battle_win");
            }
            if ("tower_win".equals(type)) {
                return Component.translatable("cobblemon-economy.quest.objective.tower_win");
            }
            if ("fossil_revive".equals(type)) {
                return withCaptureFilters(Component.translatable("cobblemon-economy.quest.objective.fossil_revive"), objective, false);
            }
        }

        MutableComponent base = Component.translatable("cobblemon-economy.quest.objective.capture");
        if (objective.species != null && !objective.species.isEmpty()) {
            String species = objective.species.stream().map(QuestGui::shortId).collect(Collectors.joining(" or "));
            base = Component.translatable("cobblemon-economy.quest.objective.capture_species", species);
        }
        return withCaptureFilters(base, objective, true);
    }

    private static Component withCaptureFilters(Component base, QuestConfig.CaptureObjective objective, boolean includeBall) {
        List<Component> parts = new ArrayList<>();
        if (objective.types != null && !objective.types.isEmpty()) {
            parts.add(Component.translatable("cobblemon-economy.quest.objective.filter.type", joinPretty(objective.types)));
        }
        if (objective.labels != null && !objective.labels.isEmpty()) {
            parts.add(Component.translatable("cobblemon-economy.quest.objective.filter.trait", joinPretty(objective.labels)));
        }
        if (includeBall && objective.pokeball != null && !objective.pokeball.isEmpty()) {
            parts.add(Component.translatable("cobblemon-economy.quest.objective.filter.ball", joinPretty(objective.pokeball)));
        }
        if (objective.dimensions != null && !objective.dimensions.isEmpty()) {
            parts.add(Component.translatable("cobblemon-economy.quest.objective.filter.location",
                    objective.dimensions.stream().map(QuestGui::prettyDimensionText).collect(Collectors.joining(" or "))));
        } else if (objective.dimension != null && !objective.dimension.isBlank()) {
            parts.add(Component.translatable("cobblemon-economy.quest.objective.filter.location", prettyDimensionText(objective.dimension)));
        }
        if (objective.shiny != null && objective.shiny) {
            parts.add(Component.translatable("cobblemon-economy.quest.objective.filter.variant_shiny"));
        }
        if (parts.isEmpty()) {
            return base;
        }

        MutableComponent out = Component.empty().append(base).append(Component.literal(" ["));
        for (int i = 0; i < parts.size(); i++) {
            out.append(parts.get(i));
            if (i < parts.size() - 1) {
                out.append(Component.literal(" | "));
            }
        }
        out.append(Component.literal("]"));
        return out;
    }

    private static String joinPretty(List<String> values) {
        return values.stream().map(QuestGui::shortId).collect(Collectors.joining(" or "));
    }

    private static String shortId(String value) {
        if (value == null || value.isBlank()) return value;
        int idx = value.indexOf(':');
        if (idx >= 0 && idx + 1 < value.length()) {
            return value.substring(idx + 1).replace('_', ' ');
        }
        return value.replace('_', ' ');
    }

    private static String prettyDimensionText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if ("minecraft:overworld".equals(lower)) {
            return "Overworld";
        }
        if ("minecraft:the_nether".equals(lower)) {
            return "Nether";
        }
        if ("minecraft:the_end".equals(lower)) {
            return "The End";
        }
        return shortId(value);
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            return String.format("%dh %02dm", hours, minutes);
        }
        if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        }
        return String.format("%ds", seconds);
    }

    private static Item resolveItem(String id, Item fallback) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) {
            return fallback;
        }
        Item item = BuiltInRegistries.ITEM.get(rl);
        return item == null || item == Items.AIR ? fallback : item;
    }

    private static String cancelKey(UUID playerUuid, String npcId, String questId) {
        return playerUuid + ":" + npcId + ":" + questId;
    }

    private static void armCancelConfirm(UUID playerUuid, String npcId, String questId) {
        PENDING_CANCEL_CONFIRM.put(cancelKey(playerUuid, npcId, questId), System.currentTimeMillis());
    }

    private static boolean isCancelConfirmed(UUID playerUuid, String npcId, String questId) {
        String key = cancelKey(playerUuid, npcId, questId);
        Long firstClick = PENDING_CANCEL_CONFIRM.get(key);
        if (firstClick == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - firstClick > CANCEL_CONFIRM_WINDOW_MS) {
            PENDING_CANCEL_CONFIRM.remove(key);
            return false;
        }
        PENDING_CANCEL_CONFIRM.remove(key);
        return true;
    }
}
