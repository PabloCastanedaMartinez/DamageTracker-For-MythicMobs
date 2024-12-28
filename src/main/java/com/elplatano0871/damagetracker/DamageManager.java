package com.elplatano0871.damagetracker;

import org.bukkit.entity.Player;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages damage tracking for bosses and players.
 */
public class DamageManager {
    private final Map<UUID, Map<UUID, Double>> bossDamageMaps;
    private final Map<UUID, Double> bossMaxHealth;
    private final Map<String, Map<UUID, Double>> trackedBossDamage;
    private final Map<String, Double> trackedBossMaxHealth;
    private final String damageFormat;
    private final String percentageFormat;

    /**
     * Constructor for DamageManager.
     *
     * @param damageFormat The format string for displaying damage.
     * @param percentageFormat The format string for displaying percentages.
     */
    public DamageManager(String damageFormat, String percentageFormat) {
        this.bossDamageMaps = new HashMap<>();
        this.bossMaxHealth = new HashMap<>();
        this.trackedBossDamage = new HashMap<>();
        this.trackedBossMaxHealth = new HashMap<>();
        this.damageFormat = damageFormat;
        this.percentageFormat = percentageFormat;
    }

    /**
     * Adds damage dealt by a player to a boss.
     *
     * @param bossId The ID of the boss.
     * @param player The player dealing the damage.
     * @param damage The amount of damage dealt.
     */
    public void addDamage(UUID bossId, Player player, double damage) {
        UUID playerId = player.getUniqueId();
        bossDamageMaps.computeIfAbsent(bossId, k -> new HashMap<>())
                .compute(playerId, (k, v) -> v == null ? damage : v + damage);
    }

    /**
     * Gets the damage map for a specific boss.
     *
     * @param bossId The ID of the boss.
     * @return A map of player UUIDs to damage amounts.
     */
    public Map<UUID, Double> getBossDamageMap(UUID bossId) {
        return bossDamageMaps.getOrDefault(bossId, new HashMap<>());
    }

    /**
     * Gets all damage data for all bosses.
     *
     * @return A map of boss UUIDs to player damage maps.
     */
    public Map<UUID, Map<UUID, Double>> getAllDamageData() {
        return new HashMap<>(bossDamageMaps);
    }

    /**
     * Calculates the total damage dealt by all players.
     *
     * @return A map of player UUIDs to total damage amounts.
     */
    public Map<UUID, Double> calculateTotalDamage() {
        Map<UUID, Double> totalDamageMap = new HashMap<>();
        for (Map<UUID, Double> damageMap : bossDamageMaps.values()) {
            for (Map.Entry<UUID, Double> entry : damageMap.entrySet()) {
                totalDamageMap.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }
        return totalDamageMap;
    }

    /**
     * Gets the top damage entries.
     *
     * @param damageMap The map of player UUIDs to damage amounts.
     * @param limit The maximum number of entries to return.
     * @return A list of entries sorted by damage in descending order.
     */
    public List<Map.Entry<UUID, Double>> getTopDamage(Map<UUID, Double> damageMap, int limit) {
        return damageMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets the damage dealt by a player to a specific boss.
     *
     * @param bossId The ID of the boss.
     * @param playerId The UUID of the player.
     * @return The damage dealt by the player.
     */
    public double getPlayerDamage(UUID bossId, UUID playerId) {
        Map<UUID, Double> bossMap = bossDamageMaps.get(bossId);
        return bossMap != null ? bossMap.getOrDefault(playerId, 0.0) : 0.0;
    }

    /**
     * Sets the maximum health of a boss.
     *
     * @param bossId The ID of the boss.
     * @param health The maximum health to set for the boss.
     */
    public void setBossMaxHealth(UUID bossId, double health) {
        bossMaxHealth.put(bossId, health);
    }

    /**
     * Gets the maximum health of a boss.
     *
     * @param bossId The ID of the boss.
     * @return The maximum health of the boss.
     */
    public double getBossMaxHealth(UUID bossId) {
        return bossMaxHealth.getOrDefault(bossId, 0.0);
    }

    /**
     * Checks if a boss has a maximum health value set.
     *
     * @param bossId The ID of the boss.
     * @return true if the boss has a maximum health value set, false otherwise.
     */
    public boolean hasBossMaxHealth(UUID bossId) {
        return bossMaxHealth.containsKey(bossId);
    }

    /**
     * Formats the damage amount for display.
     *
     * @param damage The damage amount.
     * @param maxHealth The maximum health of the boss.
     * @param displayType The type of display (e.g., "percentage").
     * @return A formatted string representing the damage.
     */
    public String formatDamage(double damage, double maxHealth, String displayType) {
        if ("percentage".equalsIgnoreCase(displayType) && maxHealth > 0) {
            double percentage = (damage / maxHealth) * 100;
            return String.format(percentageFormat, percentage);
        } else {
            return String.format(damageFormat, damage);
        }
    }

    /**
     * Removes all data related to a specific boss.
     *
     * @param bossId The ID of the boss.
     */
    public void removeBossData(UUID bossId) {
        bossDamageMaps.remove(bossId);
        bossMaxHealth.remove(bossId);
    }

    /**
     * Clears all damage and health data.
     */
    public void clearAllData() {
        bossDamageMaps.clear();
        bossMaxHealth.clear();
    }

    /**
     * Adds damage dealt by a player to a tracked boss.
     *
     * @param bossId The ID of the boss.
     * @param player The player dealing the damage.
     * @param damage The amount of damage dealt.
     */
    public void addTrackedDamage(String bossId, Player player, double damage) {
        UUID playerId = player.getUniqueId();
        trackedBossDamage.computeIfAbsent(bossId, k -> new HashMap<>())
                .compute(playerId, (k, v) -> v == null ? damage : v + damage);
    }

    /**
     * Gets the damage map for a tracked boss.
     *
     * @param bossId The ID of the boss.
     * @return A map of player UUIDs to damage amounts.
     */
    public Map<UUID, Double> getTrackedBossDamageMap(String bossId) {
        return new HashMap<>(trackedBossDamage.getOrDefault(bossId, new HashMap<>()));
    }

    /**
     * Sets the maximum health of a tracked boss.
     *
     * @param bossId The ID of the boss.
     * @param health The maximum health to set for the boss.
     */
    public void setTrackedBossMaxHealth(String bossId, double health) {
        trackedBossMaxHealth.put(bossId, health);
    }

    /**
     * Gets the maximum health of a tracked boss.
     *
     * @param bossId The ID of the boss.
     * @return The maximum health of the boss.
     */
    public double getTrackedBossMaxHealth(String bossId) {
        return trackedBossMaxHealth.getOrDefault(bossId, 0.0);
    }

    /**
     * Removes all data related to a tracked boss.
     *
     * @param bossId The ID of the boss.
     */
    public void removeTrackedBossData(String bossId) {
        trackedBossDamage.remove(bossId);
        trackedBossMaxHealth.remove(bossId);
    }

    /**
     * Gets the top damage entries for a tracked boss.
     *
     * @param bossId The ID of the boss.
     * @param limit The maximum number of entries to return.
     * @return A list of entries sorted by damage in descending order.
     */
    public List<Map.Entry<UUID, Double>> getTrackedTopDamage(String bossId, int limit) {
        Map<UUID, Double> bossData = getTrackedBossDamageMap(bossId);
        if (bossData.isEmpty()) return new ArrayList<>();

        return bossData.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .toList();
    }

    /**
     * Gets the damage dealt by a player to a tracked boss.
     *
     * @param bossId The ID of the boss.
     * @param playerId The UUID of the player.
     * @return The damage dealt by the player.
     */
    public double getTrackedPlayerDamage(String bossId, UUID playerId) {
        return trackedBossDamage.getOrDefault(bossId, new HashMap<>())
                .getOrDefault(playerId, 0.0);
    }

    /**
     * Gets the percentage of total damage dealt by a player to a tracked boss.
     *
     * @param bossId The ID of the boss.
     * @param playerId The UUID of the player.
     * @return The percentage of total damage dealt by the player.
     */
    public double getTrackedPlayerDamagePercentage(String bossId, UUID playerId) {
        Map<UUID, Double> bossData = getTrackedBossDamageMap(bossId);
        if (bossData.isEmpty()) return 0.0;

        double playerDamage = bossData.getOrDefault(playerId, 0.0);
        double totalDamage = bossData.values().stream().mapToDouble(Double::doubleValue).sum();

        return totalDamage > 0 ? (playerDamage / totalDamage) * 100 : 0.0;
    }

    /**
     * Gets the position of a player in the damage ranking for a tracked boss.
     *
     * @param bossId The ID of the boss.
     * @param playerId The UUID of the player.
     * @return An optional containing the position of the player, or empty if the player is not in the ranking.
     */
    public Optional<Integer> getTrackedPlayerPosition(String bossId, UUID playerId) {
        Map<UUID, Double> bossData = getTrackedBossDamageMap(bossId);
        if (bossData.isEmpty() || !bossData.containsKey(playerId)) return Optional.empty();

        List<UUID> sortedPlayers = bossData.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .map(Map.Entry::getKey)
                .toList();

        int position = sortedPlayers.indexOf(playerId) + 1;
        return Optional.of(position);
    }

    /**
     * Formats the damage amount for display.
     *
     * @param damage The damage amount.
     * @param bossId The ID of the boss.
     * @return A formatted string representing the damage.
     */
    public String formatTrackedDamage(double damage, String bossId) {
        double maxHealth = trackedBossMaxHealth.getOrDefault(bossId, 0.0);
        if (maxHealth > 0) {
            double percentage = (damage / maxHealth) * 100;
            return String.format(damageFormat + " (%" + percentageFormat + ")", damage, percentage);
        }
        return String.format(damageFormat, damage);
    }

    /**
     * Clears all tracked damage and health data.
     */
    public void clearAllTrackedData() {
        trackedBossDamage.clear();
        trackedBossMaxHealth.clear();
    }
}