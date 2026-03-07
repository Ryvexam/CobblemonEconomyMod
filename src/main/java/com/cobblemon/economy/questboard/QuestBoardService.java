package com.cobblemon.economy.questboard;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.networking.OpenQuestBoardPayload;
import com.cobblemon.economy.quest.QuestService;
import com.cobblemon.economy.storage.QuestConfig;
import com.cobblemon.economy.storage.QuestNpcConfig;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QuestBoardService {
    private static final Gson GSON = new Gson();
    private static final Map<String, List<String>> TYPE_PREVIEW_SPECIES = createTypePreviewSpecies();

    private QuestBoardService() {
    }

    public static boolean openBoard(ServerPlayer player, String boardId) {
        QuestNpcConfig config = CobblemonEconomy.getQuestNpcConfig();
        if (config == null || config.questNpcs == null) {
            return false;
        }
        QuestNpcConfig.QuestNpcDefinition board = config.questNpcs.get(boardId);
        if (board == null) {
            return false;
        }

        QuestBoardState state = buildState(player, boardId, board);
        ServerPlayNetworking.send(player, new OpenQuestBoardPayload(boardId, GSON.toJson(state)));
        return true;
    }

    public static QuestBoardState buildState(ServerPlayer player, String boardId, QuestNpcConfig.QuestNpcDefinition board) {
        QuestBoardState state = new QuestBoardState();
        state.boardId = boardId;
        state.boardName = board.displayName;
        state.maxActive = board.maxActive;
        state.visibleQuests = Math.max(QuestService.DEFAULT_VISIBLE_QUESTS, board.visibleQuests);
        state.rotationRemainingMs = QuestService.getMillisUntilNextRotation(board);

        List<QuestService.QuestSnapshot> snapshots = QuestService.getQuestSnapshots(player, boardId, board);
        state.activeCount = (int) snapshots.stream().filter(q -> q.status == QuestService.QuestDisplayStatus.ACTIVE).count();
        for (QuestService.QuestSnapshot snapshot : snapshots) {
            QuestBoardState.QuestCard card = new QuestBoardState.QuestCard();
            card.questId = snapshot.questId;
            card.questName = snapshot.definition.name;
            card.status = snapshot.status.name();
            card.progressSummary = QuestService.renderProgressSummary(snapshot.definition, snapshot.progress);
            card.previewKind = "CAPTURE";
            card.timeLimitMinutes = snapshot.definition.timeLimitMinutes;
            if (snapshot.status == QuestService.QuestDisplayStatus.ACTIVE && snapshot.state != null) {
                card.timeRemainingMs = QuestService.getRemainingActiveMillis(snapshot.definition, snapshot.state);
            }
            if ((snapshot.status == QuestService.QuestDisplayStatus.ON_COOLDOWN || snapshot.status == QuestService.QuestDisplayStatus.LOCKED) && snapshot.state != null) {
                card.cooldownRemainingMs = QuestService.getCooldownRemainingMillis(snapshot.state);
            }

            if (snapshot.definition.rewards != null) {
                if (snapshot.definition.rewards.pokedollars != null) {
                    card.rewardPokedollars = snapshot.definition.rewards.pokedollars;
                }
                if (snapshot.definition.rewards.pco != null) {
                    card.rewardPco = snapshot.definition.rewards.pco;
                }
            }

            if (snapshot.definition.objectives != null) {
                for (int i = 0; i < snapshot.definition.objectives.size(); i++) {
                    QuestConfig.CaptureObjective objective = snapshot.definition.objectives.get(i);
                    if (objective == null) {
                        continue;
                    }
                    applyObjectivePreview(card, objective, snapshot.questId, i);
                    if (card.requiredBall == null && objective.pokeball != null && !objective.pokeball.isEmpty()) {
                        card.requiredBall = canonicalBallId(objective.pokeball.get(0));
                    }
                    int current = i < snapshot.progress.size() ? snapshot.progress.get(i) : 0;
                    card.objectives.add(describeObjective(objective) + " " + current + "/" + objective.count);
                }
            }

            if (card.previewItem == null || card.previewItem.isBlank()) {
                card.previewItem = switch (card.previewKind) {
                    case "RAID" -> selectPreviewItem("minecraft:totem_of_undying", "minecraft:end_crystal", "minecraft:nether_star");
                    case "BATTLE" -> "minecraft:player_head";
                    case "TOWER" -> "minecraft:iron_sword";
                    case "FOSSIL" -> selectPreviewItem("cobblemon:fossil", "cobblemon:dome_fossil", "minecraft:nautilus_shell");
                    default -> selectPreviewItem("cobblemon:poke_ball", "minecraft:ender_pearl");
                };
            }

            state.quests.add(card);
        }

        return state;
    }

    private static String describeObjective(QuestConfig.CaptureObjective objective) {
        String type = objective.type == null ? "capture" : objective.type.toLowerCase(Locale.ROOT);
        if ("raid_win".equals(type)) {
            return "Win raid battles";
        }
        if ("battle_win".equals(type)) {
            return "Win battles";
        }
        if ("tower_win".equals(type)) {
            return "Win battle tower runs";
        }

        boolean hasSpecies = objective.species != null && !objective.species.isEmpty();
        boolean hasTypes = objective.types != null && !objective.types.isEmpty();
        boolean hasLabels = objective.labels != null && !objective.labels.isEmpty();
        boolean shiny = Boolean.TRUE.equals(objective.shiny);

        String target;
        if (hasSpecies) {
            target = joinPretty(objective.species);
        } else {
            StringBuilder base = new StringBuilder();
            if (shiny) {
                base.append("shiny ");
            }
            if (hasLabels) {
                base.append(joinPretty(objective.labels)).append(' ');
            }
            if (hasTypes) {
                base.append(joinPretty(objective.types)).append(' ');
            }
            base.append("Pokemon");
            target = base.toString().trim();
        }

        String verb = "fossil_revive".equals(type) ? "Revive" : "Capture";
        List<String> constraints = new ArrayList<>();
        if (hasSpecies && shiny) {
            constraints.add("shiny");
        }
        if (hasSpecies && hasTypes) {
            constraints.add("type " + joinPretty(objective.types));
        }
        if (hasSpecies && hasLabels) {
            constraints.add("trait " + joinPretty(objective.labels));
        }
        if (objective.pokeball != null && !objective.pokeball.isEmpty()) {
            constraints.add("with " + joinPretty(objective.pokeball));
        }
        if (objective.dimensions != null && !objective.dimensions.isEmpty()) {
            constraints.add("in " + joinPretty(objective.dimensions));
        } else if (objective.dimension != null && !objective.dimension.isBlank()) {
            constraints.add("in " + shortId(objective.dimension));
        }

        if (constraints.isEmpty()) {
            return verb + " " + target;
        }
        return verb + " " + target + " (" + String.join(", ", constraints) + ")";
    }

    private static void applyObjectivePreview(QuestBoardState.QuestCard card, QuestConfig.CaptureObjective objective, String questId, int objectiveIndex) {
        String type = objective.type == null ? "capture" : objective.type.toLowerCase(Locale.ROOT);

        if (("capture".equals(type) || "fossil_revive".equals(type)) && objective.species != null && !objective.species.isEmpty()) {
            String resolvedSpecies = canonicalSpeciesId(objective.species.get(0));
            if (resolvedSpecies != null && !resolvedSpecies.isBlank()) {
                card.previewKind = "POKEMON";
                card.previewSpecies = resolvedSpecies;
                if (Boolean.TRUE.equals(objective.shiny)) {
                    card.previewShiny = true;
                }
                return;
            }
        }

        if (("capture".equals(type) || "fossil_revive".equals(type)) && objective.types != null && !objective.types.isEmpty()) {
            String previewFromType = pickSpeciesForTypeObjective(objective.types, questId, objectiveIndex);
            if (previewFromType != null && !previewFromType.isBlank()) {
                card.previewKind = "POKEMON";
                card.previewSpecies = previewFromType;
                if (Boolean.TRUE.equals(objective.shiny)) {
                    card.previewShiny = true;
                }
                return;
            }
        }

        if (Boolean.TRUE.equals(objective.shiny) && card.previewSpecies != null && !card.previewSpecies.isBlank()) {
            card.previewShiny = true;
        }

        if (card.previewSpecies != null && !card.previewSpecies.isBlank()) {
            return;
        }

        switch (type) {
            case "raid_win" -> {
                card.previewKind = "RAID";
                card.previewItem = selectPreviewItem("minecraft:totem_of_undying", "minecraft:end_crystal", "minecraft:nether_star");
            }
            case "battle_win" -> {
                card.previewKind = "BATTLE";
                card.previewItem = "minecraft:player_head";
            }
            case "tower_win" -> {
                card.previewKind = "TOWER";
                card.previewItem = "minecraft:iron_sword";
            }
            case "fossil_revive" -> {
                card.previewKind = "FOSSIL";
                card.previewItem = selectPreviewItem("cobblemon:fossil", "cobblemon:dome_fossil", "minecraft:nautilus_shell");
            }
            default -> {
                card.previewKind = "CAPTURE";
                if (card.previewItem == null || card.previewItem.isBlank()) {
                    card.previewItem = selectPreviewItem("cobblemon:poke_ball", "minecraft:ender_pearl");
                }
            }
        }
    }

    private static String joinPretty(List<String> values) {
        return values.stream().map(QuestBoardService::shortId).reduce((a, b) -> a + " or " + b).orElse("");
    }

    private static String shortId(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int idx = value.indexOf(':');
        if (idx >= 0 && idx + 1 < value.length()) {
            return value.substring(idx + 1).replace('_', ' ');
        }
        return value.replace('_', ' ');
    }

    private static String canonicalSpeciesId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String normalized = normalizeToken(raw);
        ResourceLocation parsed = ResourceLocation.tryParse(normalized);
        if (parsed != null) {
            Species byId = PokemonSpecies.getByIdentifier(parsed);
            if (byId != null) {
                return byId.getResourceIdentifier().toString();
            }
        }

        String shortName = normalized;
        int idx = normalized.indexOf(':');
        if (idx >= 0 && idx + 1 < normalized.length()) {
            shortName = normalized.substring(idx + 1);
        }

        Species byName = PokemonSpecies.getByName(shortName);
        if (byName != null) {
            return byName.getResourceIdentifier().toString();
        }

        return null;
    }

    private static String selectPreviewItem(String... candidates) {
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            ResourceLocation parsed = ResourceLocation.tryParse(candidate);
            if (parsed != null && existsItem(parsed)) {
                return parsed.toString();
            }
        }
        return "minecraft:barrier";
    }

    private static String canonicalBallId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "cobblemon:poke_ball";
        }

        String normalized = normalizeToken(raw);
        ResourceLocation parsed = ResourceLocation.tryParse(normalized);
        if (parsed != null && existsItem(parsed)) {
            return parsed.toString();
        }

        String shortName = normalized;
        int idx = normalized.indexOf(':');
        if (idx >= 0 && idx + 1 < normalized.length()) {
            shortName = normalized.substring(idx + 1);
        }

        ResourceLocation cobblemon = ResourceLocation.fromNamespaceAndPath("cobblemon", shortName);
        if (existsItem(cobblemon)) {
            return cobblemon.toString();
        }

        ResourceLocation minecraft = ResourceLocation.fromNamespaceAndPath("minecraft", shortName);
        if (existsItem(minecraft)) {
            return minecraft.toString();
        }

        return "cobblemon:poke_ball";
    }

    private static boolean existsItem(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return item != null && item != Items.AIR;
    }

    private static String normalizeToken(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private static String pickSpeciesForTypeObjective(List<String> types, String questId, int objectiveIndex) {
        if (types == null || types.isEmpty()) {
            return null;
        }

        String selectedType = null;
        for (String rawType : types) {
            if (rawType == null || rawType.isBlank()) {
                continue;
            }
            String normalized = normalizeToken(rawType);
            if (TYPE_PREVIEW_SPECIES.containsKey(normalized)) {
                selectedType = normalized;
                break;
            }
        }

        if (selectedType == null) {
            return null;
        }

        List<String> speciesPool = TYPE_PREVIEW_SPECIES.get(selectedType);
        if (speciesPool == null || speciesPool.isEmpty()) {
            return null;
        }

        String salt = (questId == null ? "quest" : questId) + "#" + objectiveIndex + "#" + selectedType;
        int idx = Math.floorMod(salt.hashCode(), speciesPool.size());
        return canonicalSpeciesId(speciesPool.get(idx));
    }

    private static Map<String, List<String>> createTypePreviewSpecies() {
        Map<String, List<String>> map = new HashMap<>();
        map.put("bug", Arrays.asList("cobblemon:spinarak", "cobblemon:joltik", "cobblemon:snom"));
        map.put("dark", Arrays.asList("cobblemon:poochyena", "cobblemon:deino", "cobblemon:zorua"));
        map.put("dragon", Arrays.asList("cobblemon:dratini", "cobblemon:gible", "cobblemon:axew"));
        map.put("electric", Arrays.asList("cobblemon:shinx", "cobblemon:mareep", "cobblemon:joltik"));
        map.put("fairy", Arrays.asList("cobblemon:ralts", "cobblemon:cleffa", "cobblemon:snubbull"));
        map.put("fighting", Arrays.asList("cobblemon:machop", "cobblemon:mankey", "cobblemon:riolu"));
        map.put("fire", Arrays.asList("cobblemon:growlithe", "cobblemon:vulpix", "cobblemon:slugma"));
        map.put("flying", Arrays.asList("cobblemon:starly", "cobblemon:pidgey", "cobblemon:taillow"));
        map.put("ghost", Arrays.asList("cobblemon:gastly", "cobblemon:shuppet", "cobblemon:misdreavus"));
        map.put("grass", Arrays.asList("cobblemon:oddish", "cobblemon:bellsprout", "cobblemon:budew"));
        map.put("ground", Arrays.asList("cobblemon:sandshrew", "cobblemon:trapinch", "cobblemon:wooper"));
        map.put("ice", Arrays.asList("cobblemon:sneasel", "cobblemon:snorunt", "cobblemon:bergmite"));
        map.put("normal", Arrays.asList("cobblemon:bidoof", "cobblemon:zigzagoon", "cobblemon:sentret"));
        map.put("poison", Arrays.asList("cobblemon:ekans", "cobblemon:grimer", "cobblemon:croagunk"));
        map.put("psychic", Arrays.asList("cobblemon:abra", "cobblemon:munna", "cobblemon:ralts"));
        map.put("rock", Arrays.asList("cobblemon:geodude", "cobblemon:roggenrola", "cobblemon:aron"));
        map.put("steel", Arrays.asList("cobblemon:aron", "cobblemon:bronzor", "cobblemon:magnemite"));
        map.put("water", Arrays.asList("cobblemon:magikarp", "cobblemon:tentacool", "cobblemon:wooper"));
        return Collections.unmodifiableMap(map);
    }
}
