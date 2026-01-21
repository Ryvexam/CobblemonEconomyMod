package com.cobblemon.economy.storage;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EconomyConfig {
    public BigDecimal startingBalance = new BigDecimal(1000);
    public BigDecimal startingPco = new BigDecimal(0);
    public BigDecimal battleVictoryReward = new BigDecimal(100);
    public BigDecimal newDiscoveryReward = new BigDecimal(100);
    public BigDecimal battleVictoryPcoReward = new BigDecimal(10);

    public Map<String, ShopDefinition> shops = new HashMap<>();

    public static class ShopDefinition {
        public String title = "Shop";
        public String currency = "POKE"; 
        public String skin = "shopkeeper"; // Nouveau : Skin par défaut du marchand pour ce shop
        public boolean isSellShop = false; // Defines if the shop is for buying or selling
        public List<ShopItemDefinition> items = new ArrayList<>();
    }

    public static class ShopItemDefinition {
        public String id;
        public String name;
        public int price;

        public ShopItemDefinition(String id, String name, int price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }
    }

    public static EconomyConfig load(File configFile) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        EconomyConfig config = null;

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = gson.fromJson(reader, EconomyConfig.class);
            } catch (IOException e) {
                CobblemonEconomy.LOGGER.error("Failed to load config", e);
            }
        }

        if (config == null) {
            config = new EconomyConfig();
        }

        // If the shops list is empty, populate with defaults
        if (config.shops == null || config.shops.isEmpty()) {
            config.shops = new HashMap<>();
            
            ShopDefinition mainShop = new ShopDefinition();
            mainShop.title = "⭐ GENERAL STORE ⭐";
            mainShop.currency = "POKE";
            mainShop.skin = "shopkeeper";
            mainShop.items.add(new ShopItemDefinition("cobblemon:poke_ball", "Poke Ball", 100));
            mainShop.items.add(new ShopItemDefinition("cobblemon:great_ball", "Great Ball", 300));
            mainShop.items.add(new ShopItemDefinition("cobblemon:ultra_ball", "Ultra Ball", 600));
            mainShop.items.add(new ShopItemDefinition("cobblemon:potion", "Potion", 200));
            mainShop.items.add(new ShopItemDefinition("cobblemon:revive", "Revive", 1500));
            config.shops.put("default_poke", mainShop);

            ShopDefinition ballShop = new ShopDefinition();
            ballShop.title = "⚪ BALL SPECIALIST ⚪";
            ballShop.currency = "POKE";
            ballShop.skin = "shopkeeper";
            
            // Page 1
            ballShop.items.add(new ShopItemDefinition("cobblemon:poke_ball", "Poké Ball", 200));
            ballShop.items.add(new ShopItemDefinition("cobblemon:great_ball", "Great Ball", 600));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ultra_ball", "Ultra Ball", 1200));
            ballShop.items.add(new ShopItemDefinition("cobblemon:master_ball", "Master Ball", 100000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:premier_ball", "Premier Ball", 200));
            ballShop.items.add(new ShopItemDefinition("cobblemon:heal_ball", "Heal Ball", 300));
            ballShop.items.add(new ShopItemDefinition("cobblemon:net_ball", "Net Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:nest_ball", "Nest Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:dive_ball", "Dive Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:dusk_ball", "Dusk Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:timer_ball", "Timer Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:quick_ball", "Quick Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:repeat_ball", "Repeat Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:luxury_ball", "Luxury Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:friend_ball", "Friend Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:level_ball", "Level Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:lure_ball", "Lure Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:moon_ball", "Moon Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:heavy_ball", "Heavy Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:love_ball", "Love Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:fast_ball", "Fast Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:sport_ball", "Sport Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:safari_ball", "Safari Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:dream_ball", "Dream Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:beast_ball", "Beast Ball", 5000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:park_ball", "Park Ball", 500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:cherish_ball", "Cherish Ball", 5000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:gs_ball", "GS Ball", 10000));
            
            // Forcing 2nd page
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_poke_ball", "Ancient Poké Ball", 500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_great_ball", "Ancient Great Ball", 800));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_ultra_ball", "Ancient Ultra Ball", 1500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_heavy_ball", "Ancient Heavy Ball", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_leaden_ball", "Ancient Leaden Ball", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_gigaton_ball", "Ancient Gigaton Ball", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_feather_ball", "Ancient Feather Ball", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_wing_ball", "Ancient Wing Ball", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_jet_ball", "Ancient Jet Ball", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_origin_ball", "Ancient Origin Ball", 10000));
            
            // Page 2 stones
            ballShop.items.add(new ShopItemDefinition("cobblemon:fire_stone", "Fire Stone", 2500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:water_stone", "Water Stone", 2500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:thunder_stone", "Thunder Stone", 2500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:leaf_stone", "Leaf Stone", 2500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:moon_stone", "Moon Stone", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:sun_stone", "Sun Stone", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:shiny_stone", "Shiny Stone", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:dusk_stone", "Dusk Stone", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:dawn_stone", "Dawn Stone", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ice_stone", "Ice Stone", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:oval_stone", "Oval Stone", 1500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:everstone", "Everstone", 1000));

            // Battle items
            ballShop.items.add(new ShopItemDefinition("cobblemon:choice_band", "Choice Band", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:choice_specs", "Choice Specs", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:choice_scarf", "Choice Scarf", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:life_orb", "Life Orb", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:leftovers", "Leftovers", 5000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:assault_vest", "Assault Vest", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:rocky_helmet", "Rocky Helmet", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:focus_sash", "Focus Sash", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:expert_belt", "Expert Belt", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:black_sludge", "Black Sludge", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:flame_orb", "Flame Orb", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:toxic_orb", "Toxic Orb", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:light_clay", "Light Clay", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:mental_herb", "Mental Herb", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:power_herb", "Power Herb", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:white_herb", "White Herb", 1000));

            // Berries
            ballShop.items.add(new ShopItemDefinition("cobblemon:oran_berry", "Oran Berry", 100));
            ballShop.items.add(new ShopItemDefinition("cobblemon:sitrus_berry", "Sitrus Berry", 200));
            ballShop.items.add(new ShopItemDefinition("cobblemon:lum_berry", "Lum Berry", 200));
            ballShop.items.add(new ShopItemDefinition("cobblemon:leppa_berry", "Leppa Berry", 500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:cheri_berry", "Cheri Berry", 150));
            ballShop.items.add(new ShopItemDefinition("cobblemon:chesto_berry", "Chesto Berry", 150));
            ballShop.items.add(new ShopItemDefinition("cobblemon:pecha_berry", "Pecha Berry", 150));
            ballShop.items.add(new ShopItemDefinition("cobblemon:rawst_berry", "Rawst Berry", 150));
            ballShop.items.add(new ShopItemDefinition("cobblemon:aspear_berry", "Aspear Berry", 150));
            ballShop.items.add(new ShopItemDefinition("cobblemon:salac_berry", "Salac Berry", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:petaya_berry", "Petaya Berry", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:apicot_berry", "Apicot Berry", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:lansat_berry", "Lansat Berry", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:starf_berry", "Starf Berry", 2000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:enigma_berry", "Enigma Berry", 2000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:micle_berry", "Micle Berry", 2000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:custap_berry", "Custap Berry", 2000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:jaboca_berry", "Jaboca Berry", 2000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:rowap_berry", "Rowap Berry", 2000));
            
            config.shops.put("ball_shop", ballShop);

            ShopDefinition battleShop = new ShopDefinition();
            battleShop.title = "⚔️ BATTLE REWARDS ⚔️";
            battleShop.currency = "PCO";
            battleShop.skin = "shopkeeper";
            battleShop.items.add(new ShopItemDefinition("cobblemon:rare_candy", "Rare Candy", 50));
            battleShop.items.add(new ShopItemDefinition("cobblemon:master_ball", "Master Ball", 500));
            config.shops.put("default_pco", battleShop);

            ShopDefinition emeraldShop = new ShopDefinition();
            emeraldShop.title = "💎 GEM BUYBACK 💎";
            emeraldShop.currency = "POKE";
            emeraldShop.isSellShop = true;
            emeraldShop.items.add(new ShopItemDefinition("minecraft:emerald", "Emerald", 10));
            config.shops.put("sell_gems", emeraldShop);

            // Save to file
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
            } catch (IOException e) {
                CobblemonEconomy.LOGGER.error("Failed to save config", e);
            }
        }

        return config;
    }
}
