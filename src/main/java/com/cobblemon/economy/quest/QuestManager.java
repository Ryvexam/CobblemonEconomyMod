package com.cobblemon.economy.quest;

import com.cobblemon.economy.fabric.CobblemonEconomy;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestManager {
    private final String url;

    public QuestManager(File dbFile) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            CobblemonEconomy.LOGGER.error("SQLite JDBC driver not found for quest manager", e);
        }
        this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        initDatabase();
    }

    public static class QuestState {
        public final UUID uuid;
        public final String npcId;
        public final String questId;
        public final String status;
        public final long acceptedAt;
        public final long completedAt;
        public final long claimedAt;
        public final long availableAt;

        public QuestState(UUID uuid, String npcId, String questId, String status, long acceptedAt, long completedAt, long claimedAt, long availableAt) {
            this.uuid = uuid;
            this.npcId = npcId;
            this.questId = questId;
            this.status = status;
            this.acceptedAt = acceptedAt;
            this.completedAt = completedAt;
            this.claimedAt = claimedAt;
            this.availableAt = availableAt;
        }
    }

    public static class ObjectiveProgress {
        public final int objectiveIndex;
        public final int progress;

        public ObjectiveProgress(int objectiveIndex, int progress) {
            this.objectiveIndex = objectiveIndex;
            this.progress = progress;
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void initDatabase() {
        String stateSql = "CREATE TABLE IF NOT EXISTS quest_state (" +
                "uuid TEXT NOT NULL," +
                "npc_id TEXT NOT NULL," +
                "quest_id TEXT NOT NULL," +
                "status TEXT NOT NULL," +
                "accepted_at INTEGER NOT NULL," +
                "completed_at INTEGER NOT NULL," +
                "claimed_at INTEGER NOT NULL," +
                "available_at INTEGER NOT NULL," +
                "PRIMARY KEY (uuid, npc_id, quest_id)" +
                ");";

        String progressSql = "CREATE TABLE IF NOT EXISTS quest_progress (" +
                "uuid TEXT NOT NULL," +
                "npc_id TEXT NOT NULL," +
                "quest_id TEXT NOT NULL," +
                "objective_index INTEGER NOT NULL," +
                "progress INTEGER NOT NULL," +
                "PRIMARY KEY (uuid, npc_id, quest_id, objective_index)" +
                ");";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(stateSql);
            stmt.execute(progressSql);
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to initialize quest database", e);
        }
    }

    public List<QuestState> getPlayerQuestStates(UUID uuid, String npcId) {
        List<QuestState> states = new ArrayList<>();
        String sql = "SELECT uuid, npc_id, quest_id, status, accepted_at, completed_at, claimed_at, available_at FROM quest_state WHERE uuid = ? AND npc_id = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, npcId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                states.add(new QuestState(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("npc_id"),
                        rs.getString("quest_id"),
                        rs.getString("status"),
                        rs.getLong("accepted_at"),
                        rs.getLong("completed_at"),
                        rs.getLong("claimed_at"),
                        rs.getLong("available_at")
                ));
            }
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to load quest states for " + uuid, e);
        }
        return states;
    }

    public QuestState getQuestState(UUID uuid, String npcId, String questId) {
        String sql = "SELECT uuid, npc_id, quest_id, status, accepted_at, completed_at, claimed_at, available_at FROM quest_state WHERE uuid = ? AND npc_id = ? AND quest_id = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, npcId);
            stmt.setString(3, questId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new QuestState(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("npc_id"),
                        rs.getString("quest_id"),
                        rs.getString("status"),
                        rs.getLong("accepted_at"),
                        rs.getLong("completed_at"),
                        rs.getLong("claimed_at"),
                        rs.getLong("available_at")
                );
            }
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to load quest state for " + uuid + " quest " + questId, e);
        }
        return null;
    }

    public int countActiveQuests(UUID uuid, String npcId) {
        String sql = "SELECT COUNT(*) AS c FROM quest_state WHERE uuid = ? AND npc_id = ? AND status = 'ACTIVE'";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, npcId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("c");
            }
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to count active quests for " + uuid, e);
        }
        return 0;
    }

    public boolean acceptQuest(UUID uuid, String npcId, String questId, int objectiveCount) {
        String upsertState = "INSERT INTO quest_state(uuid, npc_id, quest_id, status, accepted_at, completed_at, claimed_at, available_at) VALUES(?, ?, ?, 'ACTIVE', ?, 0, 0, 0) " +
                "ON CONFLICT(uuid, npc_id, quest_id) DO UPDATE SET status = 'ACTIVE', accepted_at = excluded.accepted_at, completed_at = 0";
        String deleteProgress = "DELETE FROM quest_progress WHERE uuid = ? AND npc_id = ? AND quest_id = ?";
        String insertProgress = "INSERT INTO quest_progress(uuid, npc_id, quest_id, objective_index, progress) VALUES(?, ?, ?, ?, 0)";

        long now = System.currentTimeMillis();
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stateStmt = conn.prepareStatement(upsertState);
                 PreparedStatement deleteStmt = conn.prepareStatement(deleteProgress);
                 PreparedStatement progressStmt = conn.prepareStatement(insertProgress)) {
                stateStmt.setString(1, uuid.toString());
                stateStmt.setString(2, npcId);
                stateStmt.setString(3, questId);
                stateStmt.setLong(4, now);
                stateStmt.executeUpdate();

                deleteStmt.setString(1, uuid.toString());
                deleteStmt.setString(2, npcId);
                deleteStmt.setString(3, questId);
                deleteStmt.executeUpdate();

                for (int i = 0; i < objectiveCount; i++) {
                    progressStmt.setString(1, uuid.toString());
                    progressStmt.setString(2, npcId);
                    progressStmt.setString(3, questId);
                    progressStmt.setInt(4, i);
                    progressStmt.addBatch();
                }
                progressStmt.executeBatch();
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to accept quest for " + uuid + " quest " + questId, e);
            return false;
        }
    }

    public List<QuestState> getAllActiveQuests(UUID uuid) {
        List<QuestState> states = new ArrayList<>();
        String sql = "SELECT uuid, npc_id, quest_id, status, accepted_at, completed_at, claimed_at, available_at FROM quest_state WHERE uuid = ? AND status = 'ACTIVE'";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                states.add(new QuestState(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("npc_id"),
                        rs.getString("quest_id"),
                        rs.getString("status"),
                        rs.getLong("accepted_at"),
                        rs.getLong("completed_at"),
                        rs.getLong("claimed_at"),
                        rs.getLong("available_at")
                ));
            }
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to load active quests for " + uuid, e);
        }
        return states;
    }

    public List<ObjectiveProgress> getObjectiveProgress(UUID uuid, String npcId, String questId) {
        List<ObjectiveProgress> progress = new ArrayList<>();
        String sql = "SELECT objective_index, progress FROM quest_progress WHERE uuid = ? AND npc_id = ? AND quest_id = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, npcId);
            stmt.setString(3, questId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                progress.add(new ObjectiveProgress(rs.getInt("objective_index"), rs.getInt("progress")));
            }
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to load objective progress for " + uuid + " quest " + questId, e);
        }
        return progress;
    }

    public int getObjectiveProgress(UUID uuid, String npcId, String questId, int objectiveIndex) {
        String sql = "SELECT progress FROM quest_progress WHERE uuid = ? AND npc_id = ? AND quest_id = ? AND objective_index = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, npcId);
            stmt.setString(3, questId);
            stmt.setInt(4, objectiveIndex);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("progress");
            }
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to read objective progress", e);
        }
        return 0;
    }

    public int incrementObjectiveProgress(UUID uuid, String npcId, String questId, int objectiveIndex, int incrementBy, int targetCount) {
        int current = getObjectiveProgress(uuid, npcId, questId, objectiveIndex);
        int next = Math.min(targetCount, current + Math.max(0, incrementBy));
        String sql = "INSERT INTO quest_progress(uuid, npc_id, quest_id, objective_index, progress) VALUES(?, ?, ?, ?, ?) " +
                "ON CONFLICT(uuid, npc_id, quest_id, objective_index) DO UPDATE SET progress = excluded.progress";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, npcId);
            stmt.setString(3, questId);
            stmt.setInt(4, objectiveIndex);
            stmt.setInt(5, next);
            stmt.executeUpdate();
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to increment objective progress", e);
        }
        return next;
    }

    public void markQuestCompleted(UUID uuid, String npcId, String questId) {
        String sql = "UPDATE quest_state SET status = 'COMPLETED', completed_at = ? WHERE uuid = ? AND npc_id = ? AND quest_id = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, System.currentTimeMillis());
            stmt.setString(2, uuid.toString());
            stmt.setString(3, npcId);
            stmt.setString(4, questId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to mark quest completed", e);
        }
    }

    public void markQuestClaimed(UUID uuid, String npcId, String questId, long availableAt) {
        String sql = "UPDATE quest_state SET status = 'CLAIMED', claimed_at = ?, available_at = ? WHERE uuid = ? AND npc_id = ? AND quest_id = ?";
        try (Connection conn = connect(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            long now = System.currentTimeMillis();
            stmt.setLong(1, now);
            stmt.setLong(2, availableAt);
            stmt.setString(3, uuid.toString());
            stmt.setString(4, npcId);
            stmt.setString(5, questId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to mark quest claimed", e);
        }
    }
}
