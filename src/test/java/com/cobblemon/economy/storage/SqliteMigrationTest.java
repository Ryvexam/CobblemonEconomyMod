package com.cobblemon.economy.storage;

import com.cobblemon.economy.quest.QuestDatabaseSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteMigrationTest {
    @TempDir
    File tempDir;

    @Test
    void migratesLegacyEconomyDatabaseWithoutLosingBalances() throws Exception {
        File database = new File(tempDir, "economy.db");
        UUID player = UUID.randomUUID();
        try (Connection connection = connection(database);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE balances (uuid TEXT PRIMARY KEY, balance TEXT NOT NULL, pco TEXT NOT NULL)");
            statement.execute("INSERT INTO balances(uuid, balance, pco) VALUES ('" + player + "', '123.45', '7')");
        }

        EconomyDatabaseSchema.migrate(database);

        assertEquals(EconomyDatabaseSchema.CURRENT_VERSION, userVersion(database));
        assertTrue(new File(tempDir, "economy.db.bak").isFile());
        assertTrue(columns(database, "balances").contains("username"));
        assertTrue(tables(database).containsAll(Set.of(
                "balances", "purchase_limits", "sell_limits", "capture_counts", "capture_milestones")));
        try (Connection connection = connection(database);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT balance, pco FROM balances WHERE uuid = '" + player + "'")) {
            assertTrue(result.next());
            assertEquals("123.45", result.getString("balance"));
            assertEquals("7", result.getString("pco"));
        }
    }

    @Test
    void migratesLegacyQuestDatabaseWithoutLosingProgress() throws Exception {
        File database = new File(tempDir, "quests.db");
        UUID player = UUID.randomUUID();
        try (Connection connection = connection(database);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE quest_state (uuid TEXT NOT NULL, npc_id TEXT NOT NULL, quest_id TEXT NOT NULL, status TEXT NOT NULL, accepted_at INTEGER NOT NULL, completed_at INTEGER NOT NULL, claimed_at INTEGER NOT NULL, available_at INTEGER NOT NULL, PRIMARY KEY (uuid, npc_id, quest_id))");
            statement.execute("CREATE TABLE quest_progress (uuid TEXT NOT NULL, npc_id TEXT NOT NULL, quest_id TEXT NOT NULL, objective_index INTEGER NOT NULL, progress INTEGER NOT NULL, PRIMARY KEY (uuid, npc_id, quest_id, objective_index))");
            statement.execute("INSERT INTO quest_state VALUES ('" + player + "', 'safari_guide', 'safari_water_10', 'ACTIVE', 1, 0, 0, 0)");
            statement.execute("INSERT INTO quest_progress VALUES ('" + player + "', 'safari_guide', 'safari_water_10', 0, 4)");
        }

        QuestDatabaseSchema.migrate(database);

        assertEquals(QuestDatabaseSchema.CURRENT_VERSION, userVersion(database));
        assertTrue(new File(tempDir, "quests.db.bak").isFile());
        try (Connection connection = connection(database);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT progress FROM quest_progress WHERE uuid = '" + player + "'")) {
            assertTrue(result.next());
            assertEquals(4, result.getInt("progress"));
        }
    }

    @Test
    void refusesFutureEconomySchemaWithoutChangingIt() throws Exception {
        File database = new File(tempDir, "economy.db");
        try (Connection connection = connection(database);
             Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA user_version = 99");
        }

        assertThrows(SQLException.class, () -> EconomyDatabaseSchema.migrate(database));
        assertEquals(99, userVersion(database));
        assertFalse(new File(tempDir, "economy.db.bak").exists());
    }

    private static Connection connection(File file) throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }

    private static int userVersion(File file) throws SQLException {
        try (Connection connection = connection(file);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA user_version")) {
            result.next();
            return result.getInt(1);
        }
    }

    private static Set<String> tables(File file) throws SQLException {
        Set<String> result = new HashSet<>();
        try (Connection connection = connection(file);
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'")) {
            while (rows.next()) {
                result.add(rows.getString(1));
            }
        }
        return result;
    }

    private static Set<String> columns(File file, String table) throws SQLException {
        Set<String> result = new HashSet<>();
        try (Connection connection = connection(file);
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rows.next()) {
                result.add(rows.getString("name"));
            }
        }
        return result;
    }
}
