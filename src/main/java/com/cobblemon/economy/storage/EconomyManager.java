package com.cobblemon.economy.storage;

import com.cobblemon.economy.fabric.CobblemonEconomy;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
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
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            CobblemonEconomy.LOGGER.error("Failed to initialize SQLite database", e);
        }
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
        updateCurrency(uuid, "balance", amount);
    }

    public void setPco(UUID uuid, BigDecimal amount) {
        updateCurrency(uuid, "pco", amount);
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
}

