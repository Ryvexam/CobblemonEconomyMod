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

    public BigDecimal shinyMultiplier = new BigDecimal(5);
    public BigDecimal legendaryMultiplier = new BigDecimal(10);
    public BigDecimal paradoxMultiplier = new BigDecimal(3);

    public Map<String, ShopDefinition> shops = new HashMap<>();

    public static class ShopDefinition {
        public String title = "Shop";
        public String currency = "POKE"; 
        public String skin = "shopkeeper";
        public boolean isSellShop = false;
        public List<ShopItemDefinition> items = new ArrayList<>();
    }

    public static class ShopItemDefinition {
        public String id;
        public String name;
        public int price;
        public String nbt;

        public ShopItemDefinition(String id, String name, int price) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.nbt = null;
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

        if (config.shops == null) {
            config.shops = new HashMap<>();
        }

        // --- Validate and Clean Config ---
        for (Map.Entry<String, ShopDefinition> entry : config.shops.entrySet()) {
            ShopDefinition shop = entry.getValue();
            if (shop.items == null) {
                shop.items = new ArrayList<>();
            }
            shop.items.removeIf(item -> item == null || item.id == null);
        }

        // --- Add defaults if missing ---
        boolean modified = false;

        if (!config.shops.containsKey("default_poke")) {
            ShopDefinition defaultPoke = new ShopDefinition();
            defaultPoke.title = "🛒 GENERAL SHOP 🛒";
            defaultPoke.currency = "POKE";
            defaultPoke.skin = "shopkeeper";
            defaultPoke.items.add(new ShopItemDefinition("cobblemon:poke_ball", "Poké Ball", 200));
            defaultPoke.items.add(new ShopItemDefinition("cobblemon:great_ball", "Great Ball", 600));
            defaultPoke.items.add(new ShopItemDefinition("cobblemon:ultra_ball", "Ultra Ball", 1200));
            defaultPoke.items.add(new ShopItemDefinition("cobblemon:potion", "Potion", 200));
            defaultPoke.items.add(new ShopItemDefinition("cobblemon:super_potion", "Super Potion", 700));
            defaultPoke.items.add(new ShopItemDefinition("cobblemon:revive", "Revive", 2000));
            config.shops.put("default_poke", defaultPoke);
            modified = true;
        }

        if (!config.shops.containsKey("apothecary")) {
            ShopDefinition apothecary = new ShopDefinition();
            apothecary.title = "💊 APOTHECARY 💊";
            apothecary.currency = "POKE";
            apothecary.skin = "shopkeeper";
            apothecary.items.add(new ShopItemDefinition("cobblemon:potion", "Potion", 200));
            apothecary.items.add(new ShopItemDefinition("cobblemon:super_potion", "Super Potion", 700));
            apothecary.items.add(new ShopItemDefinition("cobblemon:hyper_potion", "Hyper Potion", 1500));
            apothecary.items.add(new ShopItemDefinition("cobblemon:max_potion", "Max Potion", 2500));
            apothecary.items.add(new ShopItemDefinition("cobblemon:full_restore", "Full Restore", 3000));
            apothecary.items.add(new ShopItemDefinition("cobblemon:revive", "Revive", 2000));
            apothecary.items.add(new ShopItemDefinition("cobblemon:max_revive", "Max Revive", 4000));
            apothecary.items.add(new ShopItemDefinition("cobblemon:antidote", "Antidote", 200));
            apothecary.items.add(new ShopItemDefinition("cobblemon:paralyze_heal", "Parlyz Heal", 200));
            apothecary.items.add(new ShopItemDefinition("cobblemon:awakening", "Awakening", 200));
            apothecary.items.add(new ShopItemDefinition("cobblemon:burn_heal", "Burn Heal", 200));
            apothecary.items.add(new ShopItemDefinition("cobblemon:ice_heal", "Ice Heal", 200));
            apothecary.items.add(new ShopItemDefinition("cobblemon:full_heal", "Full Heal", 400));
            apothecary.items.add(new ShopItemDefinition("cobblemon:escape_rope", "Escape Rope", 300));
            config.shops.put("apothecary", apothecary);
            modified = true;
        }

        if (!config.shops.containsKey("ball_emporium")) {
            ShopDefinition ballShop = new ShopDefinition();
            ballShop.title = "⚪ BALL EMPORIUM ⚪";
            ballShop.currency = "POKE";
            ballShop.skin = "shopkeeper";
            ballShop.items.add(new ShopItemDefinition("cobblemon:poke_ball", "Poké Ball", 200));
            ballShop.items.add(new ShopItemDefinition("cobblemon:great_ball", "Great Ball", 600));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ultra_ball", "Ultra Ball", 1200));
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
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_poke_ball", "Ancient Poké Ball", 500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_great_ball", "Ancient Great Ball", 800));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_ultra_ball", "Ancient Ultra Ball", 1500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_heavy_ball", "Ancient Heavy Ball", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_leaden_ball", "Ancient Leaden Ball", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_gigaton_ball", "Ancient Gigaton Ball", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_feather_ball", "Ancient Feather Ball", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_wing_ball", "Ancient Wing Ball", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_jet_ball", "Ancient Jet Ball", 4000));
            config.shops.put("ball_emporium", ballShop);
            modified = true;
        }

        if (!config.shops.containsKey("jeweler")) {
            ShopDefinition jeweler = new ShopDefinition();
            jeweler.title = "💎 JEWELER 💎";
            jeweler.currency = "POKE";
            jeweler.isSellShop = true;
            jeweler.skin = "shopkeeper";
            jeweler.items.add(new ShopItemDefinition("minecraft:coal", "Coal", 2));
            jeweler.items.add(new ShopItemDefinition("minecraft:iron_ingot", "Iron Ingot", 10));
            jeweler.items.add(new ShopItemDefinition("minecraft:copper_ingot", "Copper Ingot", 5));
            jeweler.items.add(new ShopItemDefinition("minecraft:gold_ingot", "Gold Ingot", 25));
            jeweler.items.add(new ShopItemDefinition("minecraft:lapis_lazuli", "Lapis Lazuli", 15));
            jeweler.items.add(new ShopItemDefinition("minecraft:redstone", "Redstone", 5));
            jeweler.items.add(new ShopItemDefinition("minecraft:diamond", "Diamond", 150));
            jeweler.items.add(new ShopItemDefinition("minecraft:emerald", "Emerald", 100));
            jeweler.items.add(new ShopItemDefinition("minecraft:amethyst_shard", "Amethyst Shard", 20));
            jeweler.items.add(new ShopItemDefinition("minecraft:netherite_scrap", "Netherite Scrap", 500));
            jeweler.items.add(new ShopItemDefinition("cobblemon:fire_stone", "Fire Stone", 500));
            jeweler.items.add(new ShopItemDefinition("cobblemon:water_stone", "Water Stone", 500));
            jeweler.items.add(new ShopItemDefinition("cobblemon:thunder_stone", "Thunder Stone", 500));
            jeweler.items.add(new ShopItemDefinition("cobblemon:leaf_stone", "Leaf Stone", 500));
            jeweler.items.add(new ShopItemDefinition("cobblemon:moon_stone", "Moon Stone", 750));
            jeweler.items.add(new ShopItemDefinition("cobblemon:sun_stone", "Sun Stone", 750));
            jeweler.items.add(new ShopItemDefinition("cobblemon:shiny_stone", "Shiny Stone", 750));
            jeweler.items.add(new ShopItemDefinition("cobblemon:dusk_stone", "Dusk Stone", 750));
            jeweler.items.add(new ShopItemDefinition("cobblemon:dawn_stone", "Dawn Stone", 750));
            jeweler.items.add(new ShopItemDefinition("cobblemon:ice_stone", "Ice Stone", 750));
            jeweler.items.add(new ShopItemDefinition("cobblemon:oval_stone", "Oval Stone", 300));
            jeweler.items.add(new ShopItemDefinition("cobblemon:everstone", "Everstone", 200));
            config.shops.put("jeweler", jeweler);
            modified = true;
        }

        if (!config.shops.containsKey("battle_rewards")) {
            ShopDefinition battleRewards = new ShopDefinition();
            battleRewards.title = "⚔️ BATTLE REWARDS ⚔️";
            battleRewards.currency = "PCO";
            battleRewards.skin = "shopkeeper";
            battleRewards.items.add(new ShopItemDefinition("cobblemon:rare_candy", "Rare Candy", 50));
            battleRewards.items.add(new ShopItemDefinition("cobblemon:master_ball", "Master Ball", 500));
            battleRewards.items.add(new ShopItemDefinition("cobblemon:choice_band", "Choice Band", 150));
            battleRewards.items.add(new ShopItemDefinition("cobblemon:choice_specs", "Choice Specs", 150));
            battleRewards.items.add(new ShopItemDefinition("cobblemon:choice_scarf", "Choice Scarf", 150));
            battleRewards.items.add(new ShopItemDefinition("cobblemon:life_orb", "Life Orb", 200));
            battleRewards.items.add(new ShopItemDefinition("cobblemon:assault_vest", "Assault Vest", 150));
            battleRewards.items.add(new ShopItemDefinition("cobblemon:focus_sash", "Focus Sash", 100));
            battleRewards.items.add(new ShopItemDefinition("cobblemon:ability_capsule", "Ability Capsule", 250));
            battleRewards.items.add(new ShopItemDefinition("cobblemon:ability_patch", "Ability Patch", 1000));
            config.shops.put("battle_rewards", battleRewards);
            modified = true;
        }

        if (!config.shops.containsKey("berry_gardener")) {
            ShopDefinition berryShop = new ShopDefinition();
            berryShop.title = "🍎 BERRY GARDENER 🍎";
            berryShop.currency = "POKE";
            berryShop.skin = "shopkeeper";
            berryShop.items.add(new ShopItemDefinition("cobblemon:oran_berry", "Oran Berry", 50));
            berryShop.items.add(new ShopItemDefinition("cobblemon:sitrus_berry", "Sitrus Berry", 150));
            berryShop.items.add(new ShopItemDefinition("cobblemon:lum_berry", "Lum Berry", 200));
            berryShop.items.add(new ShopItemDefinition("cobblemon:leppa_berry", "Leppa Berry", 300));
            berryShop.items.add(new ShopItemDefinition("cobblemon:cheri_berry", "Cheri Berry", 100));
            berryShop.items.add(new ShopItemDefinition("cobblemon:pecha_berry", "Pecha Berry", 100));
            berryShop.items.add(new ShopItemDefinition("cobblemon:rawst_berry", "Rawst Berry", 100));
            config.shops.put("berry_gardener", berryShop);
            modified = true;
        }

        if (!config.shops.containsKey("surprise_shop")) {
            ShopDefinition surpriseShop = new ShopDefinition();
            surpriseShop.title = "🎁 SURPRISE SHOP 🎁";
            surpriseShop.currency = "POKE";
            surpriseShop.skin = "shopkeeper";
            surpriseShop.items.add(new ShopItemDefinition("minecraft:*", "Random Minecraft Item", 500));
            surpriseShop.items.add(new ShopItemDefinition("minecraft:*", "Random Minecraft Item", 500));
            surpriseShop.items.add(new ShopItemDefinition("cobblemon:*", "Random Cobblemon Item", 1000));
            surpriseShop.items.add(new ShopItemDefinition("cobblemon:*", "Random Cobblemon Item", 1000));
            config.shops.put("surprise_shop", surpriseShop);
            modified = true;
        }

        // Save if modified
        if (modified) {
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
            } catch (IOException e) {
                CobblemonEconomy.LOGGER.error("Failed to save updated config", e);
            }
        }

        return config;
    }
}
