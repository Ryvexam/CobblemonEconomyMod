package com.cobblemon.economy.quest;

import com.cobblemon.economy.storage.SqliteMigrationRunner;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class QuestDatabaseSchema {
    public static final int CURRENT_VERSION = 1;

    private QuestDatabaseSchema() {
    }

    public static void migrate(File database) throws SQLException, IOException {
        SqliteMigrationRunner.migrate(database, "quest", CURRENT_VERSION,
                List.of(QuestDatabaseSchema::migrateToVersionOne));
    }

    private static void migrateToVersionOne(Connection connection) throws SQLException {
        execute(connection, "CREATE TABLE IF NOT EXISTS quest_state ("
                + "uuid TEXT NOT NULL,"
                + "npc_id TEXT NOT NULL,"
                + "quest_id TEXT NOT NULL,"
                + "status TEXT NOT NULL,"
                + "accepted_at INTEGER NOT NULL,"
                + "completed_at INTEGER NOT NULL,"
                + "claimed_at INTEGER NOT NULL,"
                + "available_at INTEGER NOT NULL,"
                + "PRIMARY KEY (uuid, npc_id, quest_id)"
                + ");");
        execute(connection, "CREATE TABLE IF NOT EXISTS quest_progress ("
                + "uuid TEXT NOT NULL,"
                + "npc_id TEXT NOT NULL,"
                + "quest_id TEXT NOT NULL,"
                + "objective_index INTEGER NOT NULL,"
                + "progress INTEGER NOT NULL,"
                + "PRIMARY KEY (uuid, npc_id, quest_id, objective_index)"
                + ");");
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
