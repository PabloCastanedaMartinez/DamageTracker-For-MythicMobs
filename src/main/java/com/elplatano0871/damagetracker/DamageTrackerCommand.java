package com.elplatano0871.damagetracker;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DamageTrackerCommand implements CommandExecutor, TabCompleter {
    private final DamageTracker plugin;
    private final String damageFormat;

    public DamageTrackerCommand(DamageTracker plugin) {
        this.plugin = plugin;
        this.damageFormat = plugin.getConfig().getString("damage_format", "%.2f");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("damagetracker")) {
            if (args.length > 0) {
                return switch (args[0].toLowerCase()) {
                    case "reload" -> handleReloadCommand(sender);
                    case "damage" -> handleDamageCommand(sender);
                    case "top" -> handleTopCommand(sender);
                    default -> showHelp(sender);
                };
            }
            return showHelp(sender);
        }
        return false;
    }

    private boolean showHelp(CommandSender sender) {
        MessageUtils.sendMessage(sender, "&eCommands available:");
        MessageUtils.sendMessage(sender, "&6/damagetracker reload &e- Reloads the configuration");
        MessageUtils.sendMessage(sender, "&6/damagetracker damage &e- Shows your current damage");
        MessageUtils.sendMessage(sender, "&6/damagetracker top &e- Shows the best players");
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("damagetracker.reload")) {
            MessageUtils.sendMessage(sender, "&cYou don't have permission to use this command.");
            return true;
        }

        try {
            plugin.loadConfig();
            MessageUtils.sendMessage(sender, "&aConfiguration of DamageTracker reloaded!");
        } catch (Exception e) {
            MessageUtils.sendMessage(sender, "&cError reloading configuration: " + e.getMessage());
        }
        return true;
    }

    private boolean handleDamageCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        double totalDamage = 0;
        Map<UUID, Map<UUID, Double>> allDamageData = plugin.getAllDamageData();
        
        for (Map<UUID, Double> damageMap : allDamageData.values()) {
            totalDamage += damageMap.getOrDefault(player.getUniqueId(), 0.0);
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Your current total damage: " + 
            String.format(damageFormat, totalDamage));
        return true;
    }

    private boolean handleTopCommand(CommandSender sender) {
        int topPlayersToShow = plugin.getDefaultTopPlayersToShow();
        Map<UUID, Double> totalDamageMap = plugin.calculateTotalDamage();
        
        List<Map.Entry<UUID, Double>> topPlayers = plugin.getTopDamage(totalDamageMap, topPlayersToShow);
        sender.sendMessage(ChatColor.YELLOW + "Top " + topPlayersToShow + " players with most damage:");
        
        for (int i = 0; i < topPlayers.size(); i++) {
            Map.Entry<UUID, Double> entry = topPlayers.get(i);
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                sender.sendMessage(ChatColor.GOLD + String.valueOf(i + 1) + ". " + 
                    ChatColor.GREEN + player.getName() + 
                    ChatColor.YELLOW + ": " + 
                    String.format(damageFormat, entry.getValue()));
            }
        }
        return true;
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
}