package com.cobblemon.economy.storage;

import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigFileStoreTest {
    @TempDir
    File tempDir;

    @Test
    void writesAtomicallyAndKeepsPreviousConfigurationBackup() throws Exception {
        File file = new File(tempDir, "config.json");
        var gson = new GsonBuilder().setPrettyPrinting().create();

        ConfigFileStore.writeAtomically(file, gson, Map.of("value", 1));
        ConfigFileStore.writeAtomically(file, gson, Map.of("value", 2));

        assertTrue(file.isFile());
        assertTrue(new File(tempDir, "config.json.bak").isFile());
        assertEquals(2.0, gson.fromJson(Files.readString(file.toPath()), Map.class).get("value"));
        assertEquals(1.0, gson.fromJson(Files.readString(new File(tempDir, "config.json.bak").toPath()), Map.class).get("value"));
    }

    @Test
    void quarantinesMalformedConfigurationBeforeFallback() throws Exception {
        File file = new File(tempDir, "config.json");
        Files.writeString(file.toPath(), "{ not valid json");

        File quarantined = ConfigFileStore.quarantine(file, "broken");

        assertTrue(quarantined.isFile());
        assertTrue(quarantined.getName().startsWith("config.json.broken-"));
        assertTrue(file.isFile());
        assertEquals("{ not valid json", Files.readString(quarantined.toPath()));
    }
}
