package com.cobblemon.economy.storage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class EconomyDatabaseSchema {
    public static final int CURRENT_VERSION = 1;

    private EconomyDatabaseSchema() {
    }

    public static void migrate(File database) throws SQLException, IOException {
        SqliteMigrationRunner.migrate(database, "economy", CURRENT_VERSION,
                List.of(EconomyDatabaseSchema::migrateToVersionOne));
    }

    private static void migrateToVersionOne(Connection connection) throws SQLException {
        execute(connection, "CREATE TABLE IF NOT EXISTS balances ("
                + "uuid TEXT PRIMARY KEY,"
                + "balance TEXT NOT NULL,"
                + "pco TEXT NOT NULL,"
                + "username TEXT"
                + ");");
        execute(connection, "CREATE TABLE IF NOT EXISTS purchase_limits ("
                + "uuid TEXT NOT NULL,"
                + "shop_id TEXT NOT NULL,"
                + "item_id TEXT NOT NULL,"
                + "window_start INTEGER NOT NULL,"
                + "count INTEGER NOT NULL,"
                + "PRIMARY KEY (uuid, shop_id, item_id)"
                + ");");
        execute(connection, "CREATE TABLE IF NOT EXISTS sell_limits ("
                + "uuid TEXT NOT NULL,"
                + "shop_id TEXT NOT NULL,"
                + "item_id TEXT NOT NULL,"
                + "window_start INTEGER NOT NULL,"
                + "count INTEGER NOT NULL,"
                + "PRIMARY KEY (uuid, shop_id, item_id)"
                + ");");
        execute(connection, "CREATE TABLE IF NOT EXISTS capture_counts ("
                + "uuid TEXT PRIMARY KEY,"
                + "count INTEGER NOT NULL"
                + ");");
        execute(connection, "CREATE TABLE IF NOT EXISTS capture_milestones ("
                + "uuid TEXT NOT NULL,"
                + "milestone INTEGER NOT NULL,"
                + "PRIMARY KEY (uuid, milestone)"
                + ");");
        ensureColumnExists(connection, "balances", "username", "TEXT");
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static void ensureColumnExists(Connection connection, String table, String column, String type)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                if (column.equalsIgnoreCase(result.getString("name"))) {
                    return;
                }
            }
        }
        execute(connection, "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
    }
}
