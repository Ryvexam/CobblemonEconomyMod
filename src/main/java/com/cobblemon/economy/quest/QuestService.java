package com.cobblemon.economy.quest;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.storage.QuestConfig;
import com.cobblemon.economy.storage.QuestNpcConfig;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class QuestService {
    public static final BigDecimal CANCEL_FEE = new BigDecimal("1000");
    public static final int DEFAULT_VISIBLE_QUESTS = 4;

    private QuestService() {
    }

    public enum QuestDisplayStatus {
        AVAILABLE,
        ACTIVE,
        COMPLETED,
        CLAIMABLE,
        ON_COOLDOWN,
        LOCKED
    }

    public static class QuestSnapshot {
        public final String questId;
        public final QuestConfig.QuestDefinition definition;
        public final QuestManager.QuestState state;
        public final QuestDisplayStatus status;
        public final List<Integer> progress;

        public QuestSnapshot(String questId, QuestConfig.QuestDefinition definition, QuestManager.QuestState state, QuestDisplayStatus status, List<Integer> progress) {
            this.questId = questId;
            this.definition = definition;
            this.state = state;
            this.status = status;
            this.progress = progress;
        }
    }

    public static List<QuestSnapshot> getQuestSnapshots(ServerPlayer player, String npcId, QuestNpcConfig.QuestNpcDefinition npcDefinition) {
        QuestConfig questConfig = CobblemonEconomy.getQuestConfig();
        QuestManager manager = CobblemonEconomy.getQuestManager();
        if (questConfig == null || manager == null || npcDefinition == null) {
            return List.of();
        }

        List<String> ongoing = new ArrayList<>();
        for (String questId : npcDefinition.questPool) {
            QuestConfig.QuestDefinition def = questConfig.quests.get(questId);
            if (def == null) {
                continue;
            }
            QuestManager.QuestState state = manager.getQuestState(player.getUUID(), npcId, questId);
            if (isExpired(def, state)) {
                manager.cancelQuest(player.getUUID(), npcId, questId, nextRotationBoundaryMillis(npcDefinition));
                player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.expired", def.name).withStyle(ChatFormatting.RED));
                state = null;
            }
            List<Integer> progress = getProgressForQuest(player.getUUID(), npcId, questId, def);
            QuestDisplayStatus status = resolveStatus(def, state, progress);
            if (status == QuestDisplayStatus.ACTIVE || status == QuestDisplayStatus.CLAIMABLE) {
                ongoing.add(questId);
            }
        }

        int visibleLimit = Math.max(1, npcDefinition.visibleQuests);
        LinkedHashSet<String> selectedIds = new LinkedHashSet<>();
        selectedIds.addAll(ongoing);
        for (String dailyId : getDailyQuestIds(player.getUUID(), npcId, npcDefinition)) {
            if (selectedIds.size() >= visibleLimit) {
                break;
            }
            selectedIds.add(dailyId);
        }

        List<QuestSnapshot> snapshots = new ArrayList<>();
        for (String questId : selectedIds) {
            if (snapshots.size() >= visibleLimit) {
                break;
            }
            QuestConfig.QuestDefinition def = questConfig.quests.get(questId);
            if (def == null) {
                continue;
            }
            QuestManager.QuestState state = manager.getQuestState(player.getUUID(), npcId, questId);
            if (isExpired(def, state)) {
                manager.cancelQuest(player.getUUID(), npcId, questId, nextRotationBoundaryMillis(npcDefinition));
                player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.expired", def.name).withStyle(ChatFormatting.RED));
                state = null;
            }
            List<Integer> progress = getProgressForQuest(player.getUUID(), npcId, questId, def);
            QuestDisplayStatus status = resolveStatus(def, state, progress);
            snapshots.add(new QuestSnapshot(questId, def, state, status, progress));
        }
        return snapshots;
    }

    public static boolean acceptQuest(ServerPlayer player, String npcId, String questId, QuestNpcConfig.QuestNpcDefinition npcDefinition) {
        QuestConfig questConfig = CobblemonEconomy.getQuestConfig();
        QuestManager manager = CobblemonEconomy.getQuestManager();
        if (questConfig == null || manager == null || npcDefinition == null) {
            return false;
        }

        QuestConfig.QuestDefinition quest = questConfig.quests.get(questId);
        if (quest == null || quest.objectives == null || quest.objectives.isEmpty()) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.not_available").withStyle(ChatFormatting.RED));
            return false;
        }

        List<String> dailyQuestIds = getDailyQuestIds(player.getUUID(), npcId, npcDefinition);
        if (!dailyQuestIds.contains(questId)) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.daily_pool_locked").withStyle(ChatFormatting.RED));
            return false;
        }

        QuestManager.QuestState state = manager.getQuestState(player.getUUID(), npcId, questId);
        if (isExpired(quest, state)) {
            manager.cancelQuest(player.getUUID(), npcId, questId, nextRotationBoundaryMillis(npcDefinition));
            state = null;
        }
        List<Integer> progress = getProgressForQuest(player.getUUID(), npcId, questId, quest);
        QuestDisplayStatus displayStatus = resolveStatus(quest, state, progress);
        if (!hasPrerequisites(player.getUUID(), npcId, quest)) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.prerequisite_missing").withStyle(ChatFormatting.RED));
            return false;
        }
        if (displayStatus != QuestDisplayStatus.AVAILABLE) {
            if (displayStatus == QuestDisplayStatus.ON_COOLDOWN) {
                player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.on_cooldown").withStyle(ChatFormatting.RED));
            } else if (displayStatus == QuestDisplayStatus.LOCKED) {
                player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.locked").withStyle(ChatFormatting.RED));
            } else {
                player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.already_active").withStyle(ChatFormatting.RED));
            }
            return false;
        }

        int activeCount = manager.countActiveQuests(player.getUUID(), npcId);
        if (activeCount >= npcDefinition.maxActive) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.too_many_active", npcDefinition.maxActive).withStyle(ChatFormatting.RED));
            return false;
        }

        boolean accepted = manager.acceptQuest(player.getUUID(), npcId, questId, quest.objectives.size());
        if (accepted) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.accepted", quest.name).withStyle(ChatFormatting.GREEN));
        }
        return accepted;
    }

    public static boolean claimQuest(ServerPlayer player, String npcId, String questId) {
        QuestConfig questConfig = CobblemonEconomy.getQuestConfig();
        QuestManager manager = CobblemonEconomy.getQuestManager();
        if (questConfig == null || manager == null) {
            return false;
        }

        QuestConfig.QuestDefinition quest = questConfig.quests.get(questId);
        if (quest == null) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.not_available").withStyle(ChatFormatting.RED));
            return false;
        }

        QuestManager.QuestState state = manager.getQuestState(player.getUUID(), npcId, questId);
        if (state == null || !"COMPLETED".equalsIgnoreCase(state.status)) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.claim_not_ready").withStyle(ChatFormatting.RED));
            return false;
        }

        giveRewards(player, quest);
        QuestNpcConfig.QuestNpcDefinition npcDefinition = CobblemonEconomy.getQuestNpcConfig() != null
                ? CobblemonEconomy.getQuestNpcConfig().questNpcs.get(npcId)
                : null;
        manager.markQuestClaimed(player.getUUID(), npcId, questId, computeNextAvailability(quest, npcDefinition));
        player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.claimed", quest.name).withStyle(ChatFormatting.GOLD));
        return true;
    }

    public static boolean cancelQuestWithFee(ServerPlayer player, String npcId, String questId) {
        QuestConfig questConfig = CobblemonEconomy.getQuestConfig();
        QuestManager manager = CobblemonEconomy.getQuestManager();
        if (questConfig == null || manager == null) {
            return false;
        }

        QuestConfig.QuestDefinition quest = questConfig.quests.get(questId);
        if (quest == null) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.not_available").withStyle(ChatFormatting.RED));
            return false;
        }

        QuestManager.QuestState state = manager.getQuestState(player.getUUID(), npcId, questId);
        if (state == null || !"ACTIVE".equalsIgnoreCase(state.status)) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.cancel_only_active").withStyle(ChatFormatting.RED));
            return false;
        }

        if (!CobblemonEconomy.getEconomyManager().subtractBalance(player.getUUID(), CANCEL_FEE)) {
            player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.cancel_not_enough", CANCEL_FEE.toPlainString()).withStyle(ChatFormatting.RED));
            return false;
        }

        QuestNpcConfig.QuestNpcDefinition npcDefinition = getNpcDefinition(npcId);
        manager.cancelQuest(player.getUUID(), npcId, questId, nextRotationBoundaryMillis(npcDefinition));
        player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.cancelled_paid", quest.name, CANCEL_FEE.toPlainString()).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.cancelled_wait_rotation").withStyle(ChatFormatting.RED));
        return true;
    }

    public static void handleCapture(PokemonCapturedEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }

        QuestConfig questConfig = CobblemonEconomy.getQuestConfig();
        QuestManager manager = CobblemonEconomy.getQuestManager();
        if (questConfig == null || manager == null) {
            return;
        }

        List<QuestManager.QuestState> active = manager.getAllActiveQuests(player.getUUID());
        if (active.isEmpty()) {
            return;
        }

        String speciesId = event.getPokemon().getSpecies().getResourceIdentifier().toString().toLowerCase(Locale.ROOT);
        Set<String> pokemonTypes = new HashSet<>();
        Set<String> pokemonLabels = new HashSet<>();
        for (ElementalType type : event.getPokemon().getTypes()) {
            if (type != null && type.getName() != null) {
                pokemonTypes.add(type.getName().toLowerCase(Locale.ROOT));
            }
        }
        if (event.getPokemon().getSpecies().getLabels() != null) {
            for (String label : event.getPokemon().getSpecies().getLabels()) {
                if (label != null && !label.isBlank()) {
                    pokemonLabels.add(label.toLowerCase(Locale.ROOT));
                }
            }
        }
        String pokeballId = event.getPokeBallEntity() != null && event.getPokeBallEntity().getPokeBall() != null
                ? event.getPokeBallEntity().getPokeBall().getName().toString().toLowerCase(Locale.ROOT)
                : "";
        String dimension = player.level().dimension().location().toString().toLowerCase(Locale.ROOT);
        boolean shiny = event.getPokemon().getShiny();

        for (QuestManager.QuestState state : active) {
            QuestConfig.QuestDefinition quest = questConfig.quests.get(state.questId);
            if (quest == null || quest.objectives == null || quest.objectives.isEmpty()) {
                continue;
            }
            if (isExpired(quest, state)) {
                manager.cancelQuest(player.getUUID(), state.npcId, state.questId, nextRotationBoundaryMillis(getNpcDefinition(state.npcId)));
                continue;
            }

            boolean changed = false;
            for (int i = 0; i < quest.objectives.size(); i++) {
                QuestConfig.CaptureObjective objective = quest.objectives.get(i);
                if (objective == null || !"capture".equalsIgnoreCase(objective.type)) {
                    continue;
                }

                int current = manager.getObjectiveProgress(player.getUUID(), state.npcId, state.questId, i);
                if (current >= objective.count) {
                    continue;
                }

                if (!matchesCapture(objective, speciesId, pokemonTypes, pokemonLabels, pokeballId, dimension, shiny)) {
                    continue;
                }

                int next = manager.incrementObjectiveProgress(player.getUUID(), state.npcId, state.questId, i, 1, objective.count);
                if (next != current) {
                    changed = true;
                }
            }

            if (!changed) {
                continue;
            }

            List<Integer> progress = getProgressForQuest(player.getUUID(), state.npcId, state.questId, quest);
            if (isQuestComplete(quest, progress)) {
                manager.markQuestCompleted(player.getUUID(), state.npcId, state.questId);
                player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.completed", quest.name).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            }
        }
    }

    public static void handleBattleVictory(ServerPlayer player, boolean isRaidVictory, boolean isTowerVictory) {
        if (player == null) {
            return;
        }

        QuestConfig questConfig = CobblemonEconomy.getQuestConfig();
        QuestManager manager = CobblemonEconomy.getQuestManager();
        if (questConfig == null || manager == null) {
            return;
        }

        List<QuestManager.QuestState> active = manager.getAllActiveQuests(player.getUUID());
        if (active.isEmpty()) {
            return;
        }

        for (QuestManager.QuestState state : active) {
            QuestConfig.QuestDefinition quest = questConfig.quests.get(state.questId);
            if (quest == null || quest.objectives == null || quest.objectives.isEmpty()) {
                continue;
            }
            if (isExpired(quest, state)) {
                manager.cancelQuest(player.getUUID(), state.npcId, state.questId, nextRotationBoundaryMillis(getNpcDefinition(state.npcId)));
                continue;
            }

            boolean changed = false;
            for (int i = 0; i < quest.objectives.size(); i++) {
                QuestConfig.CaptureObjective objective = quest.objectives.get(i);
                if (objective == null || objective.type == null) {
                    continue;
                }
                String type = objective.type.toLowerCase(Locale.ROOT);
                boolean matches = switch (type) {
                    case "battle_win" -> true;
                    case "raid_win" -> isRaidVictory;
                    case "tower_win" -> isTowerVictory;
                    default -> false;
                };

                if (!matches) {
                    continue;
                }

                int current = manager.getObjectiveProgress(player.getUUID(), state.npcId, state.questId, i);
                if (current >= objective.count) {
                    continue;
                }

                int next = manager.incrementObjectiveProgress(player.getUUID(), state.npcId, state.questId, i, 1, objective.count);
                if (next != current) {
                    changed = true;
                }
            }

            if (!changed) {
                continue;
            }

            List<Integer> progress = getProgressForQuest(player.getUUID(), state.npcId, state.questId, quest);
            if (isQuestComplete(quest, progress)) {
                manager.markQuestCompleted(player.getUUID(), state.npcId, state.questId);
                player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.completed", quest.name).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            }
        }
    }

    public static void handleFossilRevive(ServerPlayer player, Pokemon pokemon) {
        if (player == null || pokemon == null) {
            return;
        }

        QuestConfig questConfig = CobblemonEconomy.getQuestConfig();
        QuestManager manager = CobblemonEconomy.getQuestManager();
        if (questConfig == null || manager == null) {
            return;
        }

        List<QuestManager.QuestState> active = manager.getAllActiveQuests(player.getUUID());
        if (active.isEmpty()) {
            return;
        }

        String speciesId = pokemon.getSpecies().getResourceIdentifier().toString().toLowerCase(Locale.ROOT);
        Set<String> pokemonTypes = new HashSet<>();
        for (ElementalType type : pokemon.getTypes()) {
            if (type != null && type.getName() != null) {
                pokemonTypes.add(type.getName().toLowerCase(Locale.ROOT));
            }
        }
        Set<String> pokemonLabels = new HashSet<>();
        if (pokemon.getSpecies().getLabels() != null) {
            for (String label : pokemon.getSpecies().getLabels()) {
                if (label != null && !label.isBlank()) {
                    pokemonLabels.add(label.toLowerCase(Locale.ROOT));
                }
            }
        }
        String dimension = player.level().dimension().location().toString().toLowerCase(Locale.ROOT);
        boolean shiny = pokemon.getShiny();

        for (QuestManager.QuestState state : active) {
            QuestConfig.QuestDefinition quest = questConfig.quests.get(state.questId);
            if (quest == null || quest.objectives == null || quest.objectives.isEmpty()) {
                continue;
            }
            if (isExpired(quest, state)) {
                manager.cancelQuest(player.getUUID(), state.npcId, state.questId, nextRotationBoundaryMillis(getNpcDefinition(state.npcId)));
                continue;
            }

            boolean changed = false;
            for (int i = 0; i < quest.objectives.size(); i++) {
                QuestConfig.CaptureObjective objective = quest.objectives.get(i);
                if (objective == null || !"fossil_revive".equalsIgnoreCase(objective.type)) {
                    continue;
                }

                int current = manager.getObjectiveProgress(player.getUUID(), state.npcId, state.questId, i);
                if (current >= objective.count) {
                    continue;
                }

                if (!matchesCapture(objective, speciesId, pokemonTypes, pokemonLabels, "", dimension, shiny)) {
                    continue;
                }

                int next = manager.incrementObjectiveProgress(player.getUUID(), state.npcId, state.questId, i, 1, objective.count);
                if (next != current) {
                    changed = true;
                }
            }

            if (!changed) {
                continue;
            }

            List<Integer> progress = getProgressForQuest(player.getUUID(), state.npcId, state.questId, quest);
            if (isQuestComplete(quest, progress)) {
                manager.markQuestCompleted(player.getUUID(), state.npcId, state.questId);
                player.sendSystemMessage(Component.translatable("cobblemon-economy.quest.completed", quest.name).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            }
        }
    }

    public static String renderProgressSummary(QuestConfig.QuestDefinition quest, List<Integer> progress) {
        if (quest == null || quest.objectives == null || quest.objectives.isEmpty()) {
            return "0/0";
        }

        int done = 0;
        for (int i = 0; i < quest.objectives.size(); i++) {
            QuestConfig.CaptureObjective objective = quest.objectives.get(i);
            int value = i < progress.size() ? progress.get(i) : 0;
            if (objective != null && value >= objective.count) {
                done++;
            }
        }
        return done + "/" + quest.objectives.size();
    }

    private static long computeNextAvailability(QuestConfig.QuestDefinition quest, QuestNpcConfig.QuestNpcDefinition npcDefinition) {
        long now = System.currentTimeMillis();
        if (quest == null) {
            return nextRotationBoundaryMillis(npcDefinition);
        }

        if (Boolean.FALSE.equals(quest.repeatable) || "ONCE".equalsIgnoreCase(quest.repeatPolicy)) {
            return Long.MAX_VALUE;
        }

        if (quest.cooldownMinutes != null && quest.cooldownMinutes > 0) {
            return now + quest.cooldownMinutes.longValue() * 60L * 1000L;
        }

        if (quest.repeatPolicy == null || quest.repeatPolicy.isBlank()) {
            return nextRotationBoundaryMillis(npcDefinition);
        }

        return switch (quest.repeatPolicy.toUpperCase(Locale.ROOT)) {
            case "ALWAYS" -> now;
            case "HOURLY" -> now + 60L * 60L * 1000L;
            case "ONCE" -> Long.MAX_VALUE;
            default -> nextRotationBoundaryMillis(npcDefinition);
        };
    }

    private static long nextMidnightMillis() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        return now.plusDays(1).toLocalDate().atStartOfDay(now.getZone()).toInstant().toEpochMilli();
    }

    private static void giveRewards(ServerPlayer player, QuestConfig.QuestDefinition quest) {
        if (quest.rewards == null) {
            return;
        }

        if (quest.rewards.pokedollars != null && quest.rewards.pokedollars.compareTo(BigDecimal.ZERO) > 0) {
            CobblemonEconomy.getEconomyManager().addBalance(player.getUUID(), quest.rewards.pokedollars);
        }
        if (quest.rewards.pco != null && quest.rewards.pco.compareTo(BigDecimal.ZERO) > 0) {
            CobblemonEconomy.getEconomyManager().addPco(player.getUUID(), quest.rewards.pco);
        }
        if (quest.rewards.commands != null) {
            for (String command : quest.rewards.commands) {
                if (command == null || command.isBlank()) {
                    continue;
                }
                String finalCommand = command.replace("%player%", player.getGameProfile().getName());
                player.server.getCommands().performPrefixedCommand(
                        player.server.createCommandSourceStack().withPermission(4).withSuppressedOutput(),
                        finalCommand
                );
            }
        }
    }

    private static List<Integer> getProgressForQuest(UUID uuid, String npcId, String questId, QuestConfig.QuestDefinition quest) {
        QuestManager manager = CobblemonEconomy.getQuestManager();
        if (manager == null || quest == null || quest.objectives == null) {
            return List.of();
        }

        Map<Integer, Integer> indexed = new HashMap<>();
        for (QuestManager.ObjectiveProgress row : manager.getObjectiveProgress(uuid, npcId, questId)) {
            indexed.put(row.objectiveIndex, row.progress);
        }

        List<Integer> progress = new ArrayList<>();
        for (int i = 0; i < quest.objectives.size(); i++) {
            progress.add(indexed.getOrDefault(i, 0));
        }
        return progress;
    }

    private static QuestDisplayStatus resolveStatus(QuestConfig.QuestDefinition quest, QuestManager.QuestState state, List<Integer> progress) {
        long now = System.currentTimeMillis();
        if (state == null) {
            return QuestDisplayStatus.AVAILABLE;
        }

        if ("ACTIVE".equalsIgnoreCase(state.status)) {
            if (isQuestComplete(quest, progress)) {
                return QuestDisplayStatus.CLAIMABLE;
            }
            return QuestDisplayStatus.ACTIVE;
        }

        if ("COMPLETED".equalsIgnoreCase(state.status)) {
            return QuestDisplayStatus.CLAIMABLE;
        }

        if ("CLAIMED".equalsIgnoreCase(state.status)) {
            if (quest == null || quest.repeatPolicy == null) {
                return state.availableAt > now ? QuestDisplayStatus.ON_COOLDOWN : QuestDisplayStatus.AVAILABLE;
            }
            String repeat = quest.repeatPolicy.toUpperCase(Locale.ROOT);
            if ("ONCE".equals(repeat)) {
                return QuestDisplayStatus.LOCKED;
            }
            if (state.availableAt > now) {
                return QuestDisplayStatus.ON_COOLDOWN;
            }
            return QuestDisplayStatus.AVAILABLE;
        }

        if ("CANCELLED".equalsIgnoreCase(state.status)) {
            return state.availableAt > now ? QuestDisplayStatus.ON_COOLDOWN : QuestDisplayStatus.AVAILABLE;
        }

        return QuestDisplayStatus.AVAILABLE;
    }

    private static boolean isExpired(QuestConfig.QuestDefinition quest, QuestManager.QuestState state) {
        if (quest == null || state == null || !"ACTIVE".equalsIgnoreCase(state.status)) {
            return false;
        }
        int limitMinutes = quest.timeLimitMinutes == null || quest.timeLimitMinutes <= 0 ? 1440 : quest.timeLimitMinutes;
        long expiresAt = state.acceptedAt + (long) limitMinutes * 60L * 1000L;
        return System.currentTimeMillis() >= expiresAt;
    }

    public static long getRemainingActiveMillis(QuestConfig.QuestDefinition quest, QuestManager.QuestState state) {
        if (quest == null || state == null || !"ACTIVE".equalsIgnoreCase(state.status)) {
            return 0L;
        }
        int limitMinutes = quest.timeLimitMinutes == null || quest.timeLimitMinutes <= 0 ? 1440 : quest.timeLimitMinutes;
        long expiresAt = state.acceptedAt + (long) limitMinutes * 60L * 1000L;
        return Math.max(0L, expiresAt - System.currentTimeMillis());
    }

    public static long getCooldownRemainingMillis(QuestManager.QuestState state) {
        if (state == null || state.availableAt <= 0L) {
            return 0L;
        }
        return Math.max(0L, state.availableAt - System.currentTimeMillis());
    }

    public static long getMillisUntilNextMidnight() {
        return Math.max(0L, nextMidnightMillis() - System.currentTimeMillis());
    }

    public static long getMillisUntilNextRotation(QuestNpcConfig.QuestNpcDefinition npcDefinition) {
        return Math.max(0L, nextRotationBoundaryMillis(npcDefinition) - System.currentTimeMillis());
    }

    private static List<String> getDailyQuestIds(UUID playerUuid, String npcId, QuestNpcConfig.QuestNpcDefinition npcDefinition) {
        if (npcDefinition == null || npcDefinition.questPool == null || npcDefinition.questPool.isEmpty()) {
            return List.of();
        }
        long bucket = rotationBucket(npcDefinition);
        long seed = npcId.hashCode();
        if (!npcDefinition.sharedRotation) {
            seed = 31L * seed + playerUuid.hashCode();
        }
        seed = 31L * seed + bucket;

        List<String> copy = new ArrayList<>(new LinkedHashSet<>(npcDefinition.questPool));
        Collections.shuffle(copy, new Random(seed));

        int max = Math.min(Math.max(1, npcDefinition.visibleQuests), copy.size());
        return new ArrayList<>(copy.subList(0, max));
    }

    private static long rotationBucket(QuestNpcConfig.QuestNpcDefinition npcDefinition) {
        if (npcDefinition == null) {
            return ZonedDateTime.now(ZoneId.systemDefault()).toLocalDate().toEpochDay();
        }
        if ("MIDNIGHT".equalsIgnoreCase(npcDefinition.rotationMode)) {
            return ZonedDateTime.now(ZoneId.systemDefault()).toLocalDate().toEpochDay();
        }
        int hours = Math.max(1, npcDefinition.rotationHours);
        long block = hours * 60L * 60L * 1000L;
        return System.currentTimeMillis() / block;
    }

    private static long nextRotationBoundaryMillis(QuestNpcConfig.QuestNpcDefinition npcDefinition) {
        if (npcDefinition == null || "MIDNIGHT".equalsIgnoreCase(npcDefinition.rotationMode)) {
            return nextMidnightMillis();
        }

        int hours = Math.max(1, npcDefinition.rotationHours);
        long block = hours * 60L * 60L * 1000L;
        long now = System.currentTimeMillis();
        long currentBlock = now / block;
        return (currentBlock + 1L) * block;
    }

    private static boolean hasPrerequisites(UUID playerUuid, String npcId, QuestConfig.QuestDefinition quest) {
        if (quest == null || quest.requiresCompleted == null || quest.requiresCompleted.isEmpty()) {
            return true;
        }
        QuestManager manager = CobblemonEconomy.getQuestManager();
        if (manager == null) {
            return false;
        }
        for (String requiredId : quest.requiresCompleted) {
            if (requiredId == null || requiredId.isBlank()) {
                continue;
            }
            QuestManager.QuestState state = manager.getQuestState(playerUuid, npcId, requiredId);
            if (state == null) {
                return false;
            }
            String status = state.status == null ? "" : state.status.toUpperCase(Locale.ROOT);
            if (!"CLAIMED".equals(status) && !"COMPLETED".equals(status)) {
                return false;
            }
        }
        return true;
    }

    private static QuestNpcConfig.QuestNpcDefinition getNpcDefinition(String npcId) {
        QuestNpcConfig config = CobblemonEconomy.getQuestNpcConfig();
        if (config == null || config.questNpcs == null || npcId == null || npcId.isBlank()) {
            return null;
        }
        return config.questNpcs.get(npcId);
    }

    private static boolean isQuestComplete(QuestConfig.QuestDefinition quest, List<Integer> progress) {
        if (quest == null || quest.objectives == null || quest.objectives.isEmpty()) {
            return false;
        }
        for (int i = 0; i < quest.objectives.size(); i++) {
            QuestConfig.CaptureObjective objective = quest.objectives.get(i);
            int value = i < progress.size() ? progress.get(i) : 0;
            if (objective == null || value < objective.count) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesCapture(QuestConfig.CaptureObjective objective,
                                          String speciesId,
                                          Set<String> pokemonTypes,
                                          Set<String> pokemonLabels,
                                          String pokeballId,
                                          String dimension,
                                          boolean shiny) {
        if (objective.species != null && !objective.species.isEmpty()) {
            boolean speciesMatch = objective.species.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .anyMatch(speciesId::equals);
            if (!speciesMatch) {
                return false;
            }
        }

        if (objective.labels != null && !objective.labels.isEmpty()) {
            boolean labelsMatch = objective.labels.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .anyMatch(pokemonLabels::contains);
            if (!labelsMatch) {
                return false;
            }
        }

        if (objective.types != null && !objective.types.isEmpty()) {
            boolean typeMatch = objective.types.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .anyMatch(pokemonTypes::contains);
            if (!typeMatch) {
                return false;
            }
        }

        if (objective.pokeball != null && !objective.pokeball.isEmpty()) {
            boolean ballMatch = objective.pokeball.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .anyMatch(pokeballId::equals);
            if (!ballMatch) {
                return false;
            }
        }

        if (objective.dimensions != null && !objective.dimensions.isEmpty()) {
            boolean dimensionMatch = objective.dimensions.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(v -> v.toLowerCase(Locale.ROOT))
                    .anyMatch(dimension::equals);
            if (!dimensionMatch) {
                return false;
            }
        } else if (objective.dimension != null && !objective.dimension.isBlank()) {
            if (!dimension.equals(objective.dimension.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }

        if (objective.shiny != null) {
            return objective.shiny == shiny;
        }

        return true;
    }
}
