package com.elplatano0871.damagetracker;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class DamageLeaderboardExpansion extends PlaceholderExpansion {
    private final DamageTracker plugin;
    private final DatabaseManager databaseManager;

    public DamageLeaderboardExpansion(DamageTracker plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "dt";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ElPlatano0871";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("damagetop_")) {
            String bossName = params.substring(9); // Remove "damagetop_" prefix
            return databaseManager.getFormattedLeaderboard(bossName);
        }

        return null;
    }
}