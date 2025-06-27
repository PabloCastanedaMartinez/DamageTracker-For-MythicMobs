package com.elplatano0871.damagetracker.managers;

import com.elplatano0871.damagetracker.DamageTracker;
import com.elplatano0871.damagetracker.configs.BossConfig;
import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.Vector3f;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {
    private final DamageTracker plugin;
    private final Map<String, String> activeHolograms;
    private boolean fancyHologramsAvailable;
    private FileConfiguration hologramConfig;
    private File hologramConfigFile;
    
    // Hologram configurations
    private String backgroundColor;
    private int hologramDuration;
    
    public HologramManager(DamageTracker plugin) {
        this.plugin = plugin;
        this.activeHolograms = new ConcurrentHashMap<>();
        this.fancyHologramsAvailable = checkFancyHologramsAvailability();
        loadHologramConfig();
    }
    
    /**
     * Loads hologram configuration from holograms_config.yml
     */
    private void loadHologramConfig() {
        hologramConfigFile = new File(plugin.getDataFolder(), "holograms_config.yml");
        
        // Create default file if it doesn't exist
        if (!hologramConfigFile.exists()) {
            plugin.saveResource("holograms_config.yml", false);
        }
        
        hologramConfig = YamlConfiguration.loadConfiguration(hologramConfigFile);
        
        // Load configurations
        backgroundColor = hologramConfig.getString("background_color", "transparent");
        hologramDuration = hologramConfig.getInt("hologram_duration", 30);
        
        plugin.getLogger().info("Hologram configuration loaded: background_color=" + backgroundColor + ", duration=" + hologramDuration + "s");
    }
    
    /**
     * Reloads hologram configuration
     */
    public void reloadHologramConfig() {
        loadHologramConfig();
    }
    
    /**
     * Converts text color to Bukkit Color
     */
    private Color parseBackgroundColor(String colorName) {
        if (colorName == null || colorName.equalsIgnoreCase("transparent") || colorName.equalsIgnoreCase("default")) {
            return Color.fromARGB(0, 0, 0, 0); 
        }
        
        // Predefined Minecraft colors
        switch (colorName.toLowerCase()) {
            case "black": return Color.fromRGB(0, 0, 0);
            case "dark_blue": return Color.fromRGB(0, 0, 170);
            case "dark_green": return Color.fromRGB(0, 170, 0);
            case "dark_aqua": return Color.fromRGB(0, 170, 170);
            case "dark_red": return Color.fromRGB(170, 0, 0);
            case "dark_purple": return Color.fromRGB(170, 0, 170);
            case "gold": return Color.fromRGB(255, 170, 0);
            case "gray": return Color.fromRGB(170, 170, 170);
            case "dark_gray": return Color.fromRGB(85, 85, 85);
            case "blue": return Color.fromRGB(85, 85, 255);
            case "green": return Color.fromRGB(85, 255, 85);
            case "aqua": return Color.fromRGB(85, 255, 255);
            case "red": return Color.fromRGB(255, 85, 85);
            case "light_purple": return Color.fromRGB(255, 85, 255);
            case "yellow": return Color.fromRGB(255, 255, 85);
            case "white": return Color.fromRGB(255, 255, 255);
            default:
                // Try to parse as hexadecimal RGB (#RRGGBB)
                if (colorName.startsWith("#") && colorName.length() == 7) {
                    try {
                        int rgb = Integer.parseInt(colorName.substring(1), 16);
                        return Color.fromRGB(rgb);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid color format: " + colorName + ". Using default.");
                    }
                }
                return Color.fromRGB(85, 85, 85);
        }
    }
    
    /**
     * Checks if FancyHolograms is available
     */
    private boolean checkFancyHologramsAvailability() {
        try {
            return Bukkit.getPluginManager().getPlugin("FancyHolograms") != null &&
                   FancyHologramsPlugin.get() != null;
        } catch (Exception e) {
            plugin.getLogger().warning("FancyHolograms is not available: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Creates a victory hologram for a boss
     */
    public void createVictoryHologram(String bossName, String hologramType, String victoryMessageId, 
                                     List<UUID> participants, Location bossLocation, 
                                     Map<UUID, Double> bossDamageMap, double maxHealth, String displayName) {
        // Check if hologram type is FANCY
        if (!"FANCY".equalsIgnoreCase(hologramType)) {
            return;
        }
        
        // Check if FancyHolograms is available
        if (!fancyHologramsAvailable) {
            plugin.getLogger().warning("Could not create hologram: FancyHolograms is not available");
            return;
        }
        
        try {
            // Calculate average location of participants
            Location hologramLocation = calculateParticipantsLocation(participants, bossLocation);
            if (hologramLocation == null) {
                plugin.getLogger().warning("Could not calculate location for boss hologram: " + bossName);
                return;
            }
            
            // Get victory message
            String victoryMessage = plugin.getVictoryMessageManager().getVictoryMessage(victoryMessageId);
            if (victoryMessage == null) {
                victoryMessage = "Boss defeated!";
            }
            
            // Process placeholders
            String processedMessage = processVictoryMessagePlaceholders(victoryMessage, displayName, bossDamageMap, maxHealth);
            
            // Create unique name for hologram
            String hologramName = "victory_" + bossName.toLowerCase() + "_" + System.currentTimeMillis();
            
            // Create hologram data
            TextHologramData hologramData = new TextHologramData(hologramName, hologramLocation);
            
            // Configure text as list of lines (split by line breaks)
            List<String> lines = Arrays.asList(processedMessage.replace("&", "§").split("\\n"));
            hologramData.setText(lines);
            
            // Configure background color using configuration
            Color bgColor = parseBackgroundColor(backgroundColor);
            if (bgColor != null) {
                hologramData.setBackground(bgColor);
            }
            
            // Configure billboard
            hologramData.setBillboard(Display.Billboard.CENTER);
            
            // Configure scale using Vector3f
            hologramData.setScale(new Vector3f(1.2f, 1.2f, 1.2f));
            
            // Create and register hologram
            de.oliver.fancyholograms.api.HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
            Hologram hologram = manager.create(hologramData);
            manager.addHologram(hologram);
            
            // Save reference of active hologram
            activeHolograms.put(hologramName, bossName);
            
            plugin.getLogger().info("Victory hologram created for boss: " + bossName + " at " + 
                                  hologramLocation.getX() + ", " + hologramLocation.getY() + ", " + hologramLocation.getZ());
            
            // Schedule automatic hologram removal using configured duration
            scheduleHologramRemoval(hologramName, hologramDuration);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error creating victory hologram for " + bossName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Processes placeholders in victory message
     */
    private String processVictoryMessagePlaceholders(String victoryMessage, String bossDisplayName, 
                                                    Map<UUID, Double> bossDamageMap, double maxHealth) {
        // Replace {boss_name}
        String processedMessage = victoryMessage.replace("{boss_name}", bossDisplayName);
        
        // Process {top_players} if exists
        if (processedMessage.contains("{top_players}")) {
            String topPlayersMessage = generateTopPlayersMessage(bossDamageMap, maxHealth);
            processedMessage = processedMessage.replace("{top_players}", topPlayersMessage);
        }
        
        // Replace {personal_damage} with general statistics
        if (processedMessage.contains("{personal_damage}")) {
            String generalStats = generateGeneralDamageStats(bossDamageMap, maxHealth);
            processedMessage = processedMessage.replace("{personal_damage}", generalStats);
        }
        
        return processedMessage;
    }
    
    /**
     * Generates general damage statistics to replace {personal_damage}
     */
    private String generateGeneralDamageStats(Map<UUID, Double> bossDamageMap, double maxHealth) {
        if (bossDamageMap == null || bossDamageMap.isEmpty()) {
            return "";
        }
        
        double totalDamage = bossDamageMap.values().stream().mapToDouble(Double::doubleValue).sum();
        int participantCount = bossDamageMap.size();
        
        String totalDamageStr = plugin.formatDamage(totalDamage, maxHealth, "numeric");
        
        return String.format("<gray>Total: %s | %d participants", totalDamageStr, participantCount);
    }
    
    /**
     * Generates top players message
     */
    private String generateTopPlayersMessage(Map<UUID, Double> bossDamageMap, double maxHealth) {
        if (bossDamageMap == null || bossDamageMap.isEmpty()) {
            return "No participants";
        }
        
        VictoryMessageManager messageManager = plugin.getVictoryMessageManager();
        
        // Get boss configuration (use default configuration if not found)
        BossConfig defaultConfig = plugin.getDefaultBossConfig();
        int topPlayersToShow = defaultConfig.getTopPlayersToShow();
        List<String> positionFormats = messageManager.getPositionFormat(defaultConfig.getPositionFormatId());
        
        // Get top players
        List<Map.Entry<UUID, Double>> topPlayers = plugin.getTopDamage(bossDamageMap, topPlayersToShow);
        double totalDamage = bossDamageMap.values().stream().mapToDouble(Double::doubleValue).sum();
        
        // Create top players message
        StringBuilder topPlayersMessage = new StringBuilder();
        for (int i = 0; i < Math.min(topPlayersToShow, topPlayers.size()); i++) {
            Map.Entry<UUID, Double> entry = topPlayers.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                String format = i < positionFormats.size() ?
                        positionFormats.get(i) :
                        "<gray>{player_name}: {damage} ({percentage}%)";
                
                String damageStr = plugin.formatDamage(entry.getValue(), maxHealth, "numeric");
                double percentage = (entry.getValue() / totalDamage) * 100;
                String percentageStr = String.format(plugin.percentageFormat, percentage);
                
                String prefix = plugin.getPlayerPrefix(player);
                prefix = prefix.replaceAll("§([0-9a-fk-or])", "<$1>");
                
                format = format.replace("{player_name}", player.getName())
                        .replace("{damage}", damageStr)
                        .replace("{percentage}", percentageStr)
                        .replace("{prefix}", prefix);
                
                topPlayersMessage.append(format);
                if (i < Math.min(topPlayersToShow, topPlayers.size()) - 1) {
                    topPlayersMessage.append("\n");
                }
            }
        }
        
        return topPlayersMessage.toString();
    }
    
    /**
     * Calculates average location of participants
     */
    private Location calculateParticipantsLocation(List<UUID> participants, Location fallbackLocation) {
        if (participants == null || participants.isEmpty()) {
            return fallbackLocation;
        }
        
        List<Location> validLocations = new ArrayList<>();
        
        for (UUID participantId : participants) {
            Player player = Bukkit.getPlayer(participantId);
            if (player != null && player.isOnline()) {
                validLocations.add(player.getLocation());
            }
        }
        
        if (validLocations.isEmpty()) {
            return fallbackLocation;
        }
        
        // Calculate average location
        double avgX = validLocations.stream().mapToDouble(loc -> loc.getX()).average().orElse(0);
        double avgY = validLocations.stream().mapToDouble(loc -> loc.getY()).average().orElse(0) + 3; // 3 blocks above
        double avgZ = validLocations.stream().mapToDouble(loc -> loc.getZ()).average().orElse(0);
        
        Location avgLocation = new Location(validLocations.get(0).getWorld(), avgX, avgY, avgZ);
        
        plugin.getLogger().info("Calculated location for hologram: " + avgX + ", " + avgY + ", " + avgZ + 
                              " (based on " + validLocations.size() + " participants)");
        
        return avgLocation;
    }
    
    /**
     * Schedules automatic removal of a hologram
     */
    private void scheduleHologramRemoval(String hologramName, int delaySeconds) {
        new BukkitRunnable() {
            @Override
            public void run() {
                removeHologram(hologramName);
            }
        }.runTaskLater(plugin, delaySeconds * 20L); // 20 ticks = 1 second
    }
    
    /**
     * Removes a specific hologram
     */
    public void removeHologram(String hologramName) {
        if (!fancyHologramsAvailable) {
            return;
        }
        
        try {
            de.oliver.fancyholograms.api.HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
            
            // Get hologram by name and remove it
            Optional<Hologram> hologramOpt = manager.getHologram(hologramName);
            if (hologramOpt.isPresent()) {
                manager.removeHologram(hologramOpt.get());
                
                String bossName = activeHolograms.remove(hologramName);
                if (bossName != null) {
                    plugin.getLogger().info("Hologram removed for boss: " + bossName);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing hologram " + hologramName + ": " + e.getMessage());
        }
    }
    
    /**
     * Removes all active holograms
     */
    public void removeAllHolograms() {
        for (String hologramName : new HashSet<>(activeHolograms.keySet())) {
            removeHologram(hologramName);
        }
    }
    
    /**
     * Checks if FancyHolograms is available
     */
    public boolean isFancyHologramsAvailable() {
        return fancyHologramsAvailable;
    }
}
