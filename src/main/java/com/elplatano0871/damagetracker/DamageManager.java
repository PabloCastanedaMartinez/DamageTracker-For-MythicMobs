package com.elplatano0871.damagetracker;

import org.bukkit.entity.Player;
import java.util.*;
import java.util.stream.Collectors;

public class DamageManager {
    // Map to store damage dealt by players to bosses
    private final Map<UUID, Map<UUID, Double>> bossDamageMaps;
    // Map to store the maximum health of bosses
    private final Map<UUID, Double> bossMaxHealth;
    // Format strings for displaying damage and percentage
    private final String damageFormat;
    private final String percentageFormat;

    // Constructor to initialize the DamageManager with format strings
    public DamageManager(String damageFormat, String percentageFormat) {
        this.bossDamageMaps = new HashMap<>();
        this.bossMaxHealth = new HashMap<>();
        this.damageFormat = damageFormat;
        this.percentageFormat = percentageFormat;
    }

    // Method to add damage dealt by a player to a boss
    public void addDamage(UUID bossId, Player player, double damage) {
        UUID playerId = player.getUniqueId();
        bossDamageMaps.computeIfAbsent(bossId, k -> new HashMap<>())
                      .compute(playerId, (k, v) -> v == null ? damage : v + damage);
    }

    // Method to get the damage map for a specific boss
    public Map<UUID, Double> getBossDamageMap(UUID bossId) {
        return bossDamageMaps.getOrDefault(bossId, new HashMap<>());
    }

    // Method to get all damage data
    public Map<UUID, Map<UUID, Double>> getAllDamageData() {
        return new HashMap<>(bossDamageMaps);
    }

    // Method to calculate the total damage dealt by all players
    public Map<UUID, Double> calculateTotalDamage() {
        Map<UUID, Double> totalDamageMap = new HashMap<>();
        for (Map<UUID, Double> damageMap : bossDamageMaps.values()) {
            for (Map.Entry<UUID, Double> entry : damageMap.entrySet()) {
                totalDamageMap.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }
        return totalDamageMap;
    }

    // Method to get the top damage dealers from a damage map
    public List<Map.Entry<UUID, Double>> getTopDamage(Map<UUID, Double> damageMap, int limit) {
        return damageMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Method to get the damage dealt by a specific player to a specific boss
    public double getPlayerDamage(UUID bossId, UUID playerId) {
        Map<UUID, Double> bossMap = bossDamageMaps.get(bossId);
        return bossMap != null ? bossMap.getOrDefault(playerId, 0.0) : 0.0;
    }

    // Method to set the maximum health of a boss
    public void setBossMaxHealth(UUID bossId, double health) {
        bossMaxHealth.put(bossId, health);
    }

    // Method to get the maximum health of a boss
    public double getBossMaxHealth(UUID bossId) {
        return bossMaxHealth.getOrDefault(bossId, 0.0);
    }

    // Method to check if a boss has a maximum health value set
    public boolean hasBossMaxHealth(UUID bossId) {
        return bossMaxHealth.containsKey(bossId);
    }

    // Method to format damage based on the display type (percentage or absolute value)
    public String formatDamage(double damage, double maxHealth, String displayType) {
        if ("percentage".equalsIgnoreCase(displayType) && maxHealth > 0) {
            double percentage = (damage / maxHealth) * 100;
            return String.format(percentageFormat, percentage);
        } else {
            return String.format(damageFormat, damage);
        }
    }

    // Method to remove all data related to a specific boss
    public void removeBossData(UUID bossId) {
        bossDamageMaps.remove(bossId);
        bossMaxHealth.remove(bossId);
    }

    // Method to clear all damage and health data
    public void clearAllData() {
        bossDamageMaps.clear();
        bossMaxHealth.clear();
    }
}