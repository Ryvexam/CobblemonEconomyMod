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
        public int visibleQuests = 4;
        public boolean sharedRotation = true;
        public String rotationMode = "MIDNIGHT";
        public int rotationHours = 24;
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
            if (def.visibleQuests <= 0) {
                def.visibleQuests = 4;
                shouldSave = true;
            }
            if (def.rotationMode == null || def.rotationMode.isBlank()) {
                def.rotationMode = "MIDNIGHT";
                shouldSave = true;
            }
            if (def.rotationHours <= 0) {
                def.rotationHours = 24;
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
            safari.visibleQuests = 4;
            safari.sharedRotation = true;
            safari.rotationMode = "MIDNIGHT";
            safari.rotationHours = 24;
            safari.questPool.add("safari_water_10");
            safari.questPool.add("safari_bug_12");
            safari.questPool.add("safari_forest_species");
            safari.questPool.add("safari_lapras_net");
            safari.questPool.add("safari_gible_ultra");
            safari.questPool.add("safari_shiny_1");
            safari.questPool.add("safari_radiant_1");
            safari.questPool.add("safari_paradox_1");
            safari.questPool.add("safari_fly_8");
            safari.questPool.add("safari_poison_10");
            safari.questPool.add("safari_bug_shiny");
            safari.questPool.add("safari_bird_trio");
            safari.questPool.add("safari_apex_trial");
            safari.questPool.add("safari_night_stalkers");
            safari.questPool.add("safari_steel_test");
            safari.dialogues.greeting = List.of("Bienvenue au Safari, %player% !", "Les traces sont fraiches aujourd'hui.");
            safari.dialogues.inProgress = List.of("Continue, tu avances bien. %progress%", "Le Safari recompense la patience.");
            safari.dialogues.readyToClaim = List.of("Excellent travail. Ta recompense t'attend.");
            safari.dialogues.completed = List.of("Reviens plus tard pour de nouvelles missions Safari.");
            safari.dialogues.cooldown = List.of("Aucune nouvelle mission pour l'instant.");
            config.questNpcs.put("safari_guide", safari);

            QuestNpcDefinition fisherman = new QuestNpcDefinition();
            fisherman.displayName = "Harbor Fisherman";
            fisherman.skin = "shopkeeper";
            fisherman.maxActive = 2;
            fisherman.visibleQuests = 4;
            fisherman.sharedRotation = true;
            fisherman.rotationMode = "HOURS";
            fisherman.rotationHours = 12;
            fisherman.questPool.add("fisher_common_12");
            fisherman.questPool.add("fisher_magikarp_6");
            fisherman.questPool.add("fisher_lure_8");
            fisherman.questPool.add("fisher_net_8");
            fisherman.questPool.add("fisher_dive_8");
            fisherman.questPool.add("fisher_lapras_lure");
            fisherman.questPool.add("fisher_feebas_dive");
            fisherman.questPool.add("fisher_shiny_water");
            fisherman.questPool.add("fisher_radiant_tide");
            fisherman.questPool.add("fisher_tournament");
            fisherman.questPool.add("fisher_storm_run");
            fisherman.questPool.add("fossil_research_6");
            fisherman.questPool.add("fossil_shiny_1");
            fisherman.questPool.add("fossil_radiant_1");
            fisherman.dialogues.greeting = List.of("La mer est genereuse aujourd'hui, %player%.", "Pret pour une vraie peche en profondeur ?");
            fisherman.dialogues.inProgress = List.of("Ramene-moi une meilleure prise. %progress%", "La maree n'attend personne.");
            fisherman.dialogues.readyToClaim = List.of("Un pecheur tient sa parole. Voila ta recompense.");
            fisherman.dialogues.completed = List.of("Tu as gagne ta place sur ce quai.");
            fisherman.dialogues.cooldown = List.of("Pas de contrat pour le moment. Reviens a la prochaine maree.");
            config.questNpcs.put("harbor_fisherman", fisherman);

            QuestNpcDefinition archaeologist = new QuestNpcDefinition();
            archaeologist.displayName = "Fossil Curator";
            archaeologist.skin = "shopkeeper";
            archaeologist.maxActive = 2;
            archaeologist.visibleQuests = 4;
            archaeologist.sharedRotation = true;
            archaeologist.rotationMode = "HOURS";
            archaeologist.rotationHours = 12;
            archaeologist.questPool.add("fossil_research_6");
            archaeologist.questPool.add("fossil_shiny_1");
            archaeologist.questPool.add("fossil_radiant_1");
            archaeologist.questPool.add("fossil_species_pair");
            archaeologist.questPool.add("fossil_legend_1");
            archaeologist.dialogues.greeting = List.of("Chaque fossile est un tresor.", "Le passe respire encore, %player%.");
            archaeologist.dialogues.inProgress = List.of("Le catalogue avance. %progress%", "Travaille avec precision.");
            archaeologist.dialogues.readyToClaim = List.of("Restauration remarquable. Recupere ta prime.");
            archaeologist.dialogues.completed = List.of("L'aile du musee est satisfaite pour le moment.");
            archaeologist.dialogues.cooldown = List.of("Aucune nouvelle mission de fouille pour l'instant.");
            config.questNpcs.put("fossil_curator", archaeologist);

            QuestNpcDefinition explorer = new QuestNpcDefinition();
            explorer.displayName = "Frontier Explorer";
            explorer.skin = "shopkeeper";
            explorer.maxActive = 2;
            explorer.visibleQuests = 4;
            explorer.sharedRotation = true;
            explorer.rotationMode = "MIDNIGHT";
            explorer.rotationHours = 24;
            explorer.questPool.add("explore_nether_6");
            explorer.questPool.add("explore_end_6");
            explorer.questPool.add("explore_three_realms");
            explorer.questPool.add("explore_nether_fire");
            explorer.questPool.add("explore_end_dragon");
            explorer.questPool.add("explore_cave_guard");
            explorer.questPool.add("explore_shiny_trail");
            explorer.questPool.add("explore_paradox_void");
            explorer.questPool.add("explore_legend_path");
            explorer.questPool.add("explore_mythic_path");
            explorer.questPool.add("explore_skies_route");
            explorer.questPool.add("explore_nether_duel");
            explorer.questPool.add("explore_overworld_30");
            explorer.questPool.add("explore_nether_30");
            explorer.questPool.add("explore_safari_30");
            explorer.questPool.add("explore_frontier_30_any");
            explorer.dialogues.greeting = List.of("Les cartes sont pour les touristes. Ici, on fait du terrain.", "Prepare ton sac, %player%. La frontiere t'appelle.");
            explorer.dialogues.inProgress = List.of("Continue d'avancer. %progress%", "La decouverte recompense les tenaces.");
            explorer.dialogues.readyToClaim = List.of("Excellent rapport d'exploration. Paiement valide.");
            explorer.dialogues.completed = List.of("Repose-toi. La prochaine expedition sera plus rude.");
            explorer.dialogues.cooldown = List.of("Aucun briefing d'expedition pour l'instant.");
            config.questNpcs.put("frontier_explorer", explorer);

            QuestNpcDefinition ballistic = new QuestNpcDefinition();
            ballistic.displayName = "Ballistics Expert";
            ballistic.skin = "shopkeeper";
            ballistic.maxActive = 2;
            ballistic.visibleQuests = 4;
            ballistic.sharedRotation = true;
            ballistic.rotationMode = "HOURS";
            ballistic.rotationHours = 8;
            ballistic.questPool.add("ball_specialist_quick");
            ballistic.questPool.add("ball_specialist_dusk");
            ballistic.questPool.add("ball_combo_water_dive");
            ballistic.questPool.add("starter_hunter");
            ballistic.questPool.add("collector_eevee_line");
            ballistic.questPool.add("collector_starters_johto");
            ballistic.questPool.add("collector_psychic_set");
            ballistic.questPool.add("collector_ghost_set");
            ballistic.questPool.add("collector_ice_set");
            ballistic.questPool.add("collector_shiny_pair");
            ballistic.questPool.add("collector_dragon_set");
            ballistic.questPool.add("collector_ground_set");
            ballistic.questPool.add("mixed_hunter");
            ballistic.dialogues.greeting = List.of("Un vrai dresseur maitrise chaque pokeball.");
            ballistic.dialogues.inProgress = List.of("La technique avant la chance. %progress%");
            ballistic.dialogues.readyToClaim = List.of("Lancers impeccables. Recupere ton paiement.");
            ballistic.dialogues.completed = List.of("Ton niveau de lancer est solide. Reviens demain.");
            ballistic.dialogues.cooldown = List.of("Aucun exercice disponible pour le moment.");
            config.questNpcs.put("ballistics_expert", ballistic);

            QuestNpcDefinition duelist = new QuestNpcDefinition();
            duelist.displayName = "Arena Duel Master";
            duelist.skin = "shopkeeper";
            duelist.maxActive = 2;
            duelist.visibleQuests = 4;
            duelist.sharedRotation = true;
            duelist.rotationMode = "HOURS";
            duelist.rotationHours = 6;
            duelist.questPool.add("battle_streak_5");
            duelist.questPool.add("battle_streak_15");
            duelist.questPool.add("duel_battle_30");
            duelist.questPool.add("raid_winner_3");
            duelist.questPool.add("raid_winner_7");
            duelist.questPool.add("duel_raid_5");
            duelist.questPool.add("duel_raid_10");
            duelist.questPool.add("tower_champion");
            duelist.questPool.add("duel_tower_4");
            duelist.questPool.add("duel_elite_combo");
            duelist.questPool.add("duel_capture_fighter");
            duelist.questPool.add("duel_dragon_precision");
            duelist.questPool.add("duel_precision_ball");
            duelist.questPool.add("duel_paradox_hunt");
            duelist.questPool.add("legendary_echo");
            duelist.questPool.add("mythical_echo");
            duelist.questPool.add("duel_grand_champion");
            duelist.dialogues.greeting = List.of("Seuls les dresseurs disciplines survivent ici.", "Montre-moi ta meilleure strategie, %player%.");
            duelist.dialogues.inProgress = List.of("Aucune hesitation. %progress%", "La pression forge les champions.");
            duelist.dialogues.readyToClaim = List.of("Execution parfaite. Recupere ta prime de champion.");
            duelist.dialogues.completed = List.of("Beau combat. Reviens pour la prochaine serie.");
            duelist.dialogues.cooldown = List.of("Le tableau de l'arene est vide pour le moment.");
            config.questNpcs.put("arena_duel_master", duelist);

            QuestNpcDefinition commander = new QuestNpcDefinition();
            commander.displayName = "Raid Commander";
            commander.skin = "shopkeeper";
            commander.maxActive = 1;
            commander.visibleQuests = 4;
            commander.sharedRotation = true;
            commander.rotationMode = "HOURS";
            commander.rotationHours = 8;
            commander.questPool.add("raid_winner_7");
            commander.questPool.add("duel_raid_10");
            commander.questPool.add("duel_elite_combo");
            commander.questPool.add("fossil_legend_1");
            commander.dialogues.greeting = List.of("Nous avons besoin d'operateurs precis pour les raids de haut niveau.");
            commander.dialogues.inProgress = List.of("Garde ta formation serree. %progress%");
            commander.dialogues.readyToClaim = List.of("Operation terminee. Excellent travail.");
            commander.dialogues.completed = List.of("Reserve strategique complete. Reste en attente.");
            commander.dialogues.cooldown = List.of("Aucun contrat de raid actif actuellement.");
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
