package com.elplatano0871.damagetracker;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {
    private final DamageTracker plugin;
    private Connection connection;
    private FileConfiguration formatConfig;
    private File formatFile;
    private String nameFormat;
    private String damageFormat;
    private String symbolFormat;

    public DatabaseManager(DamageTracker plugin) {
        this.plugin = plugin;
        this.loadFormatConfig();
        this.initializeDatabase();
    }

    private void loadFormatConfig() {
        if (formatFile == null) {
            formatFile = new File(plugin.getDataFolder(), "leaderboard_format.yml");
        }

        if (!formatFile.exists()) {
            plugin.saveResource("leaderboard_format.yml", false);
        }

        formatConfig = YamlConfiguration.loadConfiguration(formatFile);
        loadFormats();
    }

    private void loadFormats() {
        nameFormat = formatConfig.getString("formats.name", "&e{name}");
        damageFormat = formatConfig.getString("formats.damage", "&c{damage}");
        symbolFormat = formatConfig.getString("formats.symbol", "⚔");
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" +
                    new File(plugin.getDataFolder(), "leaderboards.db").getAbsolutePath());

            createTables();
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("Could not initialize database: " + e.getMessage());
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS boss_damage (
                    boss_name TEXT,
                    player_uuid TEXT,
                    player_name TEXT,
                    damage DOUBLE,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (boss_name, player_uuid)
                )
            """);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create tables: " + e.getMessage());
        }
    }

    public void updateDamage(String bossName, UUID playerUuid, String playerName, double damage) {
        if (bossName == null || bossName.trim().isEmpty()) {
            plugin.getLogger().warning("Attempted to update damage with null or empty boss name");
            return;
        }
        String sql = """
            INSERT OR REPLACE INTO boss_damage (boss_name, player_uuid, player_name, damage, last_updated)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, bossName.toUpperCase());
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, playerName);
            pstmt.setDouble(4, damage);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not update damage: " + e.getMessage());
        }
    }

    public String getFormattedLeaderboard(String bossName) {
        String sql = """
            SELECT player_name, damage
            FROM boss_damage
            WHERE boss_name = ?
            ORDER BY damage DESC
            LIMIT 10
        """;

        StringBuilder result = new StringBuilder();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, bossName.toUpperCase());
            ResultSet rs = pstmt.executeQuery();

            int position = 1;
            while (rs.next()) {
                String playerName = rs.getString("player_name");
                double damage = rs.getDouble("damage");

                String entry = formatConfig.getString("format", "{symbol} {position}. {name}: {damage}")
                        .replace("{symbol}", symbolFormat)
                        .replace("{position}", String.valueOf(position))
                        .replace("{name}", nameFormat.replace("{name}", playerName))
                        .replace("{damage}", damageFormat.replace("{damage}", String.format("%.2f", damage)));

                result.append(entry).append("\n");
                position++;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not get leaderboard: " + e.getMessage());
            return "Error loading leaderboard";
        }

        return result.toString().trim();
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not close database connection: " + e.getMessage());
        }
    }

    public void reloadConfig() {
        loadFormatConfig();
    }
}