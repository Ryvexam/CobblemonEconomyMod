package com.cobblemon.economy.storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** Runs additive SQLite migrations while protecting the previous database. */
public final class SqliteMigrationRunner {
    @FunctionalInterface
    public interface Migration {
        void apply(Connection connection) throws SQLException;
    }

    private SqliteMigrationRunner() {
    }

    public static void migrate(File database, String label, int targetVersion, List<Migration> migrations)
            throws SQLException, IOException {
        if (targetVersion < 0 || migrations.size() != targetVersion) {
            throw new IllegalArgumentException("Migration list must contain one entry per version for " + label);
        }

        Path databasePath = database.toPath().toAbsolutePath().normalize();
        Path parent = databasePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        boolean existedBeforeOpen = Files.isRegularFile(databasePath) && Files.size(databasePath) > 0;
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            int currentVersion = readUserVersion(connection);
            if (currentVersion > targetVersion) {
                throw new SQLException("Unsupported future " + label + " database schema version "
                        + currentVersion + " (latest supported: " + targetVersion + ")");
            }
            if (currentVersion == targetVersion) {
                return;
            }

            if (existedBeforeOpen) {
                Path backup = databasePath.resolveSibling(databasePath.getFileName() + ".bak");
                Files.copy(databasePath, backup, StandardCopyOption.REPLACE_EXISTING);
            }

            boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                for (int version = currentVersion + 1; version <= targetVersion; version++) {
                    migrations.get(version - 1).apply(connection);
                    writeUserVersion(connection, version);
                }
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    public static int readUserVersion(File database) throws SQLException {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath())) {
            return readUserVersion(connection);
        }
    }

    private static int readUserVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA user_version")) {
            result.next();
            return result.getInt(1);
        }
    }

    private static void writeUserVersion(Connection connection, int version) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA user_version = " + version);
        }
    }
}
