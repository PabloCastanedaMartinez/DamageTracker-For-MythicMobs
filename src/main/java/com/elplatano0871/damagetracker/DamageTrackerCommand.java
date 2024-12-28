package com.elplatano0871.damagetracker;

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

/**
 * Command executor and tab completer for the DamageTracker plugin.
 */
public class DamageTrackerCommand implements CommandExecutor, TabCompleter {
    private final DamageTracker plugin;
    private final String damageFormat;
    private final TrackedBossCommands trackedBossCommands;

    /**
     * Constructor for DamageTrackerCommand.
     *
     * @param plugin The main plugin instance.
     * @param trackedBossManager The manager for tracked bosses.
     */
    public DamageTrackerCommand(DamageTracker plugin, TrackedBossManager trackedBossManager) {
        this.plugin = plugin;
        this.damageFormat = plugin.getConfig().getString("damage_format", "%.2f");
        this.trackedBossCommands = new TrackedBossCommands(plugin, trackedBossManager);
    }

    /**
     * Handles the execution of the /damagetracker command.
     *
     * @param sender The sender of the command.
     * @param command The command that was executed.
     * @param label The alias of the command that was used.
     * @param args The arguments passed to the command.
     * @return true if the command was handled successfully, false otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("damagetracker")) {
            return false;
        }

        if (args.length == 0) {
            return showHelp(sender);
        }

        return switch (args[0].toLowerCase()) {
            case "reload" -> handleReloadCommand(sender);
            case "damage" -> handleDamageCommand(sender);
            case "top" -> handleTopCommand(sender);
            case "check" -> trackedBossCommands.handleCheckDamageCommand(sender, args);
            case "checktop" -> trackedBossCommands.handleCheckTopCommand(sender, args);
            case "clear" -> trackedBossCommands.handleClearDataCommand(sender, args);
            default -> showHelp(sender);
        };
    }

    /**
     * Shows the help message for the /damagetracker command.
     *
     * @param sender The sender of the command.
     * @return true always.
     */
    private boolean showHelp(CommandSender sender) {
        MessageUtils.sendMessage(sender, "&eCommands available:");
        MessageUtils.sendMessage(sender, "&6/damagetracker reload &e- Reloads the configuration");
        MessageUtils.sendMessage(sender, "&6/damagetracker damage &e- Shows your current damage");
        MessageUtils.sendMessage(sender, "&6/damagetracker top &e- Shows the best players");
        MessageUtils.sendMessage(sender, "&6/damagetracker check <bossId> &e- Check your damage to a specific boss");
        MessageUtils.sendMessage(sender, "&6/damagetracker checktop <bossId> &e- Show top damage for a specific boss");
        if (sender.hasPermission("damagetracker.cleardata")) {
            MessageUtils.sendMessage(sender, "&6/damagetracker clear <bossId> &e- Clear damage data for a specific boss");
        }
        return true;
    }

    /**
     * Handles the /damagetracker reload command.
     *
     * @param sender The sender of the command.
     * @return true always.
     */
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("damagetracker.reload")) {
            MessageUtils.sendMessage(sender, "&cYou don't have permission to use this command.");
            return true;
        }

        try {
            // Reload main plugin configuration
            plugin.loadConfig();

            // Explicitly mention what was reloaded
            MessageUtils.sendMessage(sender, "&aConfiguration reloaded successfully!");
            MessageUtils.sendMessage(sender, "&7- Main configuration");
            MessageUtils.sendMessage(sender, "&7- Boss configurations");
            MessageUtils.sendMessage(sender, "&7- Tracked bosses configuration");
            MessageUtils.sendMessage(sender, "&7- Message formats");
        } catch (Exception e) {
            MessageUtils.sendMessage(sender, "&cError reloading configuration: " + e.getMessage());
            plugin.getLogger().severe("Error during config reload: " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Handles the /damagetracker damage command.
     *
     * @param sender The sender of the command.
     * @return true always.
     */
    private boolean handleDamageCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendMessage(sender, "&cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        double totalDamage = 0;
        Map<UUID, Map<UUID, Double>> allDamageData = plugin.getAllDamageData();

        for (Map<UUID, Double> damageMap : allDamageData.values()) {
            totalDamage += damageMap.getOrDefault(player.getUniqueId(), 0.0);
        }

        MessageUtils.sendMessage(sender, "&eYour current total damage: &6" +
                String.format(damageFormat, totalDamage));
        return true;
    }

    /**
     * Handles the /damagetracker top command.
     *
     * @param sender The sender of the command.
     * @return true always.
     */
    private boolean handleTopCommand(CommandSender sender) {
        int topPlayersToShow = plugin.getDefaultTopPlayersToShow();
        Map<UUID, Double> totalDamageMap = plugin.calculateTotalDamage();

        List<Map.Entry<UUID, Double>> topPlayers = plugin.getTopDamage(totalDamageMap, topPlayersToShow);
        MessageUtils.sendMessage(sender, "&eTop " + topPlayersToShow + " players with most damage:");

        for (int i = 0; i < topPlayers.size(); i++) {
            Map.Entry<UUID, Double> entry = topPlayers.get(i);
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null) {
                MessageUtils.sendMessage(sender, "&6" + (i + 1) + ". &a" +
                        player.getName() + "&e: " +
                        String.format(damageFormat, entry.getValue()));
            }
        }
        return true;
    }

    /**
     * Handles tab completion for the /damagetracker command.
     *
     * @param sender The sender of the command.
     * @param command The command that was executed.
     * @param alias The alias of the command that was used.
     * @param args The arguments passed to the command.
     * @return A list of possible completions for the last argument.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("damagetracker")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("reload");
            completions.add("damage");
            completions.add("top");
            completions.add("check");
            completions.add("checktop");
            if (sender.hasPermission("damagetracker.cleardata")) {
                completions.add("clear");
            }
            return completions.stream()
                    .filter(c -> c.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("check") ||
                args[0].equalsIgnoreCase("checktop") ||
                args[0].equalsIgnoreCase("clear"))) {
            return trackedBossCommands.onTabComplete(args);
        }

        return new ArrayList<>();
    }
}