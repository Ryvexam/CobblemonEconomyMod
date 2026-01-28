package com.cobblemon.economy.storage;

import com.cobblemon.economy.api.EconomyEvents;
import com.cobblemon.economy.fabric.CobblemonEconomy;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
    private final String url;

    public EconomyManager(File dbFile) {
        try {
            // Forcer le chargement du driver SQLite pour éviter "No suitable driver found"
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            CobblemonEconomy.LOGGER.error("SQLite JDBC driver not found!", e);
        }
        this.url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        initDatabase();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void initDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS balances (" +
                     "uuid TEXT PRIMARY KEY," +
                     "balance TEXT NOT NULL," +
                     "pco TEXT NOT NULL" +
                     ");";
        String limitSql = "CREATE TABLE IF NOT EXISTS purchase_limits (" +
                          "uuid TEXT NOT NULL," +
                          "shop_id TEXT NOT NULL," +
                          "item_id TEXT NOT NULL," +
                          "window_start INTEGER NOT NULL," +
                          "count INTEGER NOT NULL," +
                          "PRIMARY KEY (uuid, shop_id, item_id)" +
                          ");";
        String captureCountSql = "CREATE TABLE IF NOT EXISTS capture_counts (" +
                                 "uuid TEXT PRIMARY KEY," +
                                 "count INTEGER NOT NULL" +
                                 ");";
        String captureMilestonesSql = "CREATE TABLE IF NOT EXISTS capture_milestones (" +
                                      "uuid TEXT NOT NULL," +
                                      "milestone INTEGER NOT NULL," +
                                      "PRIMARY KEY (uuid, milestone)" +
                                      ");";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            stmt.execute(limitSql);
            stmt.execute(captureCountSql);
            stmt.execute(captureMilestonesSql);
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to initialize SQLite database", e);
        }
    }

    public int incrementUniqueCapture(UUID uuid) {
        String selectSql = "SELECT count FROM capture_counts WHERE uuid = ?";
        String insertSql = "INSERT INTO capture_counts(uuid, count) VALUES(?, ?)";
        String updateSql = "UPDATE capture_counts SET count = ? WHERE uuid = ?";

        int current = 0;
        try (Connection conn = connect();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, uuid.toString());
            ResultSet rs = selectStmt.executeQuery();
            boolean found = rs.next();
            if (found) {
                current = rs.getInt("count");
            }
            int newCount = current + 1;
            if (!found) {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, uuid.toString());
                    insertStmt.setInt(2, newCount);
                    insertStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, newCount);
                    updateStmt.setString(2, uuid.toString());
                    updateStmt.executeUpdate();
                }
            }
            return newCount;
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to update capture count for " + uuid, e);
        }
        return current;
    }

    public void setCaptureCount(UUID uuid, int count) {
        String selectSql = "SELECT count FROM capture_counts WHERE uuid = ?";
        String insertSql = "INSERT INTO capture_counts(uuid, count) VALUES(?, ?)";
        String updateSql = "UPDATE capture_counts SET count = ? WHERE uuid = ?";

        try (Connection conn = connect();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, uuid.toString());
            ResultSet rs = selectStmt.executeQuery();
            boolean found = rs.next();
            if (!found) {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, uuid.toString());
                    insertStmt.setInt(2, count);
                    insertStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, count);
                    updateStmt.setString(2, uuid.toString());
                    updateStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to set capture count for " + uuid, e);
        }
    }

    public boolean claimCaptureMilestone(UUID uuid, int milestone) {
        String sql = "INSERT OR IGNORE INTO capture_milestones(uuid, milestone) VALUES(?, ?)";
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, milestone);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to claim capture milestone for " + uuid, e);
        }
        return false;
    }

    public static class PurchaseLimitStatus {
        public final boolean enabled;
        public final int remaining;
        public final long resetAtMillis;

        public PurchaseLimitStatus(boolean enabled, int remaining, long resetAtMillis) {
            this.enabled = enabled;
            this.remaining = remaining;
            this.resetAtMillis = resetAtMillis;
        }
    }

    public PurchaseLimitStatus getPurchaseLimitStatus(UUID uuid, String shopId, String itemId, Integer limit, Integer cooldownMinutes) {
        if (limit == null || limit <= 0) {
            return new PurchaseLimitStatus(false, -1, 0);
        }

        long now = System.currentTimeMillis();
        long windowMs = cooldownMinutes == null ? 0 : cooldownMinutes.longValue() * 60000L;
        long windowStart = now;
        int count = 0;

        String selectSql = "SELECT window_start, count FROM purchase_limits WHERE uuid = ? AND shop_id = ? AND item_id = ?";
        String insertSql = "INSERT INTO purchase_limits(uuid, shop_id, item_id, window_start, count) VALUES(?, ?, ?, ?, ?)";
        String updateSql = "UPDATE purchase_limits SET window_start = ?, count = ? WHERE uuid = ? AND shop_id = ? AND item_id = ?";

        try (Connection conn = connect();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, uuid.toString());
            selectStmt.setString(2, shopId);
            selectStmt.setString(3, itemId);
            ResultSet rs = selectStmt.executeQuery();

            boolean found = rs.next();
            if (found) {
                windowStart = rs.getLong("window_start");
                count = rs.getInt("count");
            }

            if (windowMs > 0 && now - windowStart >= windowMs) {
                windowStart = now;
                count = 0;
            }

            if (!found) {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, uuid.toString());
                    insertStmt.setString(2, shopId);
                    insertStmt.setString(3, itemId);
                    insertStmt.setLong(4, windowStart);
                    insertStmt.setInt(5, count);
                    insertStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setLong(1, windowStart);
                    updateStmt.setInt(2, count);
                    updateStmt.setString(3, uuid.toString());
                    updateStmt.setString(4, shopId);
                    updateStmt.setString(5, itemId);
                    updateStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to read purchase limit for " + uuid, e);
        }

        long resetAt = windowMs > 0 ? windowStart + windowMs : 0;
        int remaining = Math.max(0, limit - count);
        return new PurchaseLimitStatus(true, remaining, resetAt);
    }

    public boolean consumePurchaseLimit(UUID uuid, String shopId, String itemId, int quantity, Integer limit, Integer cooldownMinutes) {
        if (limit == null || limit <= 0) {
            return true;
        }

        long now = System.currentTimeMillis();
        long windowMs = cooldownMinutes == null ? 0 : cooldownMinutes.longValue() * 60000L;
        long windowStart = now;
        int count = 0;

        String selectSql = "SELECT window_start, count FROM purchase_limits WHERE uuid = ? AND shop_id = ? AND item_id = ?";
        String insertSql = "INSERT INTO purchase_limits(uuid, shop_id, item_id, window_start, count) VALUES(?, ?, ?, ?, ?)";
        String updateSql = "UPDATE purchase_limits SET window_start = ?, count = ? WHERE uuid = ? AND shop_id = ? AND item_id = ?";

        try (Connection conn = connect();
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            selectStmt.setString(1, uuid.toString());
            selectStmt.setString(2, shopId);
            selectStmt.setString(3, itemId);
            ResultSet rs = selectStmt.executeQuery();

            boolean found = rs.next();
            if (found) {
                windowStart = rs.getLong("window_start");
                count = rs.getInt("count");
            }

            if (windowMs > 0 && now - windowStart >= windowMs) {
                windowStart = now;
                count = 0;
            }

            if (count + quantity > limit) {
                return false;
            }

            int newCount = count + quantity;
            if (!found) {
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, uuid.toString());
                    insertStmt.setString(2, shopId);
                    insertStmt.setString(3, itemId);
                    insertStmt.setLong(4, windowStart);
                    insertStmt.setInt(5, newCount);
                    insertStmt.executeUpdate();
                }
            } else {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setLong(1, windowStart);
                    updateStmt.setInt(2, newCount);
                    updateStmt.setString(3, uuid.toString());
                    updateStmt.setString(4, shopId);
                    updateStmt.setString(5, itemId);
                    updateStmt.executeUpdate();
                }
            }

            return true;
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to update purchase limit for " + uuid, e);
        }

        return false;
    }

    private void ensurePlayerExists(UUID uuid) {
        String checkSql = "SELECT uuid FROM balances WHERE uuid = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                String insertSql = "INSERT INTO balances(uuid, balance, pco) VALUES(?, ?, ?)";
                try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                    insertPstmt.setString(1, uuid.toString());
                    insertPstmt.setString(2, CobblemonEconomy.getConfig().startingBalance.toString());
                    insertPstmt.setString(3, CobblemonEconomy.getConfig().startingPco.toString());
                    insertPstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Error ensuring player exists: " + uuid, e);
        }
    }

    public BigDecimal getBalance(UUID uuid) {
        return getCurrency(uuid, "balance", CobblemonEconomy.getConfig().startingBalance);
    }

    public BigDecimal getPco(UUID uuid) {
        return getCurrency(uuid, "pco", CobblemonEconomy.getConfig().startingPco);
    }

    private BigDecimal getCurrency(UUID uuid, String column, BigDecimal defaultValue) {
        String sql = "SELECT " + column + " FROM balances WHERE uuid = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new BigDecimal(rs.getString(column));
            }
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to get " + column + " for " + uuid, e);
        }
        return defaultValue;
    }

    public void setBalance(UUID uuid, BigDecimal amount) {
        BigDecimal oldBalance = getBalance(uuid);
        if (!EconomyEvents.BALANCE_UPDATE_PRE.invoker().handle(uuid, oldBalance, amount, false)) {
            return;
        }
        updateCurrency(uuid, "balance", amount);
        EconomyEvents.BALANCE_UPDATE_POST.invoker().handle(uuid, oldBalance, amount, false);
    }

    public void setPco(UUID uuid, BigDecimal amount) {
        BigDecimal oldBalance = getPco(uuid);
        if (!EconomyEvents.BALANCE_UPDATE_PRE.invoker().handle(uuid, oldBalance, amount, true)) {
            return;
        }
        updateCurrency(uuid, "pco", amount);
        EconomyEvents.BALANCE_UPDATE_POST.invoker().handle(uuid, oldBalance, amount, true);
    }

    private void updateCurrency(UUID uuid, String column, BigDecimal amount) {
        ensurePlayerExists(uuid);
        String sql = "UPDATE balances SET " + column + " = ? WHERE uuid = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // On retire le setScale(2) qui forçait les .00
            pstmt.setString(1, amount.stripTrailingZeros().toPlainString());
            pstmt.setString(2, uuid.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to update " + column + " for " + uuid, e);
        }
    }

    public void addBalance(UUID uuid, BigDecimal amount) {
        setBalance(uuid, getBalance(uuid).add(amount));
    }

    public void addPco(UUID uuid, BigDecimal amount) {
        setPco(uuid, getPco(uuid).add(amount));
    }

    public boolean subtractBalance(UUID uuid, BigDecimal amount) {
        BigDecimal current = getBalance(uuid);
        if (current.compareTo(amount) < 0) return false;
        setBalance(uuid, current.subtract(amount));
        return true;
    }

    public boolean subtractPco(UUID uuid, BigDecimal amount) {
        BigDecimal current = getPco(uuid);
        if (current.compareTo(amount) < 0) return false;
        setPco(uuid, current.subtract(amount));
        return true;
    }

    public List<Map.Entry<UUID, BigDecimal>> getTopBalance(int limit) {
        return getTopCurrency("balance", limit);
    }

    public List<Map.Entry<UUID, BigDecimal>> getTopPco(int limit) {
        return getTopCurrency("pco", limit);
    }

    private List<Map.Entry<UUID, BigDecimal>> getTopCurrency(String column, int limit) {
        List<Map.Entry<UUID, BigDecimal>> topList = new ArrayList<>();
        // Casting to REAL for sorting. Note: This might lose precision for extremely large numbers but is sufficient for ranking.
        String sql = "SELECT uuid, " + column + " FROM balances ORDER BY CAST(" + column + " AS REAL) DESC LIMIT ?";
        
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                BigDecimal amount = new BigDecimal(rs.getString(column));
                topList.add(new AbstractMap.SimpleEntry<>(uuid, amount));
            }
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to get top " + column, e);
        }
        return topList;
    }
}
