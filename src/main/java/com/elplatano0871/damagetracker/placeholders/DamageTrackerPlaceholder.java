package com.elplatano0871.damagetracker.placeholders;

import com.elplatano0871.damagetracker.DamageTracker;
import com.elplatano0871.damagetracker.managers.DatabaseManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Placeholder expansion for the DamageTracker plugin.
 */
public class DamageTrackerPlaceholder extends PlaceholderExpansion {
    private final DamageTracker plugin;
    private final DatabaseManager databaseManager;

    /**
     * Constructor for DamageTrackerPlaceholder.
     *
     * @param plugin The main plugin instance.
     */
    public DamageTrackerPlaceholder(DamageTracker plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Gets the identifier for this placeholder expansion.
     *
     * @return The identifier string.
     */
    @Override
    public @NotNull String getIdentifier() {
        return "dt";
    }

    /**
     * Gets the author of this placeholder expansion.
     *
     * @return The author string.
     */
    @Override
    public @NotNull String getAuthor() {
        return "ElPlatano0871";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return plugin != null && plugin.isEnabled();
    }

    /**
     * Handles placeholder requests.
     *
     * @param player The player for whom the placeholder is being requested.
     * @param identifier The identifier of the placeholder.
     * @return The value of the placeholder.
     */
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        // Handle leaderboard placeholders (from DamageLeaderboardExpansion)
        if (identifier.startsWith("damagetop_")) {
            String bossName = identifier.substring(10); 
            return databaseManager.getFormattedLeaderboard(bossName);
        }

        // Handle general damage placeholders (original functionality)
        Map<UUID, Double> totalDamageMap = new HashMap<>();
        Map<UUID, Map<UUID, Double>> allDamageData = plugin.getAllDamageData();

        for (Map<UUID, Double> damageMap : allDamageData.values()) {
            for (Map.Entry<UUID, Double> entry : damageMap.entrySet()) {
                totalDamageMap.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }

        List<Map.Entry<UUID, Double>> topThree = totalDamageMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(3)
                .collect(Collectors.toList());

        String[] parts = identifier.split("_");
        if (parts.length != 2) return null;

        int position;
        try {
            position = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        if (position < 1 || position > 3) return null;
        if (topThree.size() < position) return "N/A";

        Map.Entry<UUID, Double> entry = topThree.get(position - 1);
        UUID playerId = entry.getKey();
        Double damage = entry.getValue();

        return switch (parts[0]) {
            case "top" -> parts[1].endsWith("name") ?
                    Bukkit.getOfflinePlayer(playerId).getName() :
                    parts[1].endsWith("damage") ?
                            String.format("%.2f", damage) :
                            null;
            case "player" -> {
                if (player == null) yield null;
                yield switch (parts[1]) {
                    case "damage" ->
                            String.format("%.2f", totalDamageMap.getOrDefault(player.getUniqueId(), 0.0));
                    case "position" -> {
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