package com.cobblemon.economy.storage;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuestConfig {
    public Map<String, QuestDefinition> quests = new HashMap<>();

    public static class QuestDefinition {
        public String name = "Quest";
        public String repeatPolicy = "DAILY";
        public Integer cooldownMinutes = null;
        public List<CaptureObjective> objectives = new ArrayList<>();
        public RewardDefinition rewards = new RewardDefinition();
    }

    public static class CaptureObjective {
        public String type = "capture";
        public int count = 1;
        public List<String> species;
        public List<String> types;
        public List<String> labels;
        public List<String> pokeball;
        public String dimension;
        public Boolean shiny;
    }

    public static class RewardDefinition {
        public BigDecimal pokedollars = BigDecimal.ZERO;
        public BigDecimal pco = BigDecimal.ZERO;
        public List<String> commands = new ArrayList<>();
    }

    public static QuestConfig load(File file) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        QuestConfig config = null;
        boolean shouldSave = !file.exists();

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                config = gson.fromJson(reader, QuestConfig.class);
            } catch (IOException e) {
                CobblemonEconomy.LOGGER.error("Failed to load quests config", e);
            }
        }

        if (config == null) {
            config = new QuestConfig();
            shouldSave = true;
        }

        if (config.quests == null) {
            config.quests = new HashMap<>();
            shouldSave = true;
        }

        for (Map.Entry<String, QuestDefinition> entry : config.quests.entrySet()) {
            QuestDefinition def = entry.getValue();
            if (def == null) {
                continue;
            }

            if (def.name == null || def.name.isBlank()) {
                def.name = entry.getKey();
                shouldSave = true;
            }
            if (def.repeatPolicy == null || def.repeatPolicy.isBlank()) {
                def.repeatPolicy = "DAILY";
                shouldSave = true;
            } else {
                String normalized = def.repeatPolicy.trim().toUpperCase(Locale.ROOT);
                if (!normalized.equals("ONCE") && !normalized.equals("DAILY") && !normalized.equals("ALWAYS")) {
                    def.repeatPolicy = "DAILY";
                    shouldSave = true;
                } else if (!normalized.equals(def.repeatPolicy)) {
                    def.repeatPolicy = normalized;
                    shouldSave = true;
                }
            }
            if (def.objectives == null) {
                def.objectives = new ArrayList<>();
                shouldSave = true;
            }
            if (def.rewards == null) {
                def.rewards = new RewardDefinition();
                shouldSave = true;
            }
            if (def.rewards.pokedollars == null) {
                def.rewards.pokedollars = BigDecimal.ZERO;
                shouldSave = true;
            }
            if (def.rewards.pco == null) {
                def.rewards.pco = BigDecimal.ZERO;
                shouldSave = true;
            }
            if (def.rewards.commands == null) {
                def.rewards.commands = new ArrayList<>();
                shouldSave = true;
            }

            for (CaptureObjective objective : def.objectives) {
                if (objective == null) {
                    continue;
                }
                if (objective.type == null || objective.type.isBlank()) {
                    objective.type = "capture";
                    shouldSave = true;
                }
                if (objective.count <= 0) {
                    objective.count = 1;
                    shouldSave = true;
                }
            }
        }

        if (config.quests.isEmpty()) {
            config.quests.putAll(defaultQuests());
            shouldSave = true;
        }

        if (shouldSave) {
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(config, writer);
            } catch (IOException e) {
                CobblemonEconomy.LOGGER.error("Failed to save quests config", e);
            }
        }

        return config;
    }

    private static Map<String, QuestDefinition> defaultQuests() {
        Map<String, QuestDefinition> defaults = new HashMap<>();

        defaults.put("safari_water_10", quest("Safari Water Hunter", "DAILY", reward("2000", "20"),
                captureObjective(10, null, List.of("water"), null, null, "minecraft:overworld", null)));

        defaults.put("safari_bug_12", quest("Bug Net Patrol", "DAILY", reward("1800", "15"),
                captureObjective(12, null, List.of("bug"), null, List.of("cobblemon:net_ball"), "minecraft:overworld", null)));

        defaults.put("safari_forest_species", quest("Forest Trio", "DAILY", reward("3500", "30"),
                captureObjective(1, List.of("cobblemon:oddish"), null, null, null, "minecraft:overworld", null),
                captureObjective(1, List.of("cobblemon:paras"), null, null, null, "minecraft:overworld", null),
                captureObjective(1, List.of("cobblemon:scyther"), null, null, null, "minecraft:overworld", null)));

        defaults.put("safari_lapras_net", quest("Technical Capture", "DAILY", reward("4000", "40"),
                captureObjective(1, List.of("cobblemon:lapras"), null, null, List.of("cobblemon:net_ball"), "minecraft:overworld", null)));

        defaults.put("safari_gible_ultra", quest("Dragon Precision", "DAILY", reward("4200", "35"),
                captureObjective(1, List.of("cobblemon:gible"), null, null, List.of("cobblemon:ultra_ball"), "minecraft:overworld", null)));

        defaults.put("safari_shiny_1", quest("A Shiny in Safari", "DAILY", reward("2500", "120"),
                captureObjective(1, null, null, null, null, "minecraft:overworld", true)));

        defaults.put("safari_radiant_1", quest("Radiant Miracle", "DAILY", reward("6000", "180"),
                captureObjective(1, null, null, List.of("radiant"), null, "minecraft:overworld", null)));

        defaults.put("safari_paradox_1", quest("Temporal Hunter", "DAILY", reward("5500", "90"),
                captureObjective(1, null, null, List.of("paradox"), null, "minecraft:overworld", null)));

        defaults.put("ball_specialist_quick", quest("Quick Draw", "DAILY", reward("2200", "20"),
                captureObjective(8, null, null, null, List.of("cobblemon:quick_ball"), null, null)));

        defaults.put("ball_specialist_dusk", quest("Night Collector", "DAILY", reward("2400", "25"),
                captureObjective(8, null, null, null, List.of("cobblemon:dusk_ball"), null, null)));

        defaults.put("ball_combo_water_dive", quest("Deep Sea Method", "DAILY", reward("3200", "32"),
                captureObjective(5, null, List.of("water"), null, List.of("cobblemon:dive_ball"), null, null)));

        defaults.put("starter_hunter", quest("Professor Checklist", "DAILY", reward("3000", "20"),
                captureObjective(1, List.of("cobblemon:bulbasaur"), null, null, null, null, null),
                captureObjective(1, List.of("cobblemon:charmander"), null, null, null, null, null),
                captureObjective(1, List.of("cobblemon:squirtle"), null, null, null, null, null)));

        defaults.put("legendary_echo", quest("Echo of Legends", "ONCE", reward("20000", "600"),
                captureObjective(1, null, null, List.of("legendary"), null, null, null)));

        defaults.put("mythical_echo", quest("Mythical Witness", "ONCE", reward("25000", "800"),
                captureObjective(1, null, null, List.of("mythical"), null, null, null)));

        defaults.put("raid_winner_3", quest("Raid Vanguard", "DAILY", reward("3500", "45"),
                eventObjective("raid_win", 3)));

        defaults.put("raid_winner_7", quest("Den Destroyer", "DAILY", reward("7000", "90"),
                eventObjective("raid_win", 7)));

        defaults.put("battle_streak_5", quest("Arena Warmup", "DAILY", reward("1800", "12"),
                eventObjective("battle_win", 5)));

        defaults.put("battle_streak_15", quest("Arena Marathon", "DAILY", reward("4200", "35"),
                eventObjective("battle_win", 15)));

        defaults.put("tower_champion", quest("Tower Challenger", "DAILY", reward("3800", "55"),
                eventObjective("tower_win", 2)));

        defaults.put("mixed_hunter", quest("Elite Field Trial", "DAILY", reward("8500", "120"),
                captureObjective(3, null, List.of("water"), null, null, null, null),
                captureObjective(2, null, null, List.of("radiant"), null, null, null),
                eventObjective("raid_win", 2),
                captureObjective(1, List.of("cobblemon:dragonite"), null, null, List.of("cobblemon:ultra_ball"), null, null)));

        return defaults;
    }

    private static QuestDefinition quest(String name, String repeatPolicy, RewardDefinition rewards, CaptureObjective... objectives) {
        QuestDefinition definition = new QuestDefinition();
        definition.name = name;
        definition.repeatPolicy = repeatPolicy;
        definition.rewards = rewards;
        definition.objectives = new ArrayList<>(List.of(objectives));
        return definition;
    }

    private static RewardDefinition reward(String pokedollars, String pco) {
        RewardDefinition reward = new RewardDefinition();
        reward.pokedollars = new BigDecimal(pokedollars);
        reward.pco = new BigDecimal(pco);
        return reward;
    }

    private static CaptureObjective captureObjective(int count,
                                                     List<String> species,
                                                     List<String> types,
                                                     List<String> labels,
                                                     List<String> pokeball,
                                                     String dimension,
                                                     Boolean shiny) {
        CaptureObjective objective = new CaptureObjective();
        objective.type = "capture";
        objective.count = count;
        objective.species = species;
        objective.types = types;
        objective.labels = labels;
        objective.pokeball = pokeball;
        objective.dimension = dimension;
        objective.shiny = shiny;
        return objective;
    }

    private static CaptureObjective eventObjective(String type, int count) {
        CaptureObjective objective = new CaptureObjective();
        objective.type = type;
        objective.count = count;
        return objective;
    }
}
