package com.cobblemon.economy.storage;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestNpcConfig {
    @SerializedName(value = "quest_npcs", alternate = {"questNpcs"})
    public Map<String, QuestNpcDefinition> questNpcs = new HashMap<>();

    public static class DialogueDefinition {
        public List<String> greeting = new ArrayList<>();
        public List<String> inProgress = new ArrayList<>();
        public List<String> readyToClaim = new ArrayList<>();
        public List<String> completed = new ArrayList<>();
        public List<String> cooldown = new ArrayList<>();
    }

    public static class QuestNpcDefinition {
        public String displayName = "Quest NPC";
        public String skin = "shopkeeper";
        public int maxActive = 1;
        public List<String> questPool = new ArrayList<>();
        public DialogueDefinition dialogues = new DialogueDefinition();
    }

    public static QuestNpcConfig load(File file) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        QuestNpcConfig config = null;
        boolean shouldSave = !file.exists();

        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                config = gson.fromJson(reader, QuestNpcConfig.class);
            } catch (IOException e) {
                CobblemonEconomy.LOGGER.error("Failed to load quest NPC config", e);
            }
        }

        if (config == null) {
            config = new QuestNpcConfig();
            shouldSave = true;
        }

        if (config.questNpcs == null) {
            config.questNpcs = new HashMap<>();
            shouldSave = true;
        }

        for (Map.Entry<String, QuestNpcDefinition> entry : config.questNpcs.entrySet()) {
            QuestNpcDefinition def = entry.getValue();
            if (def == null) {
                continue;
            }
            if (def.displayName == null || def.displayName.isBlank()) {
                def.displayName = entry.getKey();
                shouldSave = true;
            }
            if (def.skin == null || def.skin.isBlank()) {
                def.skin = "shopkeeper";
                shouldSave = true;
            }
            if (def.maxActive <= 0) {
                def.maxActive = 1;
                shouldSave = true;
            }
            if (def.questPool == null) {
                def.questPool = new ArrayList<>();
                shouldSave = true;
            }
            if (def.dialogues == null) {
                def.dialogues = new DialogueDefinition();
                shouldSave = true;
            }
            if (def.dialogues.greeting == null) {
                def.dialogues.greeting = new ArrayList<>();
                shouldSave = true;
            }
            if (def.dialogues.inProgress == null) {
                def.dialogues.inProgress = new ArrayList<>();
                shouldSave = true;
            }
            if (def.dialogues.readyToClaim == null) {
                def.dialogues.readyToClaim = new ArrayList<>();
                shouldSave = true;
            }
            if (def.dialogues.completed == null) {
                def.dialogues.completed = new ArrayList<>();
                shouldSave = true;
            }
            if (def.dialogues.cooldown == null) {
                def.dialogues.cooldown = new ArrayList<>();
                shouldSave = true;
            }
        }

        if (config.questNpcs.isEmpty()) {
            QuestNpcDefinition safari = new QuestNpcDefinition();
            safari.displayName = "Safari Guide";
            safari.skin = "shopkeeper";
            safari.maxActive = 2;
            safari.questPool.add("safari_water_10");
            safari.questPool.add("safari_bug_12");
            safari.questPool.add("safari_forest_species");
            safari.questPool.add("safari_lapras_net");
            safari.questPool.add("safari_gible_ultra");
            safari.questPool.add("safari_shiny_1");
            safari.questPool.add("safari_radiant_1");
            safari.questPool.add("safari_paradox_1");
            safari.dialogues.greeting = List.of("Welcome to the Safari, %player%!", "Tracks are fresh today, trainer.");
            safari.dialogues.inProgress = List.of("Keep going! %progress%", "The Safari rewards patience.");
            safari.dialogues.readyToClaim = List.of("Great work, your reward is ready.");
            safari.dialogues.completed = List.of("Come back later for more Safari quests.");
            safari.dialogues.cooldown = List.of("No new quests yet. Come back later.");
            config.questNpcs.put("safari_guide", safari);

            QuestNpcDefinition ballistic = new QuestNpcDefinition();
            ballistic.displayName = "Ballistics Expert";
            ballistic.skin = "shopkeeper";
            ballistic.maxActive = 2;
            ballistic.questPool.add("ball_specialist_quick");
            ballistic.questPool.add("ball_specialist_dusk");
            ballistic.questPool.add("ball_combo_water_dive");
            ballistic.questPool.add("starter_hunter");
            ballistic.questPool.add("mixed_hunter");
            ballistic.dialogues.greeting = List.of("A true trainer masters every ball.");
            ballistic.dialogues.inProgress = List.of("Technique first, luck second. %progress%");
            ballistic.dialogues.readyToClaim = List.of("Excellent throws. Claim your payment.");
            ballistic.dialogues.completed = List.of("Your throw game is strong. Come back tomorrow.");
            ballistic.dialogues.cooldown = List.of("No drills available right now.");
            config.questNpcs.put("ballistics_expert", ballistic);

            QuestNpcDefinition commander = new QuestNpcDefinition();
            commander.displayName = "Raid Commander";
            commander.skin = "shopkeeper";
            commander.maxActive = 2;
            commander.questPool.add("raid_winner_3");
            commander.questPool.add("raid_winner_7");
            commander.questPool.add("battle_streak_5");
            commander.questPool.add("battle_streak_15");
            commander.questPool.add("tower_champion");
            commander.questPool.add("legendary_echo");
            commander.questPool.add("mythical_echo");
            commander.dialogues.greeting = List.of("Ready for combat operations, %player%?");
            commander.dialogues.inProgress = List.of("Your squad is advancing. %progress%");
            commander.dialogues.readyToClaim = List.of("Mission complete. Debrief and collect your reward.");
            commander.dialogues.completed = List.of("Stand by for new assignments.");
            commander.dialogues.cooldown = List.of("No operations currently available.");
            config.questNpcs.put("raid_commander", commander);

            shouldSave = true;
        }

        if (shouldSave) {
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(config, writer);
            } catch (IOException e) {
                CobblemonEconomy.LOGGER.error("Failed to save quest NPC config", e);
            }
        }

        return config;
    }
}
