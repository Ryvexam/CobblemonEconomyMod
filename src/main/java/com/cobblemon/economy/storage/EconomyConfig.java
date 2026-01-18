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
        public String title = "Boutique";
        public String currency = "POKE"; 
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

        // Si la liste des shops est vide (nouvelle installation ou mise à jour), on met les défauts
        if (config.shops == null || config.shops.isEmpty()) {
            config.shops = new HashMap<>();
            
            ShopDefinition mainShop = new ShopDefinition();
            mainShop.title = "⭐ BOUTIQUE GÉNÉRALE ⭐";
            mainShop.currency = "POKE";
            mainShop.items.add(new ShopItemDefinition("cobblemon:poke_ball", "Poké Ball", 100));
            mainShop.items.add(new ShopItemDefinition("cobblemon:great_ball", "Great Ball", 300));
            mainShop.items.add(new ShopItemDefinition("cobblemon:ultra_ball", "Ultra Ball", 600));
            mainShop.items.add(new ShopItemDefinition("cobblemon:potion", "Potion", 200));
            mainShop.items.add(new ShopItemDefinition("cobblemon:revive", "Rappel", 1500));
            config.shops.put("default_poke", mainShop);

            ShopDefinition ballShop = new ShopDefinition();
            ballShop.title = "⚪ SPÉCIALISTE BALLS ⚪";
            ballShop.currency = "POKE";
            ballShop.items.add(new ShopItemDefinition("cobblemon:net_ball", "Filet Ball", 400));
            ballShop.items.add(new ShopItemDefinition("cobblemon:dusk_ball", "Sombre Ball", 400));
            ballShop.items.add(new ShopItemDefinition("cobblemon:quick_ball", "Rapide Ball", 1000));
            config.shops.put("ball_shop", ballShop);

            ShopDefinition battleShop = new ShopDefinition();
            battleShop.title = "⚔️ RÉCOMPENSES DE COMBAT ⚔️";
            battleShop.currency = "PCO";
            battleShop.items.add(new ShopItemDefinition("cobblemon:rare_candy", "Super Bonbon", 50));
            battleShop.items.add(new ShopItemDefinition("cobblemon:master_ball", "Master Ball", 500));
            config.shops.put("default_pco", battleShop);

            // On sauvegarde pour que l'utilisateur voit les nouveaux choix
            try (FileWriter writer = new FileWriter(configFile)) {
                gson.toJson(config, writer);
            } catch (IOException e) {
                CobblemonEconomy.LOGGER.error("Failed to save config", e);
            }
        }

        return config;
    }
}
