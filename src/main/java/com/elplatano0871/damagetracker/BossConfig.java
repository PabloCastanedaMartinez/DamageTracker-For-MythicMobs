package com.elplatano0871.damagetracker;

import java.util.ArrayList;
import java.util.List;

public class BossConfig {
    public String victoryMessage;
    public int topPlayersToShow;
    public List<String> topPlayersFormat;

    /**
     * Constructor for BossConfig with all parameters
     *
     * @param victoryMessage    The message to display when a boss is defeated
     * @param topPlayersToShow  Number of top players to display in the ranking
     * @param topPlayersFormat  List of format strings for each ranking position
     */
    public BossConfig(String victoryMessage, int topPlayersToShow, List<String> topPlayersFormat) {
        this.victoryMessage = victoryMessage;
        this.topPlayersToShow = topPlayersToShow;
        this.topPlayersFormat = topPlayersFormat != null ? new ArrayList<>(topPlayersFormat) : new ArrayList<>();
    }

    /**
     * Default constructor with default values
     */
    public BossConfig() {
        this.victoryMessage = "&6The boss has been defeated!";
        this.topPlayersToShow = 3;
        this.topPlayersFormat = new ArrayList<>();
        this.topPlayersFormat.add("&aFirst place: {prefix}&7{player_name} &c({damage} | {percentage}%)");
        this.topPlayersFormat.add("&eSecond place: {prefix}&7{player_name} &c({damage} | {percentage}%)");
        this.topPlayersFormat.add("&6Third place: {prefix}&7{player_name} &c({damage} | {percentage}%)");
    }

    /**
     * Gets the victory message
     *
     * @return Victory message string
     */
    public String getVictoryMessage() {
        return victoryMessage;
    }

    /**
     * Sets the victory message
     *
     * @param victoryMessage New victory message
     */
    public void setVictoryMessage(String victoryMessage) {
        this.victoryMessage = victoryMessage;
    }

    /**
     * Gets the number of top players to show
     *
     * @return Number of top players
     */
    public int getTopPlayersToShow() {
        return topPlayersToShow;
    }

    /**
     * Sets the number of top players to show
     *
     * @param topPlayersToShow New number of top players
     */
    public void setTopPlayersToShow(int topPlayersToShow) {
        this.topPlayersToShow = Math.max(1, topPlayersToShow); // Ensure at least 1 player is shown
    }

    /**
     * Gets the format list for top players
     *
     * @return List of format strings
     */
    public List<String> getTopPlayersFormat() {
        return new ArrayList<>(topPlayersFormat);
    }

    /**
     * Sets the format list for top players
     *
     * @param topPlayersFormat New list of format strings
     */
    public void setTopPlayersFormat(List<String> topPlayersFormat) {
        this.topPlayersFormat = topPlayersFormat != null ? new ArrayList<>(topPlayersFormat) : new ArrayList<>();
    }

    /**
     * Gets a specific format string for a position
     *
     * @param position Position in the ranking (0-based)
     * @return Format string for the position, or default format if not found
     */
    public String getFormatForPosition(int position) {
        if (position >= 0 && position < topPlayersFormat.size()) {
            return topPlayersFormat.get(position);
        }
        return "&7#{position}: {prefix}{player_name} ({damage} | {percentage}%)";
    }

    /**
     * Creates a copy of this BossConfig
     *
     * @return A new BossConfig instance with the same values
     */
    public BossConfig copy() {
        return new BossConfig(
            this.victoryMessage,
            this.topPlayersToShow,
            new ArrayList<>(this.topPlayersFormat)
        );
    }

    /**
     * Validates and fixes the configuration
     * Ensures all required fields have at least default values
     */
    public void validate() {
        if (victoryMessage == null || victoryMessage.trim().isEmpty()) {
            victoryMessage = "&6The boss has been defeated!";
        }

        if (topPlayersToShow < 1) {
            topPlayersToShow = 3;
        }

        if (topPlayersFormat == null) {
            topPlayersFormat = new ArrayList<>();
        }

        // Ensure we have enough formats for the number of players to show
        while (topPlayersFormat.size() < topPlayersToShow) {
            int position = topPlayersFormat.size() + 1;
            topPlayersFormat.add(String.format("&7#%d: {prefix}{player_name} ({damage} | {percentage}%%)", position));
        }
    }

    @Override
    public String toString() {
        return "BossConfig{" +
                "victoryMessage='" + victoryMessage + '\'' +
                ", topPlayersToShow=" + topPlayersToShow +
                ", topPlayersFormat=" + topPlayersFormat +
                '}';
    }
}