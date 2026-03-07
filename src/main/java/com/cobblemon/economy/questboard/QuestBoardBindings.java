package com.cobblemon.economy.questboard;

import com.cobblemon.economy.fabric.CobblemonEconomy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QuestBoardBindings {
    public Map<String, String> bindings = new HashMap<>();

    public static QuestBoardBindings load(File file) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        QuestBoardBindings result = null;
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                result = gson.fromJson(reader, QuestBoardBindings.class);
            } catch (IOException e) {
                CobblemonEconomy.LOGGER.error("Failed to load quest board bindings", e);
            }
        }

        if (result == null) {
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
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(bindings, writer);
        } catch (IOException e) {
            CobblemonEconomy.LOGGER.error("Failed to save quest board bindings", e);
        }
    }

    public static String key(String dimension, int x, int y, int z) {
        return dimension + ";" + x + ";" + y + ";" + z;
    }
}
