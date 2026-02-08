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
        public Boolean repeatable = true;
        public Integer timeLimitMinutes = 1440;
        public Integer cooldownMinutes = null;
        public List<String> requiresCompleted = new ArrayList<>();
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
        public List<String> dimensions;
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
            if (def.repeatable == null) {
                def.repeatable = !"ONCE".equals(def.repeatPolicy);
                shouldSave = true;
            }
            if (def.objectives == null) {
                def.objectives = new ArrayList<>();
                shouldSave = true;
            }
            if (def.requiresCompleted == null) {
                def.requiresCompleted = new ArrayList<>();
                shouldSave = true;
            }
            if (def.timeLimitMinutes == null || def.timeLimitMinutes <= 0) {
                def.timeLimitMinutes = 1440;
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

        defaults.put("safari_fly_8", quest("Wing Survey", "DAILY", reward("2200", "20"),
                captureObjective(8, null, List.of("flying"), null, null, "minecraft:overworld", null)));

        defaults.put("safari_poison_10", quest("Swamp Venom", "DAILY", reward("2600", "24"),
                captureObjective(10, null, List.of("poison"), null, null, "minecraft:overworld", null)));

        defaults.put("safari_bug_shiny", quest("Shining Carapace", "DAILY", reward("5000", "140"),
                captureObjective(1, null, List.of("bug"), null, null, "minecraft:overworld", true)));

        defaults.put("safari_bird_trio", quest("Feather Census", "DAILY", reward("3300", "30"),
                captureObjective(1, List.of("cobblemon:pidgeotto"), null, null, null, "minecraft:overworld", null),
                captureObjective(1, List.of("cobblemon:fearow"), null, null, null, "minecraft:overworld", null),
                captureObjective(1, List.of("cobblemon:staravia"), null, null, null, "minecraft:overworld", null)));

        defaults.put("safari_apex_trial", quest("Safari Apex Trial", "DAILY", reward("9200", "160"),
                captureObjective(6, null, List.of("grass"), null, null, "minecraft:overworld", null),
                captureObjective(3, null, List.of("water"), null, List.of("cobblemon:net_ball"), "minecraft:overworld", null),
                captureObjective(1, null, null, List.of("radiant"), null, "minecraft:overworld", null)));

        defaults.put("safari_night_stalkers", quest("Night Stalkers", "DAILY", reward("3400", "36"),
                captureObjective(6, null, List.of("dark", "ghost"), null, List.of("cobblemon:dusk_ball"), "minecraft:overworld", null)));

        defaults.put("safari_steel_test", quest("Metal Frame", "DAILY", reward("3900", "42"),
                captureObjective(7, null, List.of("steel"), null, null, "minecraft:overworld", null)));

        defaults.put("fisher_common_12", quest("Morning Haul", "DAILY", reward("1800", "18"),
                captureObjective(12, null, List.of("water"), null, null, "minecraft:overworld", null)));

        defaults.put("fisher_magikarp_6", quest("Carp Festival", "DAILY", reward("2000", "20"),
                captureObjective(6, List.of("cobblemon:magikarp"), null, null, null, "minecraft:overworld", null)));

        defaults.put("fisher_lure_8", quest("Lure Specialist", "DAILY", reward("2600", "25"),
                captureObjective(8, null, List.of("water"), null, List.of("cobblemon:lure_ball"), "minecraft:overworld", null)));

        defaults.put("fisher_net_8", quest("Net Sweep", "DAILY", reward("2400", "25"),
                captureObjective(8, null, List.of("water"), null, List.of("cobblemon:net_ball"), "minecraft:overworld", null)));

        defaults.put("fisher_dive_8", quest("Depth Charge", "DAILY", reward("3000", "30"),
                captureObjective(8, null, List.of("water"), null, List.of("cobblemon:dive_ball"), "minecraft:overworld", null)));

        defaults.put("fisher_lapras_lure", quest("Captain's Prize", "DAILY", reward("4800", "60"),
                captureObjective(1, List.of("cobblemon:lapras"), null, null, List.of("cobblemon:lure_ball"), "minecraft:overworld", null)));

        defaults.put("fisher_feebas_dive", quest("River Secret", "DAILY", reward("5200", "70"),
                captureObjective(1, List.of("cobblemon:feebas"), null, null, List.of("cobblemon:dive_ball"), "minecraft:overworld", null)));

        defaults.put("fisher_shiny_water", quest("Glittering Tide", "DAILY", reward("4500", "130"),
                captureObjective(1, null, List.of("water"), null, null, "minecraft:overworld", true)));

        defaults.put("fisher_radiant_tide", quest("Radiant Wave", "DAILY", reward("6500", "180"),
                captureObjective(1, null, List.of("water"), List.of("radiant"), null, "minecraft:overworld", null)));

        defaults.put("fisher_tournament", quest("Harbor Tournament", "DAILY", reward("7000", "90"),
                captureObjective(6, null, List.of("water"), null, null, "minecraft:overworld", null),
                eventObjective("battle_win", 3)));

        defaults.put("fisher_storm_run", quest("Storm Run", "DAILY", reward("6200", "75"),
                captureObjective(10, null, List.of("water"), null, List.of("cobblemon:quick_ball"), "minecraft:overworld", null),
                eventObjective("raid_win", 1)));

        defaults.put("fossil_research_6", quest("Ancient Research", "DAILY", reward("6500", "80"),
                eventObjective("fossil_revive", 6)));

        defaults.put("fossil_shiny_1", quest("Spark in Amber", "DAILY", reward("15000", "220"),
                fossilObjective(1, null, null, null, true)));

        defaults.put("fossil_radiant_1", quest("Prismatic Relic", "DAILY", reward("17500", "260"),
                fossilObjective(1, null, null, List.of("radiant"), null)));

        defaults.put("fossil_legend_1", quest("Relic of Kings", "ONCE", reward("28000", "900"),
                fossilObjective(1, null, null, List.of("legendary"), null)));

        defaults.put("fossil_species_pair", quest("Museum Reconstruction", "DAILY", reward("8500", "110"),
                fossilObjective(1, List.of("cobblemon:omanyte"), null, null, null),
                fossilObjective(1, List.of("cobblemon:kabuto"), null, null, null)));

        defaults.put("explore_nether_6", quest("Blazing Frontier", "DAILY", reward("3600", "40"),
                captureObjective(6, null, null, null, null, "minecraft:the_nether", null)));

        defaults.put("explore_end_6", quest("Void Frontier", "DAILY", reward("4200", "48"),
                captureObjective(6, null, null, null, null, "minecraft:the_end", null)));

        defaults.put("explore_three_realms", quest("Three Realms Survey", "DAILY", reward("8000", "110"),
                captureObjective(2, null, null, null, null, "minecraft:overworld", null),
                captureObjective(2, null, null, null, null, "minecraft:the_nether", null),
                captureObjective(2, null, null, null, null, "minecraft:the_end", null)));

        defaults.put("explore_nether_fire", quest("Magma Ecology", "DAILY", reward("4500", "52"),
                captureObjective(6, null, List.of("fire"), null, null, "minecraft:the_nether", null)));

        defaults.put("explore_end_dragon", quest("Abyss Drakes", "DAILY", reward("5200", "65"),
                captureObjective(4, null, List.of("dragon"), null, null, "minecraft:the_end", null)));

        defaults.put("explore_cave_guard", quest("Stone Watch", "DAILY", reward("2800", "22"),
                captureObjective(8, null, List.of("rock", "ground"), null, null, "minecraft:overworld", null)));

        defaults.put("explore_shiny_trail", quest("Starlight Trail", "DAILY", reward("6200", "170"),
                captureObjective(1, null, null, null, null, "minecraft:the_end", true)));

        defaults.put("explore_paradox_void", quest("Temporal Rupture", "DAILY", reward("7000", "120"),
                captureObjective(1, null, null, List.of("paradox"), null, "minecraft:the_end", null)));

        defaults.put("explore_legend_path", quest("Legend's Footsteps", "ONCE", reward("24000", "700"),
                captureObjective(1, null, null, List.of("legendary"), null, "minecraft:the_end", null)));

        defaults.put("explore_mythic_path", quest("Myth Underworld", "ONCE", reward("26000", "820"),
                captureObjective(1, null, null, List.of("mythical"), null, "minecraft:the_nether", null)));

        defaults.put("explore_skies_route", quest("Sky Route Mapping", "DAILY", reward("5200", "60"),
                captureObjective(8, null, List.of("flying"), null, null, null, null),
                captureObjective(2, null, List.of("flying"), null, null, "minecraft:the_end", null)));

        defaults.put("explore_nether_duel", quest("Lavafront Exercises", "DAILY", reward("7600", "95"),
                captureObjective(5, null, null, null, null, "minecraft:the_nether", null),
                eventObjective("battle_win", 4)));

        defaults.put("explore_overworld_30", quest("Overworld Census", "DAILY", reward("7800", "90"),
                captureObjective(30, null, null, null, null, "minecraft:overworld", null)));

        defaults.put("explore_nether_30", quest("Nether Census", "DAILY", reward("9800", "120"),
                captureObjective(30, null, null, null, null, "minecraft:the_nether", null)));

        defaults.put("explore_safari_30", quest("Safari Census", "DAILY", reward("10500", "140"),
                captureObjective(30, null, null, null, null, "safari:safari", null)));

        defaults.put("explore_frontier_30_any", quest("Frontier Mega Census", "DAILY", reward("11800", "165"),
                captureObjectiveAnyDimensions(30, List.of("minecraft:overworld", "minecraft:the_nether", "safari:safari"))));

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

        defaults.put("duel_battle_30", quest("War Room", "DAILY", reward("7800", "85"),
                eventObjective("battle_win", 30)));

        defaults.put("duel_raid_5", quest("Raid Frontline", "DAILY", reward("5200", "60"),
                eventObjective("raid_win", 5)));

        defaults.put("duel_raid_10", quest("Den Suppressor", "DAILY", reward("9000", "120"),
                eventObjective("raid_win", 10)));

        defaults.put("tower_champion", quest("Tower Challenger", "DAILY", reward("3800", "55"),
                eventObjective("tower_win", 2)));

        defaults.put("duel_tower_4", quest("Tower Linebreaker", "DAILY", reward("7600", "100"),
                eventObjective("tower_win", 4)));

        defaults.put("duel_elite_combo", quest("Elite Rotation", "DAILY", reward("11000", "170"),
                eventObjective("battle_win", 10),
                eventObjective("raid_win", 3),
                eventObjective("tower_win", 2)));

        defaults.put("duel_capture_fighter", quest("Victory and Valor", "DAILY", reward("6000", "70"),
                eventObjective("battle_win", 8),
                captureObjective(4, null, List.of("fighting"), null, null, null, null)));

        defaults.put("duel_dragon_precision", quest("Final Bout", "DAILY", reward("9500", "135"),
                eventObjective("battle_win", 12),
                captureObjective(1, List.of("cobblemon:garchomp"), null, null, List.of("cobblemon:ultra_ball"), null, null)));

        defaults.put("duel_grand_champion", quest("Grand Champion Circuit", "ONCE", reward("40000", "1200"),
                eventObjective("battle_win", 40),
                eventObjective("raid_win", 12),
                eventObjective("tower_win", 8)));

        defaults.put("duel_precision_ball", quest("Precision League", "DAILY", reward("8200", "105"),
                eventObjective("battle_win", 8),
                captureObjective(3, null, null, null, List.of("cobblemon:great_ball"), null, null)));

        defaults.put("duel_paradox_hunt", quest("Rift Duel Protocol", "DAILY", reward("13000", "190"),
                eventObjective("raid_win", 4),
                captureObjective(1, null, null, List.of("paradox"), null, null, null)));

        defaults.put("collector_eevee_line", quest("Evolution Enthusiast", "DAILY", reward("5200", "55"),
                captureObjective(1, List.of("cobblemon:eevee"), null, null, null, null, null),
                captureObjective(1, List.of("cobblemon:vaporeon"), null, null, null, null, null),
                captureObjective(1, List.of("cobblemon:jolteon"), null, null, null, null, null),
                captureObjective(1, List.of("cobblemon:flareon"), null, null, null, null, null)));

        defaults.put("collector_starters_johto", quest("Johto Internship", "DAILY", reward("4800", "50"),
                captureObjective(1, List.of("cobblemon:chikorita"), null, null, null, null, null),
                captureObjective(1, List.of("cobblemon:cyndaquil"), null, null, null, null, null),
                captureObjective(1, List.of("cobblemon:totodile"), null, null, null, null, null)));

        defaults.put("collector_psychic_set", quest("Mind Atlas", "DAILY", reward("4300", "44"),
                captureObjective(10, null, List.of("psychic"), null, null, null, null)));

        defaults.put("collector_ghost_set", quest("Night Ledger", "DAILY", reward("4300", "44"),
                captureObjective(10, null, List.of("ghost"), null, null, null, null)));

        defaults.put("collector_ice_set", quest("Cold Archive", "DAILY", reward("4300", "44"),
                captureObjective(10, null, List.of("ice"), null, null, null, null)));

        defaults.put("collector_shiny_pair", quest("Double Spark", "DAILY", reward("9000", "260"),
                captureObjective(2, null, null, null, null, null, true)));

        defaults.put("collector_dragon_set", quest("Drake Archive", "DAILY", reward("6800", "90"),
                captureObjective(10, null, List.of("dragon"), null, null, null, null)));

        defaults.put("collector_ground_set", quest("Tectonic Index", "DAILY", reward("5200", "58"),
                captureObjective(10, null, List.of("ground"), null, null, null, null)));

        defaults.put("mixed_hunter", quest("Elite Field Trial", "DAILY", reward("8500", "120"),
                captureObjective(3, null, List.of("water"), null, null, null, null),
                captureObjective(2, null, null, List.of("radiant"), null, null, null),
                eventObjective("raid_win", 2),
                captureObjective(1, List.of("cobblemon:dragonite"), null, null, List.of("cobblemon:ultra_ball"), null, null)));

        defaults.get("safari_apex_trial").requiresCompleted = List.of("safari_water_10", "safari_bug_12");
        defaults.get("duel_grand_champion").requiresCompleted = List.of("duel_elite_combo", "duel_tower_4");
        defaults.get("fossil_legend_1").requiresCompleted = List.of("fossil_research_6");

        return defaults;
    }

    private static QuestDefinition quest(String name, String repeatPolicy, RewardDefinition rewards, CaptureObjective... objectives) {
        QuestDefinition definition = new QuestDefinition();
        definition.name = name;
        definition.repeatPolicy = repeatPolicy;
        definition.repeatable = !"ONCE".equalsIgnoreCase(repeatPolicy);
        definition.rewards = rewards;
        definition.objectives = new ArrayList<>(List.of(objectives));
        return definition;
    }

    private static RewardDefinition reward(String pokedollars, String pco) {
        RewardDefinition reward = new RewardDefinition();
        reward.pokedollars = new BigDecimal(pokedollars);
        reward.pco = BigDecimal.ZERO;
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

    private static CaptureObjective captureObjectiveAnyDimensions(int count, List<String> dimensions) {
        CaptureObjective objective = new CaptureObjective();
        objective.type = "capture";
        objective.count = count;
        objective.dimensions = dimensions;
        return objective;
    }

    private static CaptureObjective fossilObjective(int count,
                                                    List<String> species,
                                                    List<String> types,
                                                    List<String> labels,
                                                    Boolean shiny) {
        CaptureObjective objective = new CaptureObjective();
        objective.type = "fossil_revive";
        objective.count = count;
        objective.species = species;
        objective.types = types;
        objective.labels = labels;
        objective.shiny = shiny;
        return objective;
    }
}
