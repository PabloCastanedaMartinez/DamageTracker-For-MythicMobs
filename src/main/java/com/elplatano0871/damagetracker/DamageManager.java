package com.elplatano0871.damagetracker;

import org.bukkit.entity.Player;
import java.util.*;
import java.util.stream.Collectors;

public class DamageManager {
    private final Map<UUID, Map<UUID, Double>> bossDamageMaps;
    private final Map<UUID, Double> bossMaxHealth;
    private final String damageFormat;
    private final String percentageFormat;

    public DamageManager(String damageFormat, String percentageFormat) {
        this.bossDamageMaps = new HashMap<>();
        this.bossMaxHealth = new HashMap<>();
        this.damageFormat = damageFormat;
        this.percentageFormat = percentageFormat;
    }

    public void addDamage(UUID bossId, Player player, double damage) {
        UUID playerId = player.getUniqueId();
        bossDamageMaps.computeIfAbsent(bossId, k -> new HashMap<>())
                      .compute(playerId, (k, v) -> v == null ? damage : v + damage);
    }

    public Map<UUID, Double> getBossDamageMap(UUID bossId) {
        return bossDamageMaps.getOrDefault(bossId, new HashMap<>());
    }

    public Map<UUID, Map<UUID, Double>> getAllDamageData() {
        return new HashMap<>(bossDamageMaps);
    }

    public Map<UUID, Double> calculateTotalDamage() {
        Map<UUID, Double> totalDamageMap = new HashMap<>();
        for (Map<UUID, Double> damageMap : bossDamageMaps.values()) {
            for (Map.Entry<UUID, Double> entry : damageMap.entrySet()) {
                totalDamageMap.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }
        return totalDamageMap;
    }

    public List<Map.Entry<UUID, Double>> getTopDamage(Map<UUID, Double> damageMap, int limit) {
        return damageMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public double getPlayerDamage(UUID bossId, UUID playerId) {
        Map<UUID, Double> bossMap = bossDamageMaps.get(bossId);
        return bossMap != null ? bossMap.getOrDefault(playerId, 0.0) : 0.0;
    }

    public void setBossMaxHealth(UUID bossId, double health) {
        bossMaxHealth.put(bossId, health);
    }

    public double getBossMaxHealth(UUID bossId) {
        return bossMaxHealth.getOrDefault(bossId, 0.0);
    }

    public boolean hasBossMaxHealth(UUID bossId) {
        return bossMaxHealth.containsKey(bossId);
    }

    public String formatDamage(double damage, double maxHealth, String displayType) {
        if ("percentage".equalsIgnoreCase(displayType) && maxHealth > 0) {
            double percentage = (damage / maxHealth) * 100;
            return String.format(percentageFormat, percentage);
        } else {
            return String.format(damageFormat, damage);
        }
    }

    public void removeBossData(UUID bossId) {
        bossDamageMaps.remove(bossId);
        bossMaxHealth.remove(bossId);
    }

    public void clearAllData() {
        bossDamageMaps.clear();
        bossMaxHealth.clear();
    }
}