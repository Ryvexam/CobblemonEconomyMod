package com.cobblemon.economy.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;

public class EconomyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("cobblemon-economy-config.json").toFile();

    public BigDecimal catchReward = BigDecimal.valueOf(50);
    public BigDecimal newDiscoveryReward = BigDecimal.valueOf(100);
    public BigDecimal battleVictoryReward = BigDecimal.valueOf(100);
    public BigDecimal battleVictoryPcoReward = BigDecimal.valueOf(10); // Reward in PCo
    public BigDecimal startingBalance = BigDecimal.valueOf(1000);
    public BigDecimal startingPco = BigDecimal.valueOf(0);

    public static EconomyConfig load() {
        if (!CONFIG_FILE.exists()) {
            EconomyConfig config = new EconomyConfig();
            config.save();
            return config;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            EconomyConfig config = GSON.fromJson(reader, EconomyConfig.class);
            return config != null ? config : new EconomyConfig();
        } catch (IOException e) {
            e.printStackTrace();
            return new EconomyConfig();
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
