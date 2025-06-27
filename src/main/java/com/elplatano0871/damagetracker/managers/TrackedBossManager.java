package com.elplatano0871.damagetracker.managers;

import com.elplatano0871.damagetracker.DamageTracker;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages the tracking of bosses and their damage data.
 */
public class TrackedBossManager {
    private final DamageTracker plugin;
    private final Set<String> trackedBossIds;
    private FileConfiguration config;
    private File configFile;
    private boolean persistData;
    private int dataRetentionTime;

    /**
     * Constructor for TrackedBossManager.
     * @param plugin The instance of the DamageTracker plugin.
     */
    public TrackedBossManager(DamageTracker plugin) {
        this.plugin = plugin;
        this.trackedBossIds = ConcurrentHashMap.newKeySet();
        loadConfig();
    }

    /**
     * Loads the configuration from the tracked_bosses.yml file.
     */
    public void loadConfig() {
        try {
            if (configFile == null) {
                configFile = new File(plugin.getDataFolder(), "tracked_bosses.yml");
            }

            if (!configFile.exists()) {
                plugin.saveResource("tracked_bosses.yml", false);
                plugin.getLogger().info("Created new tracked_bosses.yml file");
            }

            config = YamlConfiguration.loadConfiguration(configFile);


            trackedBossIds.clear();
            trackedBossIds.addAll(config.getStringList("tracked_bosses.enabled_bosses").stream().map(String::toUpperCase).collect(Collectors.toList()));

            persistData = config.getBoolean("tracking_config.persist_data", false);
            dataRetentionTime = config.getInt("tracking_config.data_retention_time", 300);

            if (persistData && dataRetentionTime <= 0 && dataRetentionTime != -1) {
                plugin.getLogger().warning("Invalid data_retention_time value. Setting to default (300 seconds)");
                dataRetentionTime = 300;
            }

            plugin.getLogger().info("Loaded " + trackedBossIds.size() + " tracked bosses");
            plugin.getLogger().info("Data persistence: " + persistData);
            plugin.getLogger().info("Data retention time: " + (dataRetentionTime == -1 ? "Until restart" : dataRetentionTime + " seconds"));

            if (!trackedBossIds.isEmpty()) {
                plugin.getLogger().info("Tracked bosses: " + String.join(", ", trackedBossIds));
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error loading tracked_bosses.yml: " + e.getMessage());
            e.printStackTrace();

            // Set safe default values
            persistData = false;
            dataRetentionTime = 300;
            trackedBossIds.clear();
        }
    }

    /**
     * Saves the current configuration to the tracked_bosses.yml file.
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save tracked_bosses.yml", e);
        }
    }

    /**
     * Checks if a boss is being tracked.
     * @param bossId The ID of the boss.
     * @return True if the boss is being tracked, false otherwise.
     */
    public boolean isTrackedBoss(String bossId) {
        return trackedBossIds.contains(bossId.toUpperCase());
    }

    /**
     * Adds damage to a boss for a specific player.
     * @param bossId The ID of the boss.
     * @param player The player dealing the damage.
     * @param damage The amount of damage dealt.
     */
    public void addDamage(String bossId, Player player, double damage) {
        if (!isTrackedBoss(bossId)) return;
        plugin.getDamageManager().addTrackedDamage(bossId, player, damage);
    }

    /**
     * Sets the maximum health of a boss.
     * @param bossId The ID of the boss.
     * @param health The maximum health of the boss.
     */
    public void setBossMaxHealth(String bossId, double health) {
        if (!isTrackedBoss(bossId)) return;
        plugin.getDamageManager().setTrackedBossMaxHealth(bossId, health);
    }

    /**
     * Clears the damage data for a specific boss.
     * @param bossId The ID of the boss.
     */
    public void clearBossData(String bossId) {
        if (!isTrackedBoss(bossId)) return;
        plugin.getDamageManager().removeTrackedBossData(bossId);
    }

    /**
     * Schedules the cleanup of damage data for a specific boss.
     * @param bossId The ID of the boss.
     */
    public void scheduleDataCleanup(String bossId) {
        if (!persistData || dataRetentionTime <= 0) {
            clearBossData(bossId);
            return;
        }

        if (dataRetentionTime > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    clearBossData(bossId);
                }
            }.runTaskLater(plugin, dataRetentionTime * 20L);
        }
    }

    /**
     * Gets the top damage entries for a specific boss.
     * @param bossId The ID of the boss.
     * @param limit The maximum number of entries to return.
     * @return A list of entries sorted by damage in descending order.
     */
    public List<Map.Entry<UUID, Double>> getTopDamage(String bossId, int limit) {
        if (!isTrackedBoss(bossId)) return new ArrayList<>();
        return plugin.getDamageManager().getTrackedTopDamage(bossId, limit);
    }

    /**
     * Gets the position of a player in the damage ranking for a specific boss.
     * @param bossId The ID of the boss.
     * @param playerId The UUID of the player.
     * @return An optional containing the position of the player, or empty if the player is not in the ranking.
     */
    public Optional<Integer> getPlayerPosition(String bossId, UUID playerId) {
        if (!isTrackedBoss(bossId)) return Optional.empty();
        return plugin.getDamageManager().getTrackedPlayerPosition(bossId, playerId);
    }

    /**
     * Gets the damage dealt by a player to a specific boss.
     * @param bossId The ID of the boss.
     * @param playerId The UUID of the player.
     * @return The damage dealt by the player.
     */
    public double getPlayerDamage(String bossId, UUID playerId) {
        if (!isTrackedBoss(bossId)) return 0.0;
        return plugin.getDamageManager().getTrackedPlayerDamage(bossId, playerId);
    }

    /**
     * Gets the percentage of total damage dealt by a player to a specific boss.
     * @param bossId The ID of the boss.
     * @param playerId The UUID of the player.
     * @return The percentage of total damage dealt by the player.
     */
    public double getPlayerDamagePercentage(String bossId, UUID playerId) {
        if (!isTrackedBoss(bossId)) return 0.0;
        return plugin.getDamageManager().getTrackedPlayerDamagePercentage(bossId, playerId);
    }

    /**
     * Gets the set of IDs of bosses that are being tracked.
     * @return Immutable set with the IDs of the tracked bosses.
     */
    public Set<String> getTrackedBossIds() {
        return Collections.unmodifiableSet(trackedBossIds);
    }

    /**
     * Formats the damage for a tracked boss.
     * @param damage The damage amount.
     * @param bossId The ID of the boss.
     * @return A formatted string representing the damage.
     */
    public String formatDamage(double damage, String bossId) {
        return plugin.getDamageManager().formatTrackedDamage(damage, bossId);
    }

    /**
     * Gets the damage data for a tracked boss.
     * @param bossId The ID of the boss.
     * @return A map of player UUIDs to damage amounts.
     */
    public Map<UUID, Double> getDamageData(String bossId) {
        if (!isTrackedBoss(bossId)) return new HashMap<>();
        return plugin.getDamageManager().getTrackedBossDamageMap(bossId);
    }
}