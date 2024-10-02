package com.elplatano0871.damagetracker;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.command.TabCompleter;
import net.milkbowl.vault.chat.Chat;
import net.luckperms.api.LuckPerms;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DamageTracker extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private BukkitAudiences adventure;
    private MiniMessage miniMessage;
    private Map<UUID, Map<UUID, Double>> bossDamageMaps;
    private Map<String, BossConfig> bossConfigs;
    private BossConfig defaultBossConfig;
    private Map<UUID, Double> bossMaxHealth;
    private String damageFormat;
    private String percentageFormat;
    private boolean useVault;
    private boolean useLuckPerms;
    private Chat vaultChat;
    private LuckPerms luckPerms;
    private String personalMessageFormat;

    private static class BossConfig {
        String victoryMessage;
        int topPlayersToShow;
        List<String> topPlayersFormat;
        String damageDisplay;

        BossConfig(String victoryMessage, int topPlayersToShow, List<String> topPlayersFormat, String damageDisplay) {
            this.victoryMessage = victoryMessage;
            this.topPlayersToShow = topPlayersToShow;
            this.topPlayersFormat = topPlayersFormat;
            this.damageDisplay = damageDisplay;
        }
    }

    @Override
    public void onEnable() {
        bossDamageMaps = new HashMap<>();
        bossConfigs = new HashMap<>();
        bossMaxHealth = new HashMap<>();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        new DamageTrackerPlaceholder(this).register();
        getCommand("damagetracker").setExecutor(this);
        getCommand("damagetracker").setTabCompleter(this);
        this.adventure = BukkitAudiences.create(this);
        this.miniMessage = MiniMessage.miniMessage();
        setupVault();
        setupLuckPerms();
        personalMessageFormat = getConfig().getString("personal_message_format",
            "&6Your contribution: &ePosition: {position}, Damage: {damage} ({percentage}%)");
        
        displayAsciiArt();
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

    private String convertLegacyAndHexToMiniMessage(String input) {
        // Convert legacy color codes
        String result = ChatColor.translateAlternateColorCodes('&', input);
        
        // Convert hex color codes
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#$1>");
        }
        matcher.appendTail(sb);
        result = sb.toString();
        
        // Convert legacy color codes to MiniMessage format
        result = result.replace("§0", "<black>")
                       .replace("§1", "<dark_blue>")
                       .replace("§2", "<dark_green>")
                       .replace("§3", "<dark_aqua>")
                       .replace("§4", "<dark_red>")
                       .replace("§5", "<dark_purple>")
                       .replace("§6", "<gold>")
                       .replace("§7", "<gray>")
                       .replace("§8", "<dark_gray>")
                       .replace("§9", "<blue>")
                       .replace("§a", "<green>")
                       .replace("§b", "<aqua>")
                       .replace("§c", "<red>")
                       .replace("§d", "<light_purple>")
                       .replace("§e", "<yellow>")
                       .replace("§f", "<white>")
                       .replace("§l", "<bold>")
                       .replace("§m", "<strikethrough>")
                       .replace("§n", "<underline>")
                       .replace("§o", "<italic>")
                       .replace("§r", "<reset>");
        
        return result;
    }

    private void sendMessage(Player player, String message) {
        message = convertLegacyAndHexToMiniMessage(message);
        Component component = miniMessage.deserialize(message);
        adventure.player(player).sendMessage(component);
    }

    private void setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            org.bukkit.plugin.RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp != null) {
                vaultChat = rsp.getProvider();
                useVault = true;
                getLogger().info("Vault integration enabled.");
            }
        }
    }

    private void setupLuckPerms() {
        org.bukkit.plugin.RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
            useLuckPerms = true;
            getLogger().info("LuckPerms integration enabled.");
        }
    }

    private String getPlayerPrefix(Player player) {
        String prefix = "";
        if (useVault && vaultChat != null) {
            prefix = vaultChat.getPlayerPrefix(player);
        } else if (useLuckPerms && luckPerms != null) {
            net.luckperms.api.model.user.User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                prefix = user.getCachedData().getMetaData().getPrefix();
            }
        }
        return prefix.replaceAll("§([0-9a-fk-or])", "<$1>");
    }

    private void loadConfig() {
        saveDefaultConfig();
        reloadConfig();

        ConfigurationSection bossesSection = getConfig().getConfigurationSection("bosses");
        if (bossesSection != null) {
            for (String bossName : bossesSection.getKeys(false)) {
                ConfigurationSection bossSection = bossesSection.getConfigurationSection(bossName);
                if (bossSection != null && !bossSection.getKeys(false).isEmpty()) {
                    String victoryMessage = bossSection.getString("victory_message");
                    int topPlayersToShow = bossSection.getInt("top_players_to_show", 3);
                    List<String> topPlayersFormat = bossSection.getStringList("top_players_format");
                    String damageDisplay = bossSection.getString("damage_display", "percentage");
                    bossConfigs.put(bossName.toUpperCase(), new BossConfig(victoryMessage, topPlayersToShow, topPlayersFormat, damageDisplay));
                } else {
                    bossConfigs.put(bossName.toUpperCase(), null);
                }
            }
        }

        ConfigurationSection defaultSection = getConfig().getConfigurationSection("default_boss_config");
        if (defaultSection != null) {
            String defaultVictoryMessage = defaultSection.getString("victory_message");
            int defaultTopPlayersToShow = defaultSection.getInt("top_players_to_show", 3);
            List<String> defaultTopPlayersFormat = defaultSection.getStringList("top_players_format");
            String defaultDamageDisplay = defaultSection.getString("damage_display", "percentage");
            defaultBossConfig = new BossConfig(defaultVictoryMessage, defaultTopPlayersToShow, defaultTopPlayersFormat, defaultDamageDisplay);
        }

        damageFormat = getConfig().getString("damage_format", "%.2f");
        percentageFormat = getConfig().getString("percentage_format", "%.1f%%");
        getLogger().info("Configuration loaded. Number of configured bosses: " + bossConfigs.size());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("damagetracker")) {
            if (args.length > 0) {
                switch (args[0].toLowerCase()) {
                    case "reload":
                        return handleReloadCommand(sender);
                    case "damage":
                        return handleDamageCommand(sender);
                    case "top":
                        return handleTopCommand(sender);
                }
            }
            sender.sendMessage(ChatColor.YELLOW + "Available commands: /damagetracker reload, /damagetracker damage, /damagetracker top");
            return true;
        }
        return false;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("damagetracker")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                completions.add("reload");
                completions.add("damage");
                completions.add("top");
                return completions.stream()
                        .filter(c -> c.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (sender.hasPermission("damagetracker.reload")) {
            try {
                loadConfig();
                sender.sendMessage(ChatColor.GREEN + "DamageTracker configuration reloaded!");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Error reloading configuration: " + e.getMessage());
                getLogger().severe("Error reloading configuration: " + e.getMessage());
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
        }
        return true;
    }

    private boolean handleDamageCommand(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            double totalDamage = 0;
            for (Map<UUID, Double> damageMap : bossDamageMaps.values()) {
                totalDamage += damageMap.getOrDefault(player.getUniqueId(), 0.0);
            }
            sender.sendMessage(ChatColor.YELLOW + "Your current total damage: " + String.format(damageFormat, totalDamage));
        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
        }
        return true;
    }

    private boolean handleTopCommand(CommandSender sender) {
        int topPlayersToShow = defaultBossConfig != null ? defaultBossConfig.topPlayersToShow : 3;
        Map<UUID, Double> totalDamageMap = new HashMap<>();
        
        for (Map<UUID, Double> damageMap : bossDamageMaps.values()) {
            for (Map.Entry<UUID, Double> entry : damageMap.entrySet()) {
                totalDamageMap.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }
        
        List<Map.Entry<UUID, Double>> topPlayers = getTopDamage(totalDamageMap, topPlayersToShow);
        sender.sendMessage(ChatColor.YELLOW + "Top " + topPlayersToShow + " damage dealers across all bosses:");
        for (int i = 0; i < topPlayers.size(); i++) {
            Map.Entry<UUID, Double> entry = topPlayers.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                sender.sendMessage(ChatColor.GOLD + String.valueOf(i + 1) + ". " + ChatColor.GREEN + player.getName() + 
                                   ChatColor.YELLOW + ": " + String.format(damageFormat, entry.getValue()));
            }
        }
        return true;
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        ActiveMob activeMob = event.getMob();
        String mobInternalName = activeMob.getMobType();
        UUID mobUniqueId = activeMob.getUniqueId();

        if (!bossConfigs.containsKey(mobInternalName.toUpperCase())) {
            return;
        }

        BossConfig bossConfig = bossConfigs.get(mobInternalName.toUpperCase());
        if (bossConfig == null) {
            getLogger().info("Using default configuration for boss: " + mobInternalName);
            bossConfig = defaultBossConfig;
        }

        if (bossConfig == null || bossConfig.victoryMessage == null) {
            getLogger().warning("No victory message configured for boss: " + mobInternalName);
            return;
        }

        Map<UUID, Double> bossDamageMap = bossDamageMaps.getOrDefault(mobUniqueId, new HashMap<>());
        List<Map.Entry<UUID, Double>> topPlayers = getTopDamage(bossDamageMap, bossConfig.topPlayersToShow);

        StringBuilder topPlayersMessage = new StringBuilder();
        double maxHealth = bossMaxHealth.getOrDefault(mobUniqueId, 0.0);
        double totalDamage = bossDamageMap.values().stream().mapToDouble(Double::doubleValue).sum();

        for (int i = 0; i < Math.min(bossConfig.topPlayersToShow, topPlayers.size()); i++) {
            Map.Entry<UUID, Double> entry = topPlayers.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                String format = i < bossConfig.topPlayersFormat.size() ? bossConfig.topPlayersFormat.get(i) : "<gray>{player_name}: <red>{damage}";
                String damageString = formatDamage(entry.getValue(), maxHealth, bossConfig.damageDisplay);
                String prefix = getPlayerPrefix(player);
                prefix = prefix.replaceAll("§([0-9a-fk-or])", "<$1>");
                format = format.replace("{player_name}", player.getName())
                               .replace("{damage}", damageString)
                               .replace("{prefix}", prefix);
                topPlayersMessage.append(format).append("\n");
            }
        }

        String message = bossConfig.victoryMessage
        .replace("{boss_name}", activeMob.getDisplayName())
        .replace("{top_players}", topPlayersMessage.toString());
    
    for (Player player : Bukkit.getOnlinePlayers()) {
        sendMessage(player, message);
    }

        // Send personal messages to all participants
        List<Map.Entry<UUID, Double>> sortedDamageList = new ArrayList<>(bossDamageMap.entrySet());
        sortedDamageList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        for (int i = 0; i < sortedDamageList.size(); i++) {
            Map.Entry<UUID, Double> entry = sortedDamageList.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                double damage = entry.getValue();
                double percentage = (damage / totalDamage) * 100;
                String personalMessage = personalMessageFormat
                .replace("{position}", String.valueOf(i + 1))
                .replace("{damage}", formatDamage(damage, maxHealth, "numeric"))
                .replace("{percentage}", String.format("%.2f", percentage));
            personalMessage = personalMessage.replaceAll("§([0-9a-fk-or])", "<$1>");
            sendMessage(player, personalMessage);
            }
        }
        
        bossDamageMaps.remove(mobUniqueId);
        bossMaxHealth.remove(mobUniqueId);
    }


    private String formatDamage(double damage, double maxHealth, String displayType) {
        if ("percentage".equalsIgnoreCase(displayType) && maxHealth > 0) {
            double percentage = (damage / maxHealth) * 100;
            return String.format(percentageFormat, percentage);
        } else {
            return String.format(damageFormat, damage);
        }
    }

    @EventHandler
    public void onMythicMobDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Player player = (Player) event.getDamager();
        LivingEntity entity = (LivingEntity) event.getEntity();

        try {
            ActiveMob activeMob = MythicBukkit.inst().getMobManager().getMythicMobInstance(entity);
            if (activeMob != null && bossConfigs.containsKey(activeMob.getMobType().toUpperCase())) {
                UUID mobUniqueId = activeMob.getUniqueId();
                addDamage(mobUniqueId, player, event.getFinalDamage());
                
                if (!bossMaxHealth.containsKey(mobUniqueId)) {
                    bossMaxHealth.put(mobUniqueId, ((LivingEntity) activeMob.getEntity().getBukkitEntity()).getMaxHealth());
                }
            }
        } catch (Exception e) {
            getLogger().warning("Error processing damage event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addDamage(UUID bossId, Player player, double damage) {
        UUID playerId = player.getUniqueId();
        bossDamageMaps.computeIfAbsent(bossId, k -> new HashMap<>())
                      .compute(playerId, (k, v) -> v == null ? damage : v + damage);
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

    public Map<UUID, Map<UUID, Double>> getAllDamageData() {
        return new HashMap<>(bossDamageMaps);
    }
    
    public double getBossMaxHealth(UUID bossId) {
        return bossMaxHealth.getOrDefault(bossId, 0.0);
    }

    private void displayAsciiArt() {
        String version = getDescription().getVersion();
        String enableMessage = String.format("v%s has been enabled!", version);

        String[] asciiArt = {
            " ____                                  _____               _             ",
            "|  _ \\  __ _ _ __ ___   __ _  __ _  __|_   _| __ __ _  ___| | _____ _ __",
            "| | | |/ _` | '_ ` _ \\ / _` |/ _` |/ _ \\| || '__/ _` |/ __| |/ / _ \\ '__|",
            "| |_| | (_| | | | | | | (_| | (_| |  __/| || | | (_| | (__|   <  __/ |  ",
            "|____/ \\__,_|_| |_| |_|\\__,_|\\__, |\\___|_||_|  \\__,_|\\___|_|\\_\\___|_|  ",
            "                             |___/                                      "
        };

        int maxAsciiWidth = Arrays.stream(asciiArt).mapToInt(String::length).max().orElse(0);

        for (int i = 0; i < asciiArt.length; i++) {
            StringBuilder line = new StringBuilder(asciiArt[i]);
            while (line.length() < maxAsciiWidth) {
                line.append(" ");
            }
            line.append("  ");

            if (i == 2) {
                line.append("§9").append(enableMessage);
            }

            Bukkit.getConsoleSender().sendMessage(line.toString());
        }
    }
}