package com.elplatano0871.damagetracker;

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
import java.util.*;
import java.util.stream.Collectors;

public class DamageTracker extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private Map<UUID, Map<UUID, Double>> bossDamageMaps;
    private Map<String, BossConfig> bossConfigs;
    private BossConfig defaultBossConfig;
    private Map<UUID, Double> bossMaxHealth;
    private String damageFormat;

    private static class BossConfig {
        String victoryMessage;
        int topPlayersToShow;

        BossConfig(String victoryMessage, int topPlayersToShow) {
            this.victoryMessage = victoryMessage;
            this.topPlayersToShow = topPlayersToShow;
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
        
        displayAsciiArt();
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
                    bossConfigs.put(bossName.toUpperCase(), new BossConfig(victoryMessage, topPlayersToShow));
                } else {
                    bossConfigs.put(bossName.toUpperCase(), null);
                }
            }
        }

        ConfigurationSection defaultSection = getConfig().getConfigurationSection("default_boss_config");
        if (defaultSection != null) {
            String defaultVictoryMessage = defaultSection.getString("victory_message");
            int defaultTopPlayersToShow = defaultSection.getInt("top_players_to_show", 3);
            defaultBossConfig = new BossConfig(defaultVictoryMessage, defaultTopPlayersToShow);
        }

        damageFormat = getConfig().getString("damage_format", "%.2f");
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
            getLogger().info("Boss " + mobInternalName + " not found in configuration. Ignoring.");
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

        String message = bossConfig.victoryMessage
            .replace("{boss_name}", activeMob.getDisplayName());

        StringBuilder topPlayersMessage = new StringBuilder();
        String[] positions = {"&aPrimer puesto: ", "&eSegundo puesto: ", "&6Tercer puesto: ", "&bCuarto puesto: ", "&dQuinto puesto: "};
        for (int i = 0; i < Math.min(bossConfig.topPlayersToShow, topPlayers.size()); i++) {
            Map.Entry<UUID, Double> entry = topPlayers.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                String positionPrefix = i < positions.length ? positions[i] : "&7" + (i + 1) + "º puesto: ";
                topPlayersMessage.append(positionPrefix)
                   .append("&7").append(player.getName())
                   .append(" &c").append(String.format(damageFormat, entry.getValue()))
                   .append("\n");
            }
        }
        message = message.replace("{top_players}", topPlayersMessage.toString());

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
        
        bossDamageMaps.remove(mobUniqueId);
        bossMaxHealth.remove(mobUniqueId);
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
        String pluginName = "DamageTracker for MythicMobs";
        String version = getDescription().getVersion();
        String enableMessage = String.format("%s v%s has been enabled!", pluginName, version);

        String[] asciiArt = {
            "§9______  §f_    _§9______ ",
            "§9| ___ \\§f| |  | |§9 ___ \\",
            "§9| |_/ /§f| |  | |§9 |_/ /",
            "§9| ___ \\§f| |/\\| |§9 ___ \\",
            "§9| |_/ /§f\\  /\\  /§9 |_/ /",
            "§9\\____/  §f\\/  \\/§9\\____/ "
        };

        int maxAsciiWidth = Arrays.stream(asciiArt).mapToInt(String::length).max().orElse(0);
        int padding = 2;

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