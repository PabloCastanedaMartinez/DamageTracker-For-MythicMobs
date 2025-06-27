package com.elplatano0871.damagetracker;

import com.elplatano0871.damagetracker.commands.DamageTrackerCommand;
import com.elplatano0871.damagetracker.configs.BossConfig;
import com.elplatano0871.damagetracker.listeners.MythicMobListeners;
import com.elplatano0871.damagetracker.managers.DamageManager;
import com.elplatano0871.damagetracker.managers.DatabaseManager;
import com.elplatano0871.damagetracker.managers.TrackedBossManager;
import com.elplatano0871.damagetracker.managers.VictoryMessageManager;
import com.elplatano0871.damagetracker.placeholders.DamageTrackerPlaceholder;
import com.elplatano0871.damagetracker.utils.MessageUtils;
import com.elplatano0871.damagetracker.managers.HologramManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.chat.Chat;
import java.util.*;

public class DamageTracker extends JavaPlugin {
    private Map<String, BossConfig> bossConfigs;
    private BossConfig defaultBossConfig;
    private DamageManager damageManager;
    private TrackedBossManager trackedBossManager;
    private VictoryMessageManager victoryMessageManager;
    private DatabaseManager databaseManager;
    private boolean useVault;
    private Chat vaultChat;
    public String personalMessageFormat;
    public String damageFormat;
    public String percentageFormat;
    private HologramManager hologramManager;
    
    @Override
    public void onEnable() {
        // Initialize database manager
        databaseManager = new DatabaseManager(this);
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
        
        // Initialize hologram manager
        hologramManager = new HologramManager(this);
    }

    @Override
    public void onDisable() {
        // Close message utilities
        MessageUtils.close();
        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        // Remove all holograms
        if (hologramManager != null) {
            hologramManager.removeAllHolograms();
        }
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
        // Register placeholder (unified expansion)
        new DamageTrackerPlaceholder(this, databaseManager).register();
    
        // Register command handler
        DamageTrackerCommand commandHandler = new DamageTrackerCommand(this, trackedBossManager);
        getCommand("damagetracker").setExecutor(commandHandler);
        getCommand("damagetracker").setTabCompleter(commandHandler);
    }

    private void setupIntegrations() {
        // Setup Vault integration only
        setupVault();
    }

    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().info("Vault plugin not found. Prefix features will be disabled.");
            useVault = false;
            vaultChat = null;
            return;
        }

        try {
            org.bukkit.plugin.RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp != null) {
                vaultChat = rsp.getProvider();
                useVault = true;
                getLogger().info("Vault integration enabled successfully.");
            } else {
                getLogger().warning("Vault found but no chat provider available. Prefix features will be disabled.");
                useVault = false;
                vaultChat = null;
            }
        } catch (Exception e) {
            getLogger().warning("Error setting up Vault integration. Prefix features will be disabled.");
            useVault = false;
            vaultChat = null;
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
                            bossSection.getBoolean("broadcast_message", true),
                            bossSection.getString("hologram_type", "NONE") // Nueva línea
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
                    defaultSection.getBoolean("broadcast_message", true),
                    defaultSection.getString("hologram_type", "NONE") // Nueva línea
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
                " ____    ____    _    ",
                "|  _ \\  __ _ _ __ ___   __ _  __ _  __|_   _| __ __ _  ___| | ____ _ __",
                "| | | |/ _` | '_ ` _ \\ / _` |/ _` |/ _ \\| || '__/ _` |/ __| |/ / _ \\ '__|",
                "| |_| | (_| | | | | | | (_| | (_| |  __/| || | | (_| | (__|   <  __/ |  ",
                "|____/ \\__,_|_| |_| |_|\\__,_|\\__, |\\___|_||_|  \\__,_|\\___|_|\\_\\___|_|  ",
                "    |___/    "
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

    public Map<String, BossConfig> getBossConfigs() {
        return bossConfigs;
    }

    public BossConfig getDefaultBossConfig() {
        return defaultBossConfig;
    }

    public int getDefaultTopPlayersToShow() {
        return defaultBossConfig != null ? defaultBossConfig.getTopPlayersToShow() : 3;
    }
 
    public int getTopPlayersToShow(String bossName) {
        if (bossName == null) {
            return getDefaultTopPlayersToShow();
        }
        
        BossConfig config = bossConfigs.get(bossName.toUpperCase());
        if (config != null) {
            return config.getTopPlayersToShow();
        } else {
            return getDefaultTopPlayersToShow();
        }
    }


    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public String getPlayerPrefix(Player player) {
        String prefix = "";
        try {
            if (useVault && vaultChat != null) {
                prefix = vaultChat.getPlayerPrefix(player);
            }
        } catch (Exception e) {
            getLogger().warning("Error getting player prefix: " + e.getMessage());
        }
        return prefix != null ? prefix : "";
    }

    public void addDamage(UUID bossId, Player player, double damage) {
        damageManager.addDamage(bossId, player, damage);
    }

    public Map<UUID, Double> getBossDamageMap(UUID bossId) {
        return damageManager.getBossDamageMap(bossId);
    }

    public Map<UUID, Map<UUID, Double>> getAllDamageData() {
        return damageManager.getAllDamageData();
    }

    public double getBossMaxHealth(UUID bossId) {
        return damageManager.getBossMaxHealth(bossId);
    }

    public boolean hasBossMaxHealth(UUID bossId) {
        return damageManager.hasBossMaxHealth(bossId);
    }

    public void setBossMaxHealth(UUID bossId, double health) {
        damageManager.setBossMaxHealth(bossId, health);
    }

    public void removeBossData(UUID bossId) {
        damageManager.removeBossData(bossId);
    }

    public String formatDamage(double damage, double maxHealth, String displayType) {
        return damageManager.formatDamage(damage, maxHealth, displayType);
    }

    public Map<UUID, Double> calculateTotalDamage() {
        return damageManager.calculateTotalDamage();
    }

    public List<Map.Entry<UUID, Double>> getTopDamage(Map<UUID, Double> damageMap, int limit) {
        return damageManager.getTopDamage(damageMap, limit);
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }
}