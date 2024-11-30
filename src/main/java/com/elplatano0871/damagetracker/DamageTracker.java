package com.elplatano0871.damagetracker;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.milkbowl.vault.chat.Chat;
import net.luckperms.api.LuckPerms;
import java.util.*;

public class DamageTracker extends JavaPlugin {
    private BukkitAudiences adventure;
    private Map<String, BossConfig> bossConfigs;
    private BossConfig defaultBossConfig;
    private DamageManager damageManager;
    private boolean useVault;
    private boolean useLuckPerms;
    private Chat vaultChat;
    private LuckPerms luckPerms;
    private String personalMessageFormat;
    public String damageFormat;
    public String percentageFormat;

    @Override
    public void onEnable() {
        // Initialize boss configurations
        bossConfigs = new HashMap<>();
        // Load configuration from file
        loadConfig();
        // Initialize the damage manager
        initializeDamageManager();
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

    private void registerHandlers() {
        // Register event listeners
        getServer().getPluginManager().registerEvents(new MythicMobListeners(this), this);
        // Register placeholder
        new DamageTrackerPlaceholder(this).register();
        
        // Register command handler
        DamageTrackerCommand commandHandler = new DamageTrackerCommand(this);
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
        // Get boss configurations section from config
        var bossesSection = getConfig().getConfigurationSection("bosses");
        if (bossesSection != null) {
            // Iterate through each boss configuration
            for (String bossName : bossesSection.getKeys(false)) {
                var bossSection = bossesSection.getConfigurationSection(bossName);
                if (bossSection != null && !bossSection.getKeys(false).isEmpty()) {
                    // Add boss configuration to map
                    bossConfigs.put(bossName.toUpperCase(), new BossConfig(
                        bossSection.getString("victory_message"),
                        bossSection.getInt("top_players_to_show", 3),
                        bossSection.getStringList("top_players_format")
                    ));
                } else {
                    // Add null configuration if section is empty
                    bossConfigs.put(bossName.toUpperCase(), null);
                }
            }
        }
        getLogger().info("Configuration loaded. Number of configured bosses: " + bossConfigs.size());
    }

    private void loadDefaultConfig() {
        // Get default boss configuration section from config
        var defaultSection = getConfig().getConfigurationSection("default_boss_config");
        if (defaultSection != null) {
            // Initialize default boss configuration
            defaultBossConfig = new BossConfig(
                defaultSection.getString("victory_message"),
                defaultSection.getInt("top_players_to_show", 3),
                defaultSection.getStringList("top_players_format")
            );
        } else {
            // Create a default configuration if none exists
            defaultBossConfig = new BossConfig();
        }
    }

    private void loadPersonalMessageFormat() {
        // Get personal message format from config
        personalMessageFormat = getConfig().getString("personal_message_format",
            "&6Your contribution: &ePosition: {position}, Damage: {damage} ({percentage}%)");
    }

    public void sendMessage(Player player, String message) {
        // Send a message to a player using Adventure API
        if (player != null && message != null && adventure != null) {
            Component component = MessageUtils.deserialize(message);
            adventure.player(player).sendMessage(component);
        }
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
    public Map<String, BossConfig> getBossConfigs() {
        return bossConfigs;
    }

    public BossConfig getDefaultBossConfig() {
        return defaultBossConfig;
    }

    public int getDefaultTopPlayersToShow() {
        return defaultBossConfig != null ? defaultBossConfig.getTopPlayersToShow() : 3;
    }

    public String getPersonalMessageFormat() {
        return personalMessageFormat;
    }

    public String getPlayerPrefix(Player player) {
        // Get player prefix using Vault or LuckPerms
        if (useVault && vaultChat != null) {
            return vaultChat.getPlayerPrefix(player);
        } else if (useLuckPerms && luckPerms != null) {
            var user = luckPerms.getUserManager().getUser(player.getUniqueId());
            return user != null ? user.getCachedData().getMetaData().getPrefix() : "";
        }
        return "";
    }

    // Methods delegated to DamageManager
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
}