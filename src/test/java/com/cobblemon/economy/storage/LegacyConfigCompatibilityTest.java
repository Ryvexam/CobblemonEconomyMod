package com.cobblemon.economy.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyConfigCompatibilityTest {
    @TempDir
    File tempDir;

    @Test
    void importsLegacyInlineShopsAndKeepsLegacyAliases() throws Exception {
        File configFile = new File(tempDir, "config.json");
        File shopsFile = new File(tempDir, "shops.json");
        Files.writeString(configFile.toPath(), """
                {
                  "mainCurrency": "cobeco",
                  "captureReward": 25,
                  "shops": {
                    "legacy_shop": {
                      "title": "Legacy Shop",
                      "currency": "POKE",
                      "items": [{"id": "minecraft:stone", "name": "Stone", "price": 2}]
                    }
                  }
                }
                """);

        EconomyConfig config = EconomyConfig.load(configFile, shopsFile);

        assertEquals("cobeco", config.mainCurrency);
        assertEquals("25", config.captureReward.toPlainString());
        assertNotNull(config.shops.get("legacy_shop"));
        assertTrue(shopsFile.isFile());
        JsonObject exported = new Gson().fromJson(Files.readString(shopsFile.toPath()), JsonObject.class);
        assertTrue(exported.getAsJsonObject("shops").has("legacy_shop"));
    }
}
