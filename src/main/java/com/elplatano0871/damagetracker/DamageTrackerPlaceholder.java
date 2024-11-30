package com.elplatano0871.damagetracker;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.stream.Collectors;

public class DamageTrackerPlaceholder extends PlaceholderExpansion {
    private final DamageTracker plugin;

    // Constructor to initialize the plugin instance
    public DamageTrackerPlaceholder(DamageTracker plugin) {
        this.plugin = plugin;
    }

    // Returns the identifier for this placeholder expansion
    @Override
    public @NotNull String getIdentifier() {
        return "damagetracker_for_mythicmobs";
    }

    // Returns the author of this placeholder expansion
    @Override
    public @NotNull String getAuthor() {
        return "ElPlatano0871";
    }

    // Returns the version of this placeholder expansion
    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    // Handles the placeholder request
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        // Map to store total damage for each player
        Map<UUID, Double> totalDamageMap = new HashMap<>();
        // Retrieve all damage data from the plugin
        Map<UUID, Map<UUID, Double>> allDamageData = plugin.getAllDamageData();
        
        // Aggregate total damage for each player
        for (Map<UUID, Double> damageMap : allDamageData.values()) {
            for (Map.Entry<UUID, Double> entry : damageMap.entrySet()) {
                totalDamageMap.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        // Get the top three players by damage
        List<Map.Entry<UUID, Double>> topThree = totalDamageMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(3)
                .collect(Collectors.toList());

        // Split the identifier to determine the type of request
        String[] parts = identifier.split("_");
        if (parts.length != 2) return null;

        int position;
        try {
            // Parse the position from the identifier
            position = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        // Validate the position
        if (position < 1 || position > 3) return null;
        if (topThree.size() < position) return "N/A";

        // Get the player and damage for the specified position
        Map.Entry<UUID, Double> entry = topThree.get(position - 1);
        UUID playerId = entry.getKey();
        Double damage = entry.getValue();

        // Handle different types of requests
        return switch (parts[0]) {
            case "top" -> parts[1].endsWith("name") ?
                    // Return the name of the player in the specified position
                    Bukkit.getOfflinePlayer(playerId).getName() :
                    parts[1].endsWith("damage") ?
                            // Return the damage of the player in the specified position
                            String.format("%.2f", damage) :
                            null;
            case "player" -> {
                if (player == null) yield null;
                yield switch (parts[1]) {
                    case "damage" -> 
                        // Return the total damage for the specified player
                        String.format("%.2f", totalDamageMap.getOrDefault(player.getUniqueId(), 0.0));
                    case "position" -> {
                        // Determine the position of the specified player
                        int playerPosition = topThree.indexOf(topThree.stream()
                                .filter(e -> e.getKey().equals(player.getUniqueId()))
                                .findFirst()
                                .orElse(null)) + 1;
                        yield playerPosition > 0 ? String.valueOf(playerPosition) : "N/A";
                    }
                    default -> null;
                };
            }
            default -> null;
        };
    }
}