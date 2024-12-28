package com.elplatano0871.damagetracker;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.chat.Chat;
import net.luckperms.api.LuckPerms;
import java.util.*;

public class DamageTracker extends JavaPlugin {
    private Map<String, BossConfig> bossConfigs;
    private BossConfig defaultBossConfig;
    private DamageManager damageManager;
    private TrackedBossManager trackedBossManager;
    private VictoryMessageManager victoryMessageManager;
    private boolean useVault;
    private boolean useLuckPerms;
    private Chat vaultChat;
    private LuckPerms luckPerms;
    public String personalMessageFormat;
    public String damageFormat;
    public String percentageFormat;

    @Override
    public void onEnable() {
        // Initialize boss configurations
        bossConfigs = new HashMap<>();
        // Initialize the damage manager
        initializeDamageManager();
        // Initialize the tracked boss manager
        initializeTrackedBossManager();
        // Initialize the victory message manager
        victoryMessageManager = new VictoryMessageManager(this);
        // Load configuration from file
        loadConfig();
        // Initialize message utilities
        MessageUtils.init(this); 
        // Register event handlers and commands
        registerHandlers();
        // Setup integrations with other plugins
        setupIntegrations();
        // Display ASCII art in the console
        displayAsciiArt();
    }

    @Override
    public void onDisable() {
        // Close message utilities
        MessageUtils.close(); 
    }

    private void initializeDamageManager() {
        // Get damage and percentage formats from config
        String damageFormat = getConfig().getString("damage_format", "%.2f");
        String percentageFormat = getConfig().getString("percentage_format", "%.1f%%");
        // Initialize the damage manager with the formats
        this.damageManager = new DamageManager(damageFormat, percentageFormat);
    }

    private void initializeTrackedBossManager() {
        trackedBossManager = new TrackedBossManager(this);
    }

    private void registerHandlers() {
        // Register event listeners
        getServer().getPluginManager().registerEvents(new MythicMobListeners(this), this);
        // Register placeholder
        new DamageTrackerPlaceholder(this).register();
        
        // Register command handler
        DamageTrackerCommand commandHandler = new DamageTrackerCommand(this, trackedBossManager);
        getCommand("damagetracker").setExecutor(commandHandler);
        getCommand("damagetracker").setTabCompleter(commandHandler);
    }

    private void setupIntegrations() {
        // Setup Vault integration
        setupVault();
        // Setup LuckPerms integration
        setupLuckPerms();
    }

    private void setupVault() {
        // Check if Vault plugin is available
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            // Get Vault chat provider
            org.bukkit.plugin.RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp != null) {
                vaultChat = rsp.getProvider();
                useVault = true;
                getLogger().info("Vault integration enabled.");
            }
        }
    }

    private void setupLuckPerms() {
        // Get LuckPerms provider
        org.bukkit.plugin.RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
            useLuckPerms = true;
            getLogger().info("LuckPerms integration enabled.");
        }
    }

    public void loadConfig() {
        // Save default config if not present
        saveDefaultConfig();
        // Reload config from file
        reloadConfig();
        // Clear existing boss configurations
        bossConfigs.clear();
        // Load formats from config
        loadFormats();
        // Load boss configurations from config
        loadBossConfigs();
        // Load default boss configuration from config
        loadDefaultConfig();
        // Load personal message format from config
        loadPersonalMessageFormat();
        // Load tracked boss manager configuration
        trackedBossManager.loadConfig();
        // Load victory message manager configuration
        victoryMessageManager.reloadConfig();
    }

    private void loadFormats() {
        // Get damage and percentage formats from config
        damageFormat = getConfig().getString("damage_format", "%.2f");
        percentageFormat = getConfig().getString("percentage_format", "%.1f%%");
        // Get personal message format from config
        personalMessageFormat = getConfig().getString("personal_message_format",
            "&6Your contribution: &ePosition: {position}, Damage: {damage} ({percentage}%)");
    }

    private void loadBossConfigs() {
        // Clear existing boss configurations
        bossConfigs.clear();

        // Get boss configurations section from config
        var bossesSection = getConfig().getConfigurationSection("bosses");
        if (bossesSection != null) {
            // Iterate through each boss configuration
            for (String bossName : bossesSection.getKeys(false)) {
                var bossSection = bossesSection.getConfigurationSection(bossName);
                if (bossSection != null && !bossSection.getKeys(false).isEmpty()) {
                    // Create new boss configuration with message IDs
                    BossConfig config = new BossConfig(
                            bossSection.getString("victory_message_id", "DEFAULT_VICTORY"),
                            bossSection.getString("position_format_id", "DEFAULT"),
                            bossSection.getString("personal_message_id", "DEFAULT"),
                            bossSection.getString("non_participant_message_id", "DEFAULT"),
                            bossSection.getInt("top_players_to_show", 3),
                            bossSection.getBoolean("broadcast_message", true)
                    );

                    // Validate the configuration
                    config.validate();

                    // Add to boss configurations map
                    bossConfigs.put(bossName.toUpperCase(), config);
                } else {
                    // Add null configuration if section is empty
                    bossConfigs.put(bossName.toUpperCase(), null);
                }
            }
        }

        getLogger().info("Configuration loaded. Number of configured bosses: " + bossConfigs.size());

        // Log any bosses using default messages
        bossConfigs.forEach((bossName, config) -> {
            if (config != null && config.getVictoryMessageId().equals("DEFAULT_VICTORY")) {
                getLogger().info("Boss '" + bossName + "' is using default victory message");
            }
        });
    }

    private void loadDefaultConfig() {
        // Get default boss configuration section from config
        var defaultSection = getConfig().getConfigurationSection("default_boss_config");
        if (defaultSection != null) {
            // Initialize default boss configuration with message IDs
            defaultBossConfig = new BossConfig(
                    defaultSection.getString("victory_message_id", "DEFAULT_VICTORY"),
                    defaultSection.getString("position_format_id", "DEFAULT"),
                    defaultSection.getString("personal_message_id", "DEFAULT"),
                    defaultSection.getString("non_participant_message_id", "DEFAULT"),
                    defaultSection.getInt("top_players_to_show", 3),
                    defaultSection.getBoolean("broadcast_message", true)
            );
        } else {
            // Create a default configuration if none exists
            defaultBossConfig = new BossConfig();
        }

        // Validate the default configuration
        defaultBossConfig.validate();

        // Log default configuration details
        getLogger().info("Default configuration loaded:");
        getLogger().info("- Victory Message ID: " + defaultBossConfig.getVictoryMessageId());
        getLogger().info("- Position Format ID: " + defaultBossConfig.getPositionFormatId());
        getLogger().info("- Top Players to Show: " + defaultBossConfig.getTopPlayersToShow());
        getLogger().info("- Broadcast Messages: " + defaultBossConfig.isBroadcastMessage());
    }

    private void loadPersonalMessageFormat() {
        // Get personal message format from config
        personalMessageFormat = getConfig().getString("personal_message_format",
            "&6Your contribution: &ePosition: {position}, Damage: {damage} ({percentage}%)");
    }

    private void displayAsciiArt() {
        // Display ASCII art in the console
        String version = getDescription().getVersion();
        String enableMessage = String.format("v%s has been enabled! ~ElPlatano0871", version);

        String[] asciiArt = {
            " ____                                  _____               _             ",
            "|  _ \\  __ _ _ __ ___   __ _  __ _  __|_   _| __ __ _  ___| | _____ _ __",
            "| | | |/ _` | '_ ` _ \\ / _` |/ _` |/ _ \\| || '__/ _` |/ __| |/ / _ \\ '__|",
            "| |_| | (_| | | | | | | (_| | (_| |  __/| || | | (_| | (__|   <  __/ |  ",
            "|____/ \\__,_|_| |_| |_|\\__,_|\\__, |\\___|_||_|  \\__,_|\\___|_|\\_\\___|_|  ",
            "                             |___/                                      "
        };

        Arrays.stream(asciiArt).forEach(line -> {
            if (line.contains("| | |")) {
                Bukkit.getConsoleSender().sendMessage(line + "  §9" + enableMessage);
            } else {
                Bukkit.getConsoleSender().sendMessage(line);
            }
        });
    }

    // Getters

    public DamageManager getDamageManager() {
        return damageManager;
    }

    public TrackedBossManager getTrackedBossManager() {
        return trackedBossManager;
    }

    public VictoryMessageManager getVictoryMessageManager() {
        return victoryMessageManager;
    }

    /**
     * Gets the map of boss configurations.
     * @return A map where the key is the boss name and the value is the BossConfig object.
     */
    public Map<String, BossConfig> getBossConfigs() {
        return bossConfigs;
    }

    /**
     * Gets the default boss configuration.
     * @return The default BossConfig object.
     */
    public BossConfig getDefaultBossConfig() {
        return defaultBossConfig;
    }

    /**
     * Gets the default number of top players to show.
     * @return The number of top players to show, or 3 if the default configuration is null.
     */
    public int getDefaultTopPlayersToShow() {
        return defaultBossConfig != null ? defaultBossConfig.getTopPlayersToShow() : 3;
    }

    /**
     * Gets the player's prefix using Vault or LuckPerms.
     * @param player The player whose prefix is to be retrieved.
     * @return The player's prefix, or an empty string if neither Vault nor LuckPerms is used.
     */
    public String getPlayerPrefix(Player player) {
        if (useVault && vaultChat != null) {
            return vaultChat.getPlayerPrefix(player);
        } else if (useLuckPerms && luckPerms != null) {
            var user = luckPerms.getUserManager().getUser(player.getUniqueId());
            return user != null ? user.getCachedData().getMetaData().getPrefix() : "";
        }
        return "";
    }

    /**
     * Adds damage to a boss for a specific player.
     * @param bossId The UUID of the boss.
     * @param player The player dealing the damage.
     * @param damage The amount of damage dealt.
     */
    public void addDamage(UUID bossId, Player player, double damage) {
        damageManager.addDamage(bossId, player, damage);
    }

    /**
     * Gets the damage map for a specific boss.
     * @param bossId The UUID of the boss.
     * @return A map where the key is the player's UUID and the value is the damage dealt.
     */
    public Map<UUID, Double> getBossDamageMap(UUID bossId) {
        return damageManager.getBossDamageMap(bossId);
    }

    /**
     * Gets all damage data for all bosses.
     * @return A map where the key is the boss's UUID and the value is another map of player UUIDs to damage amounts.
     */
    public Map<UUID, Map<UUID, Double>> getAllDamageData() {
        return damageManager.getAllDamageData();
    }

    /**
     * Gets the maximum health of a specific boss.
     * @param bossId The UUID of the boss.
     * @return The maximum health of the boss.
     */
    public double getBossMaxHealth(UUID bossId) {
        return damageManager.getBossMaxHealth(bossId);
    }

    /**
     * Checks if a specific boss has a maximum health value set.
     * @param bossId The UUID of the boss.
     * @return True if the boss has a maximum health value set, false otherwise.
     */
    public boolean hasBossMaxHealth(UUID bossId) {
        return damageManager.hasBossMaxHealth(bossId);
    }

    /**
     * Sets the maximum health of a specific boss.
     * @param bossId The UUID of the boss.
     * @param health The maximum health to set for the boss.
     */
    public void setBossMaxHealth(UUID bossId, double health) {
        damageManager.setBossMaxHealth(bossId, health);
    }

    /**
     * Removes the damage data for a specific boss.
     * @param bossId The UUID of the boss.
     */
    public void removeBossData(UUID bossId) {
        damageManager.removeBossData(bossId);
    }

    /**
     * Formats the damage amount for display.
     * @param damage The damage amount.
     * @param maxHealth The maximum health of the boss.
     * @param displayType The type of display format.
     * @return A formatted string representing the damage.
     */
    public String formatDamage(double damage, double maxHealth, String displayType) {
        return damageManager.formatDamage(damage, maxHealth, displayType);
    }

    /**
     * Calculates the total damage dealt by all players.
     * @return A map where the key is the player's UUID and the value is the total damage dealt.
     */
    public Map<UUID, Double> calculateTotalDamage() {
        return damageManager.calculateTotalDamage();
    }

    /**
     * Gets the top damage entries from a damage map.
     * @param damageMap A map where the key is the player's UUID and the value is the damage dealt.
     * @param limit The maximum number of entries to return.
     * @return A list of entries sorted by damage in descending order.
     */
    public List<Map.Entry<UUID, Double>> getTopDamage(Map<UUID, Double> damageMap, int limit) {
        return damageManager.getTopDamage(damageMap, limit);
    }
}