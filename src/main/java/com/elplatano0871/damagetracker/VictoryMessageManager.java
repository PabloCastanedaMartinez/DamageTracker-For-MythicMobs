package com.elplatano0871.damagetracker;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages victory messages for the DamageTracker plugin.
 */
public class VictoryMessageManager {
    private final DamageTracker plugin;
    private FileConfiguration messageConfig;
    private File messageFile;
    private final Map<String, String> victoryMessages;
    private final Map<String, List<String>> positionFormats;
    private final Map<String, String> personalMessages;
    private final Map<String, String> nonParticipantMessages;

    /**
     * Constructor for VictoryMessageManager.
     *
     * @param plugin The main plugin instance.
     */
    public VictoryMessageManager(DamageTracker plugin) {
        this.plugin = plugin;
        this.victoryMessages = new HashMap<>();
        this.positionFormats = new HashMap<>();
        this.personalMessages = new HashMap<>();
        this.nonParticipantMessages = new HashMap<>();
        loadMessages();
    }

    /**
     * Loads the victory messages from the configuration file.
     */
    public void loadMessages() {
        if (messageFile == null) {
            messageFile = new File(plugin.getDataFolder(), "victory_messages.yml");
        }

        if (!messageFile.exists()) {
            plugin.saveResource("victory_messages.yml", false);
            plugin.getLogger().info("Created new victory_messages.yml file");
        }

        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
        loadVictoryMessages();
        loadPositionFormats();
        loadPersonalMessages();
        loadNonParticipantMessages();
    }

    /**
     * Loads the victory messages from the configuration section.
     */
    private void loadVictoryMessages() {
        victoryMessages.clear();
        var messagesSection = messageConfig.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                victoryMessages.put(key, messagesSection.getString(key));
            }
        }
    }

    /**
     * Loads the position formats from the configuration section.
     */
    private void loadPositionFormats() {
        positionFormats.clear();
        var formatsSection = messageConfig.getConfigurationSection("position_formats");
        if (formatsSection != null) {
            for (String key : formatsSection.getKeys(false)) {
                positionFormats.put(key, formatsSection.getStringList(key));
            }
        }
    }

    /**
     * Loads the personal messages from the configuration section.
     */
    private void loadPersonalMessages() {
        personalMessages.clear();
        var personalSection = messageConfig.getConfigurationSection("personal_messages");
        if (personalSection != null) {
            for (String key : personalSection.getKeys(false)) {
                personalMessages.put(key, personalSection.getString(key));
            }
        }
    }

    /**
     * Loads the non-participant messages from the configuration section.
     */
    private void loadNonParticipantMessages() {
        nonParticipantMessages.clear();
        var nonParticipantSection = messageConfig.getConfigurationSection("non_participant_messages");
        if (nonParticipantSection != null) {
            for (String key : nonParticipantSection.getKeys(false)) {
                nonParticipantMessages.put(key, nonParticipantSection.getString(key));
            }
        }
    }

    /**
     * Gets the victory message for a given message ID.
     *
     * @param messageId The ID of the message.
     * @return The victory message.
     */
    public String getVictoryMessage(String messageId) {
        return victoryMessages.getOrDefault(messageId, victoryMessages.get("DEFAULT_VICTORY"));
    }

    /**
     * Gets the position format for a given format ID.
     *
     * @param formatId The ID of the format.
     * @return The list of position formats.
     */
    public List<String> getPositionFormat(String formatId) {
        return positionFormats.getOrDefault(formatId, positionFormats.get("DEFAULT"));
    }

    /**
     * Gets the personal message for a given message ID.
     *
     * @param messageId The ID of the message.
     * @return The personal message.
     */
    public String getPersonalMessage(String messageId) {
        return personalMessages.getOrDefault(messageId, personalMessages.get("DEFAULT"));
    }

    /**
     * Gets the non-participant message for a given message ID.
     *
     * @param messageId The ID of the message.
     * @return The non-participant message.
     */
    public String getNonParticipantMessage(String messageId) {
        return nonParticipantMessages.getOrDefault(messageId, nonParticipantMessages.get("DEFAULT"));
    }

    /**
     * Saves the configuration to the file.
     */
    public void saveConfig() {
        try {
            messageConfig.save(messageFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save victory_messages.yml", e);
        }
    }

    /**
     * Reloads the configuration from the file.
     */
    public void reloadConfig() {
        loadMessages();
    }
}