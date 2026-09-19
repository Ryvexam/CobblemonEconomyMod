package com.cobblemon.economy.questboard;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.cobblemon.economy.storage.ConfigFileStore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QuestBoardBindings {
    public int configVersion = 1;
    public Map<String, String> bindings = new HashMap<>();

    public static QuestBoardBindings load(File file) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        QuestBoardBindings result = null;
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                result = gson.fromJson(reader, QuestBoardBindings.class);
            } catch (Exception e) {
                CobblemonEconomy.LOGGER.error("Failed to load quest board bindings", e);
                quarantine(file, "broken");
            }
        }

        if (result == null) {
            if (file.exists()) {
                quarantine(file, "broken");
            }
            result = new QuestBoardBindings();
            save(file, result);
        }

        if (result.bindings == null) {
            result.bindings = new HashMap<>();
            save(file, result);
        }

        return result;
    }

    public static void save(File file, QuestBoardBindings bindings) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            ConfigFileStore.writeAtomically(file, gson, bindings);
        } catch (IOException e) {
            CobblemonEconomy.LOGGER.error("Failed to save quest board bindings", e);
        }
    }

    private static void quarantine(File file, String reason) {
        if (!file.exists()) {
            return;
        }
        try {
            ConfigFileStore.quarantine(file, reason);
        } catch (IOException quarantineError) {
            CobblemonEconomy.LOGGER.error("Failed to quarantine invalid quest board bindings", quarantineError);
        }
    }

    public static String key(String dimension, int x, int y, int z) {
        return dimension + ";" + x + ";" + y + ";" + z;
    }
}
