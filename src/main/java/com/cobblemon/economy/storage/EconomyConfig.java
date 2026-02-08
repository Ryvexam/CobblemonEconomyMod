package com.cobblemon.economy.storage;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

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

public class EconomyConfig {
    @SerializedName(value = "main_currency", alternate = {"mainCurrency"})
    public String mainCurrency = "cobeco";
    public BigDecimal startingBalance = new BigDecimal(1000);
    public BigDecimal startingPco = new BigDecimal(0);
    public BigDecimal battleVictoryReward = new BigDecimal(100);
    public BigDecimal raidDenVictoryReward = null;
    public BigDecimal cobbleDollarsToPokedollarsRate = BigDecimal.ONE;
    public BigDecimal impactorToPokedollarsRate = BigDecimal.ONE;
    public BigDecimal captureReward = null;
    public BigDecimal newDiscoveryReward = new BigDecimal(100);
    public BigDecimal battleVictoryPcoReward = new BigDecimal(10);
    public BigDecimal battleTowerCompletionPcoBonus = new BigDecimal(2);

    public BigDecimal shinyMultiplier = new BigDecimal(5);
    public BigDecimal radiantMultiplier = new BigDecimal(6);
    public BigDecimal legendaryMultiplier = new BigDecimal(10);
    public BigDecimal paradoxMultiplier = new BigDecimal(3);

    public boolean enableProfiling = false;
    public int profilingThresholdMs = 5;

    public transient Map<String, BigDecimal> captureMilestones = new HashMap<>();

    public Map<String, ShopDefinition> shops = new HashMap<>();

    public static class ShopDefinition {
        public String title = "Shop";
        public String currency = "POKE"; 
        public String skin = "shopkeeper";
        public boolean isSellShop = false;
        public String linkedShop = null; // Optional: ID of a linked shop (e.g., link a buy shop to a sell shop)
        public String linkedShopIcon = null; // Optional: Custom icon for the linked shop button (e.g., "minecraft:diamond")
        public List<ShopItemDefinition> items = new ArrayList<>();
    }

    public static class DisplayItemDefinition {
        public String material;
        public String displayname;
        public Boolean enchantEffect = false;
    }

    public static class ShopItemDefinition {
        public String type = "item"; // "item" or "command"
        public String id;
        public String name;
        public int price;
        public String nbt;
        public List<String> dropTable;
        public String lootTable; // Minecraft loot table resource location (e.g., "minecraft:chests/simple_dungeon")
        public Map<String, String> components;
        public Integer buyLimit;
        public Integer buyCooldownMinutes;
        public Integer sellLimit;
        public Integer sellCooldownMinutes;
        public String command; // Command to execute when type is "command"
        public DisplayItemDefinition displayItem; // Custom display item when type is "command"

        public ShopItemDefinition(String id, String name, int price) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.nbt = null;
            this.dropTable = null;
            this.lootTable = null;
            this.components = null;
            this.buyLimit = null;
            this.buyCooldownMinutes = null;
            this.sellLimit = null;
            this.sellCooldownMinutes = null;
            this.command = null;
            this.displayItem = null;
        }
    }

    private static ShopItemDefinition createItem(String id, String name, int price) {
        ShopItemDefinition def = new ShopItemDefinition(id, name, price);
        def.type = "item";
        return def;
    }

    public static EconomyConfig load(File configFile) {
        return load(configFile, null);
    }

    public static EconomyConfig load(File configFile, File shopsFile) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        EconomyConfig config = null;
        boolean isNewConfig = !configFile.exists();
        boolean shopsDirty = false;
        File milestoneFile = new File(configFile.getParentFile(), "milestone.json");

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

        Map<String, ShopDefinition> loadedShops = null;
        if (shopsFile != null) {
            loadedShops = loadShops(gson, shopsFile);
        }
        if ((loadedShops == null || loadedShops.isEmpty()) && config.shops != null && !config.shops.isEmpty()) {
            loadedShops = new HashMap<>(config.shops);
            if (shopsFile != null && !shopsFile.exists()) {
                shopsDirty = true;
            }
        }
        if (loadedShops == null) {
            loadedShops = new HashMap<>();
        }
        config.shops = loadedShops;

        config.captureMilestones = loadMilestones(gson, milestoneFile);

        if (config.mainCurrency == null || config.mainCurrency.isBlank()) {
            config.mainCurrency = "cobeco";
        } else {
            String normalized = config.mainCurrency.trim().toLowerCase(Locale.ROOT);
            if (!normalized.equals("cobeco") && !normalized.equals("cobbledollars") && !normalized.equals("impactor")) {
                CobblemonEconomy.LOGGER.warn("Unknown mainCurrency '{}', defaulting to 'cobeco'", config.mainCurrency);
                normalized = "cobeco";
            }
            config.mainCurrency = normalized;
        }

        if (config.battleVictoryReward == null) {
            config.battleVictoryReward = new BigDecimal(100);
        }

        if (config.raidDenVictoryReward == null) {
            config.raidDenVictoryReward = config.battleVictoryReward;
        }

        if (config.cobbleDollarsToPokedollarsRate == null) {
            config.cobbleDollarsToPokedollarsRate = BigDecimal.ONE;
        }

        if (config.impactorToPokedollarsRate == null) {
            config.impactorToPokedollarsRate = BigDecimal.ONE;
        }

        if (config.battleTowerCompletionPcoBonus == null) {
            config.battleTowerCompletionPcoBonus = new BigDecimal(2);
        }

        if (config.captureReward == null) {
            config.captureReward = config.battleVictoryReward;
        }
        
        // Validate multipliers are not null (can happen with malformed configs)
        if (config.shinyMultiplier == null) {
            config.shinyMultiplier = new BigDecimal(5);
        }
        if (config.radiantMultiplier == null) {
            config.radiantMultiplier = new BigDecimal(6);
        }
        if (config.legendaryMultiplier == null) {
            config.legendaryMultiplier = new BigDecimal(10);
        }
        if (config.paradoxMultiplier == null) {
            config.paradoxMultiplier = new BigDecimal(3);
        }

        if (config.profilingThresholdMs <= 0) {
            config.profilingThresholdMs = 5;
        }

        // --- Validate and Clean Config ---
        for (Map.Entry<String, ShopDefinition> entry : config.shops.entrySet()) {
            ShopDefinition shop = entry.getValue();
            if (shop.items == null) {
                shop.items = new ArrayList<>();
                shopsDirty = true;
            }
            int beforeSize = shop.items.size();
            shop.items.removeIf(item -> item == null || item.id == null);
            if (shop.items.size() != beforeSize) {
                shopsDirty = true;
            }
        }

        // --- Add defaults ONLY if it's a fresh install ---
        boolean modified = isNewConfig;

        if (config.shops.isEmpty()) {
            // 1. General Shop
            ShopDefinition defaultPoke = new ShopDefinition();
            defaultPoke.title = "GENERAL SHOP";
            defaultPoke.currency = "POKE";
            defaultPoke.skin = "shopkeeper";
            defaultPoke.items.add(createItem("cobblemon:poke_ball", "Poké Ball", 200));
            defaultPoke.items.add(createItem("cobblemon:great_ball", "Great Ball", 600));
            defaultPoke.items.add(createItem("cobblemon:ultra_ball", "Ultra Ball", 1200));
            defaultPoke.items.add(createItem("cobblemon:potion", "Potion", 200));
            defaultPoke.items.add(createItem("cobblemon:super_potion", "Super Potion", 700));
            defaultPoke.items.add(createItem("cobblemon:revive", "Revive", 2000));
            config.shops.put("default_poke", defaultPoke);

            // 2. Apothecary (Buy Shop)
            ShopDefinition apothecary = new ShopDefinition();
            apothecary.title = "APOTHECARY";
            apothecary.currency = "POKE";
            apothecary.skin = "shopkeeper";
            apothecary.linkedShop = "apothecary_sell"; // Link to sell shop
            apothecary.linkedShopIcon = "cobblemon:potion"; // Custom icon for the linked shop button
            apothecary.items.add(createItem("cobblemon:potion", "Potion", 200));
            apothecary.items.add(createItem("cobblemon:super_potion", "Super Potion", 700));
            apothecary.items.add(createItem("cobblemon:hyper_potion", "Hyper Potion", 1500));
            apothecary.items.add(createItem("cobblemon:max_potion", "Max Potion", 2500));
            apothecary.items.add(createItem("cobblemon:full_restore", "Full Restore", 3000));
            apothecary.items.add(createItem("cobblemon:revive", "Revive", 2000));
            apothecary.items.add(createItem("cobblemon:max_revive", "Max Revive", 4000));
            apothecary.items.add(createItem("cobblemon:antidote", "Antidote", 200));
            apothecary.items.add(createItem("cobblemon:paralyze_heal", "Parlyz Heal", 200));
            apothecary.items.add(createItem("cobblemon:awakening", "Awakening", 200));
            apothecary.items.add(createItem("cobblemon:burn_heal", "Burn Heal", 200));
            apothecary.items.add(createItem("cobblemon:ice_heal", "Ice Heal", 200));
            apothecary.items.add(createItem("cobblemon:full_heal", "Full Heal", 400));
            apothecary.items.add(createItem("cobblemon:escape_rope", "Escape Rope", 300));
            config.shops.put("apothecary", apothecary);

            // 2b. Apothecary Sell Shop (Linked to Apothecary)
            ShopDefinition apothecary_sell = new ShopDefinition();
            apothecary_sell.title = "APOTHECARY - SELL";
            apothecary_sell.currency = "POKE";
            apothecary_sell.isSellShop = true;
            apothecary_sell.skin = "shopkeeper";
            apothecary_sell.linkedShop = "apothecary"; // Link back to buy shop
            apothecary_sell.linkedShopIcon = "cobblemon:super_potion"; // Custom icon for the linked shop button
            apothecary_sell.items.add(createItem("cobblemon:potion", "Potion", 100));
            apothecary_sell.items.add(createItem("cobblemon:super_potion", "Super Potion", 350));
            apothecary_sell.items.add(createItem("cobblemon:hyper_potion", "Hyper Potion", 750));
            apothecary_sell.items.add(createItem("cobblemon:max_potion", "Max Potion", 1250));
            apothecary_sell.items.add(createItem("cobblemon:full_restore", "Full Restore", 1500));
            apothecary_sell.items.add(createItem("cobblemon:revive", "Revive", 1000));
            apothecary_sell.items.add(createItem("cobblemon:max_revive", "Max Revive", 2000));
            config.shops.put("apothecary_sell", apothecary_sell);

            // 3. Ball Emporium
            ShopDefinition ballShop = new ShopDefinition();
            ballShop.title = "BALL EMPORIUM";
            ballShop.currency = "POKE";
            ballShop.skin = "shopkeeper";
            ballShop.items.add(createItem("cobblemon:poke_ball", "Poké Ball", 200));
            ballShop.items.add(createItem("cobblemon:great_ball", "Great Ball", 600));
            ballShop.items.add(createItem("cobblemon:ultra_ball", "Ultra Ball", 1200));
            ballShop.items.add(createItem("cobblemon:premier_ball", "Premier Ball", 200));
            ballShop.items.add(createItem("cobblemon:heal_ball", "Heal Ball", 300));
            ballShop.items.add(createItem("cobblemon:net_ball", "Net Ball", 1000));
            ballShop.items.add(createItem("cobblemon:nest_ball", "Nest Ball", 1000));
            ballShop.items.add(createItem("cobblemon:dive_ball", "Dive Ball", 1000));
            ballShop.items.add(createItem("cobblemon:dusk_ball", "Dusk Ball", 1000));
            ballShop.items.add(createItem("cobblemon:timer_ball", "Timer Ball", 1000));
            ballShop.items.add(createItem("cobblemon:quick_ball", "Quick Ball", 1000));
            ballShop.items.add(createItem("cobblemon:repeat_ball", "Repeat Ball", 1000));
            ballShop.items.add(createItem("cobblemon:luxury_ball", "Luxury Ball", 3000));
            ballShop.items.add(createItem("cobblemon:ancient_poke_ball", "Ancient Poké Ball", 500));
            ballShop.items.add(createItem("cobblemon:ancient_great_ball", "Ancient Great Ball", 800));
            ballShop.items.add(createItem("cobblemon:ancient_ultra_ball", "Ancient Ultra Ball", 1500));
            ballShop.items.add(createItem("cobblemon:ancient_heavy_ball", "Ancient Heavy Ball", 3500));
            ballShop.items.add(createItem("cobblemon:ancient_leaden_ball", "Ancient Leaden Ball", 3500));
            ballShop.items.add(createItem("cobblemon:ancient_gigaton_ball", "Ancient Gigaton Ball", 4000));
            ballShop.items.add(createItem("cobblemon:ancient_feather_ball", "Ancient Feather Ball", 3500));
            ballShop.items.add(createItem("cobblemon:ancient_wing_ball", "Ancient Wing Ball", 3500));
            ballShop.items.add(createItem("cobblemon:ancient_jet_ball", "Ancient Jet Ball", 4000));
            config.shops.put("ball_emporium", ballShop);

            // 4. Jeweler (Sell)
            ShopDefinition jeweler = new ShopDefinition();
            jeweler.title = "JEWELER";
            jeweler.currency = "POKE";
            jeweler.isSellShop = true;
            jeweler.skin = "shopkeeper";
            jeweler.linkedShop = "jeweler_buy"; // Link to buy shop
            jeweler.items.add(createItem("minecraft:coal", "Coal", 2));
            jeweler.items.add(createItem("minecraft:iron_ingot", "Iron Ingot", 10));
            jeweler.items.add(createItem("minecraft:copper_ingot", "Copper Ingot", 5));
            jeweler.items.add(createItem("minecraft:gold_ingot", "Gold Ingot", 25));
            jeweler.items.add(createItem("minecraft:lapis_lazuli", "Lapis Lazuli", 15));
            jeweler.items.add(createItem("minecraft:redstone", "Redstone", 5));
            jeweler.items.add(createItem("minecraft:diamond", "Diamond", 150));
            jeweler.items.add(createItem("minecraft:emerald", "Emerald", 100));
            jeweler.items.add(createItem("minecraft:amethyst_shard", "Amethyst Shard", 20));
            jeweler.items.add(createItem("minecraft:netherite_scrap", "Netherite Scrap", 500));
            jeweler.items.add(createItem("cobblemon:fire_stone", "Fire Stone", 500));
            jeweler.items.add(createItem("cobblemon:water_stone", "Water Stone", 500));
            jeweler.items.add(createItem("cobblemon:thunder_stone", "Thunder Stone", 500));
            jeweler.items.add(createItem("cobblemon:leaf_stone", "Leaf Stone", 500));
            jeweler.items.add(createItem("cobblemon:moon_stone", "Moon Stone", 750));
            jeweler.items.add(createItem("cobblemon:sun_stone", "Sun Stone", 750));
            jeweler.items.add(createItem("cobblemon:shiny_stone", "Shiny Stone", 750));
            jeweler.items.add(createItem("cobblemon:dusk_stone", "Dusk Stone", 750));
            jeweler.items.add(createItem("cobblemon:dawn_stone", "Dawn Stone", 750));
            jeweler.items.add(createItem("cobblemon:ice_stone", "Ice Stone", 750));
            jeweler.items.add(createItem("cobblemon:oval_stone", "Oval Stone", 300));
            jeweler.items.add(createItem("cobblemon:everstone", "Everstone", 200));
            config.shops.put("jeweler", jeweler);

            // 4b. Jeweler Buy Shop (Linked to Jeweler)
            ShopDefinition jeweler_buy = new ShopDefinition();
            jeweler_buy.title = "JEWELER - BUY";
            jeweler_buy.currency = "POKE";
            jeweler_buy.skin = "shopkeeper";
            jeweler_buy.linkedShop = "jeweler"; // Link back to sell shop
            jeweler_buy.items.add(createItem("cobblemon:fire_stone", "Fire Stone", 1000));
            jeweler_buy.items.add(createItem("cobblemon:water_stone", "Water Stone", 1000));
            jeweler_buy.items.add(createItem("cobblemon:thunder_stone", "Thunder Stone", 1000));
            jeweler_buy.items.add(createItem("cobblemon:leaf_stone", "Leaf Stone", 1000));
            jeweler_buy.items.add(createItem("cobblemon:moon_stone", "Moon Stone", 1500));
            jeweler_buy.items.add(createItem("cobblemon:sun_stone", "Sun Stone", 1500));
            jeweler_buy.items.add(createItem("cobblemon:shiny_stone", "Shiny Stone", 1500));
            jeweler_buy.items.add(createItem("cobblemon:dusk_stone", "Dusk Stone", 1500));
            jeweler_buy.items.add(createItem("cobblemon:dawn_stone", "Dawn Stone", 1500));
            jeweler_buy.items.add(createItem("cobblemon:ice_stone", "Ice Stone", 1500));
            jeweler_buy.items.add(createItem("cobblemon:oval_stone", "Oval Stone", 600));
            jeweler_buy.items.add(createItem("cobblemon:everstone", "Everstone", 400));
            config.shops.put("jeweler_buy", jeweler_buy);

            // 5. Battle Rewards
            ShopDefinition battleRewards = new ShopDefinition();
            battleRewards.title = "BATTLE REWARDS";
            battleRewards.currency = "PCO";
            battleRewards.skin = "shopkeeper";
            ShopItemDefinition rareCandy = new ShopItemDefinition("cobblemon:rare_candy", "Rare Candy", 50);
            rareCandy.type = "item";
            rareCandy.buyLimit = 3;
            rareCandy.buyCooldownMinutes = 1200;
            battleRewards.items.add(rareCandy);

            ShopItemDefinition masterBall = new ShopItemDefinition("cobblemon:master_ball", "Master Ball", 500);
            masterBall.type = "item";
            masterBall.buyLimit = 1;
            masterBall.buyCooldownMinutes = 1440;
            battleRewards.items.add(masterBall);
            battleRewards.items.add(createItem("cobblemon:choice_band", "Choice Band", 150));
            battleRewards.items.add(createItem("cobblemon:choice_specs", "Choice Specs", 150));
            battleRewards.items.add(createItem("cobblemon:choice_scarf", "Choice Scarf", 150));
            battleRewards.items.add(createItem("cobblemon:life_orb", "Life Orb", 200));
            battleRewards.items.add(createItem("cobblemon:assault_vest", "Assault Vest", 150));
            battleRewards.items.add(createItem("cobblemon:focus_sash", "Focus Sash", 100));
            ShopItemDefinition boosterPack = new ShopItemDefinition("academy:booster_pack", "Oui", 200);
            boosterPack.type = "item";
            boosterPack.components = new HashMap<>();
            boosterPack.components.put("academy:booster_pack", "\"base\"");
            battleRewards.items.add(boosterPack);
            battleRewards.items.add(createItem("cobblemon:ability_capsule", "Ability Capsule", 250));
            ShopItemDefinition abilityPatch = new ShopItemDefinition("cobblemon:ability_patch", "Ability Patch", 1000);
            abilityPatch.type = "item";
            abilityPatch.buyLimit = 1;
            abilityPatch.buyCooldownMinutes = 720;
            battleRewards.items.add(abilityPatch);
            config.shops.put("battle_rewards", battleRewards);

            // 6. Berry Gardener
            ShopDefinition berryShop = new ShopDefinition();
            berryShop.title = "BERRY GARDENER";
            berryShop.currency = "POKE";
            berryShop.skin = "shopkeeper";
            berryShop.items.add(createItem("cobblemon:oran_berry", "Oran Berry", 50));
            berryShop.items.add(createItem("cobblemon:sitrus_berry", "Sitrus Berry", 150));
            berryShop.items.add(createItem("cobblemon:lum_berry", "Lum Berry", 200));
            berryShop.items.add(createItem("cobblemon:leppa_berry", "Leppa Berry", 300));
            berryShop.items.add(createItem("cobblemon:cheri_berry", "Cheri Berry", 100));
            berryShop.items.add(createItem("cobblemon:pecha_berry", "Pecha Berry", 100));
            berryShop.items.add(createItem("cobblemon:rawst_berry", "Rawst Berry", 100));
            config.shops.put("berry_gardener", berryShop);

            // 7. Surprise Shop
            ShopDefinition surpriseShop = new ShopDefinition();
            surpriseShop.title = "SURPRISE SHOP";
            surpriseShop.currency = "POKE";
            surpriseShop.skin = "shopkeeper";
            surpriseShop.items.add(createItem("minecraft:*", "Random Minecraft Item", 500));
            surpriseShop.items.add(createItem("minecraft:*", "Random Minecraft Item", 500));
            surpriseShop.items.add(createItem("cobblemon:*", "Random Cobblemon Item", 1000));
            surpriseShop.items.add(createItem("cobblemon:*", "Random Cobblemon Item", 1000));
            config.shops.put("surprise_shop", surpriseShop);

            // 8. Black Market
            ShopDefinition blackMarket = new ShopDefinition();
            blackMarket.title = "BLACK MARKET";
            blackMarket.currency = "PCO";
            blackMarket.skin = "shopkeeper";
            ShopItemDefinition lootBox = new ShopItemDefinition("minecraft:black_shulker_box", "Suspicious Crate", 100);
            lootBox.type = "item";
            lootBox.buyLimit = 3;
            lootBox.buyCooldownMinutes = 1200;
            lootBox.dropTable = new ArrayList<>();
            lootBox.dropTable.add("cobblemon:master_ball");
            lootBox.dropTable.add("cobblemon:rare_candy");
            lootBox.dropTable.add("cobblemon:ability_patch");
            lootBox.dropTable.add("minecraft:netherite_ingot");
            lootBox.dropTable.add("minecraft:diamond_block");
            blackMarket.items.add(lootBox);
            config.shops.put("black_market", blackMarket);

            // 9. Example: Command Shop (commented out by default)
            // This demonstrates how to create items that execute commands instead of giving items
            /*
            ShopDefinition commandShop = new ShopDefinition();
            commandShop.title = "COMMAND SHOP";
            commandShop.currency = "PCO";
            commandShop.skin = "shopkeeper";
            ShopItemDefinition voteKey = new ShopItemDefinition("minecraft:air", "Vote Key", 50);
            voteKey.type = "command";
            voteKey.command = "crate key give vote 1 %player%";
            voteKey.displayItem = new DisplayItemDefinition();
            voteKey.displayItem.material = "supplementaries:key";
            voteKey.displayItem.displayname = "Vote Key";
            voteKey.displayItem.enchantEffect = true;
            voteKey.buyLimit = 1;
            voteKey.buyCooldownMinutes = 1440;
            commandShop.items.add(voteKey);
            config.shops.put("command_shop", commandShop);
            */

            modified = true;
            shopsDirty = true;
        }

        // Save if modified
        if (modified) {
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
            } catch (IOException e) {
                CobblemonEconomy.LOGGER.error("Failed to save updated config", e);
            }
        }

        if (shopsFile != null && (!shopsFile.exists() || shopsDirty)) {
            saveShops(gson, shopsFile, config.shops);
        }

        return config;
    }

    private static class ShopsFileModel {
        Map<String, ShopDefinition> shops = new HashMap<>();
    }

    private static Map<String, ShopDefinition> loadShops(Gson gson, File shopsFile) {
        if (shopsFile == null || !shopsFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(shopsFile)) {
            ShopsFileModel wrapped = gson.fromJson(reader, ShopsFileModel.class);
            if (wrapped != null && wrapped.shops != null && !wrapped.shops.isEmpty()) {
                return wrapped.shops;
            }
        } catch (Exception ignored) {
        }

        try (FileReader reader = new FileReader(shopsFile)) {
            return gson.fromJson(reader, new TypeToken<Map<String, ShopDefinition>>() {}.getType());
        } catch (Exception e) {
            CobblemonEconomy.LOGGER.error("Failed to load shops config", e);
            return null;
        }
    }

    private static void saveShops(Gson gson, File shopsFile, Map<String, ShopDefinition> shops) {
        ShopsFileModel model = new ShopsFileModel();
        model.shops = shops == null ? new HashMap<>() : shops;
        try (FileWriter writer = new FileWriter(shopsFile)) {
            gson.toJson(model, writer);
        } catch (IOException e) {
            CobblemonEconomy.LOGGER.error("Failed to save shops config", e);
        }
    }

    private static Map<String, BigDecimal> loadMilestones(Gson gson, File milestoneFile) {
        Map<String, BigDecimal> milestones = new HashMap<>();
        boolean shouldSave = false;
        if (milestoneFile.exists()) {
            try (FileReader reader = new FileReader(milestoneFile)) {
                Map<String, BigDecimal> loaded = gson.fromJson(reader, new TypeToken<Map<String, BigDecimal>>() {}.getType());
                if (loaded != null) {
                    for (Map.Entry<String, BigDecimal> entry : loaded.entrySet()) {
                        if (entry.getKey() != null && entry.getValue() != null) {
                            milestones.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            } catch (Exception e) {
                CobblemonEconomy.LOGGER.error("Failed to load milestone config", e);
                shouldSave = true;
            }
        } else {
            shouldSave = true;
        }

        if (milestones.isEmpty()) {
            milestones.put("10", new BigDecimal(300));
            milestones.put("50", new BigDecimal(700));
            milestones.put("100", new BigDecimal(1500));
            milestones.put("200", new BigDecimal(3000));
            milestones.put("300", new BigDecimal(6000));
            shouldSave = true;
        }

        if (shouldSave) {
            saveMilestones(gson, milestoneFile, milestones);
        }

        return milestones;
    }

    private static void saveMilestones(Gson gson, File milestoneFile, Map<String, BigDecimal> milestones) {
        try (FileWriter writer = new FileWriter(milestoneFile)) {
            gson.toJson(milestones, writer);
        } catch (IOException e) {
            CobblemonEconomy.LOGGER.error("Failed to save milestone config", e);
        }
    }
}
