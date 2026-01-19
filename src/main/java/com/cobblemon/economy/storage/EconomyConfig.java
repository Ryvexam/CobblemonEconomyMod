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
            
            // Page 1
            ballShop.items.add(new ShopItemDefinition("cobblemon:poke_ball", "Poké Ball", 200));
            ballShop.items.add(new ShopItemDefinition("cobblemon:great_ball", "Super Ball", 600));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ultra_ball", "Hyper Ball", 1200));
            ballShop.items.add(new ShopItemDefinition("cobblemon:master_ball", "Master Ball", 100000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:premier_ball", "Honor Ball", 200));
            ballShop.items.add(new ShopItemDefinition("cobblemon:heal_ball", "Soin Ball", 300));
            ballShop.items.add(new ShopItemDefinition("cobblemon:net_ball", "Filet Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:nest_ball", "Faiblo Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:dive_ball", "Scuba Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:dusk_ball", "Sombre Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:timer_ball", "Chrono Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:quick_ball", "Rapide Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:repeat_ball", "Bis Ball", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:luxury_ball", "Luxe Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:friend_ball", "Copain Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:level_ball", "Niveau Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:lure_ball", "Appât Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:moon_ball", "Lune Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:heavy_ball", "Masse Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:love_ball", "Love Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:fast_ball", "Speed Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:sport_ball", "Sport Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:safari_ball", "Safari Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:dream_ball", "Rêve Ball", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:beast_ball", "Ultra Ball (Beast)", 5000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:park_ball", "Parc Ball", 500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:cherish_ball", "Mémoire Ball", 5000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:gs_ball", "GS Ball", 10000));
            
            // Remplissage pour forcer une 2ème page (plus de 36 items)
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_poke_ball", "Poké Ball Ancienne", 500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_great_ball", "Super Ball Ancienne", 800));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_ultra_ball", "Hyper Ball Ancienne", 1500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_heavy_ball", "Masse Ball Ancienne", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_leaden_ball", "Plomb Ball Ancienne", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_gigaton_ball", "Gigaton Ball Ancienne", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_feather_ball", "Plume Ball Ancienne", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_wing_ball", "Aile Ball Ancienne", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_jet_ball", "Jet Ball Ancienne", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ancient_origin_ball", "Origine Ball Ancienne", 10000));
            
            // Remplissage massif pour forcer 3 ou 4 pages
            // Page 2 : Pierres d'évolution
            ballShop.items.add(new ShopItemDefinition("cobblemon:fire_stone", "Pierre Feu", 2500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:water_stone", "Pierre Eau", 2500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:thunder_stone", "Pierre Foudre", 2500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:leaf_stone", "Pierre Plante", 2500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:moon_stone", "Pierre Lune", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:sun_stone", "Pierre Soleil", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:shiny_stone", "Pierre Éclat", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:dusk_stone", "Pierre Nuit", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:dawn_stone", "Pierre Aube", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:ice_stone", "Pierre Glace", 3000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:oval_stone", "Pierre Ovale", 1500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:everstone", "Pierre Stase", 1000));

            // Page 2 suite / Page 3 : Objets de combat
            ballShop.items.add(new ShopItemDefinition("cobblemon:choice_band", "Bandeau Choix", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:choice_specs", "Lunettes Choix", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:choice_scarf", "Mouchoir Choix", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:life_orb", "Orbe Vie", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:leftovers", "Restes", 5000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:assault_vest", "Veste de Combat", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:rocky_helmet", "Casque Brut", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:focus_sash", "Ceinture Force", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:expert_belt", "Ceinture Pro", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:black_sludge", "Boue Noire", 4000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:flame_orb", "Orbe Flamme", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:toxic_orb", "Orbe Toxique", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:light_clay", "Lumargile", 3500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:mental_herb", "Herbe Mental", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:power_herb", "Herbe Pouvoir", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:white_herb", "Herbe Blanche", 1000));

            // Page 3 suite / Page 4 : Baies
            ballShop.items.add(new ShopItemDefinition("cobblemon:oran_berry", "Baie Oran", 100));
            ballShop.items.add(new ShopItemDefinition("cobblemon:sitrus_berry", "Baie Sitrus", 200));
            ballShop.items.add(new ShopItemDefinition("cobblemon:lum_berry", "Baie Prine", 200));
            ballShop.items.add(new ShopItemDefinition("cobblemon:leppa_berry", "Baie Mepo", 500));
            ballShop.items.add(new ShopItemDefinition("cobblemon:cheri_berry", "Baie Ceriz", 150));
            ballShop.items.add(new ShopItemDefinition("cobblemon:chesto_berry", "Baie Maron", 150));
            ballShop.items.add(new ShopItemDefinition("cobblemon:pecha_berry", "Baie Pêcha", 150));
            ballShop.items.add(new ShopItemDefinition("cobblemon:rawst_berry", "Baie Fraive", 150));
            ballShop.items.add(new ShopItemDefinition("cobblemon:aspear_berry", "Baie Willia", 150));
            ballShop.items.add(new ShopItemDefinition("cobblemon:salac_berry", "Baie Sailak", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:petaya_berry", "Baie Pitaye", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:apicot_berry", "Baie Abricot", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:lansat_berry", "Baie Lansat", 1000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:starf_berry", "Baie Frista", 2000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:enigma_berry", "Baie Enigma", 2000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:micle_berry", "Baie Micle", 2000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:custap_berry", "Baie Chérim", 2000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:jaboca_berry", "Baie Jaboca", 2000));
            ballShop.items.add(new ShopItemDefinition("cobblemon:rowap_berry", "Baie Pomroz", 2000));
            
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
