package com.elplatano0871.damagetracker;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

/**
 * Handles commands related to tracked bosses.
 */
public class TrackedBossCommands {
    private final DamageTracker plugin;
    private final TrackedBossManager trackedBossManager;

    /**
     * Constructor for TrackedBossCommands.
     * @param plugin The instance of the DamageTracker plugin.
     * @param trackedBossManager The manager for tracked bosses.
     */
    public TrackedBossCommands(DamageTracker plugin, TrackedBossManager trackedBossManager) {
        this.plugin = plugin;
        this.trackedBossManager = trackedBossManager;
    }

    /**
     * Handles the command to check damage dealt to a boss.
     * @param sender The command sender.
     * @param args The command arguments.
     * @return True if the command was handled, false otherwise.
     */
    public boolean handleCheckDamageCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtils.sendMessage(sender, "&cThis command can only be used by players.");
            return true;
        }

        if (!sender.hasPermission("damagetracker.check")) {
            MessageUtils.sendMessage(sender, "&cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "&cUsage: /damagetracker check <bossId>");
            return true;
        }

        String bossId = args[1].toUpperCase();
        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        if (!trackedBossManager.isTrackedBoss(bossId)) {
            MessageUtils.sendMessage(sender, "&cThat boss is not being tracked.");
            return true;
        }

        double damage = trackedBossManager.getPlayerDamage(bossId, playerId);
        double percentage = trackedBossManager.getPlayerDamagePercentage(bossId, playerId);

        String message = plugin.getConfig().getString("tracked_boss_messages.check_damage.format",
                        "&6Damage to {boss_name}: &e{damage} &7(&e{percentage}%&7)")
                .replace("{boss_name}", bossId)
                .replace("{damage}", trackedBossManager.formatDamage(damage, bossId))
                .replace("{percentage}", String.format("%.1f", percentage));

        MessageUtils.sendMessage(sender, message);
        return true;
    }

    /**
     * Handles the command to check the top damage dealt to a boss.
     * @param sender The command sender.
     * @param args The command arguments.
     * @return True if the command was handled, false otherwise.
     */
    public boolean handleCheckTopCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("damagetracker.checktop")) {
            MessageUtils.sendMessage(sender, "&cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "&cUsage: /damagetracker top <bossId>");
            return true;
        }

        String bossId = args[1].toUpperCase();
        if (!trackedBossManager.isTrackedBoss(bossId)) {
            MessageUtils.sendMessage(sender, "&cThat boss is not being tracked.");
            return true;
        }

        List<Map.Entry<UUID, Double>> topDamage = trackedBossManager.getTopDamage(bossId, 10);
        if (topDamage.isEmpty()) {
            String noDataMessage = plugin.getConfig().getString("tracked_boss_messages.check_top.no_data",
                    "&cNo top data available for this boss.");
            MessageUtils.sendMessage(sender, noDataMessage);
            return true;
        }

        String header = plugin.getConfig().getString("tracked_boss_messages.check_top.header",
                        "&6=== Top Damage: {boss_name} ===")
                .replace("{boss_name}", bossId);
        MessageUtils.sendMessage(sender, header);

        String entryFormat = plugin.getConfig().getString("tracked_boss_messages.check_top.entry",
                "&e#{position}. {player_name}: &f{damage} &7({percentage}%)");

        for (int i = 0; i < topDamage.size(); i++) {
            Map.Entry<UUID, Double> entry = topDamage.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                String message = entryFormat
                        .replace("{position}", String.valueOf(i + 1))
                        .replace("{player_name}", player.getName())
                        .replace("{damage}", trackedBossManager.formatDamage(entry.getValue(), bossId))
                        .replace("{percentage}",
                                String.format("%.1f", trackedBossManager.getPlayerDamagePercentage(bossId, entry.getKey())));
                MessageUtils.sendMessage(sender, message);
            }
        }

        String footer = plugin.getConfig().getString("tracked_boss_messages.check_top.footer",
                "&6=========================");
        MessageUtils.sendMessage(sender, footer);
        return true;
    }

    /**
     * Handles the command to clear damage data for a boss.
     * @param sender The command sender.
     * @param args The command arguments.
     * @return True if the command was handled, false otherwise.
     */
    public boolean handleClearDataCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("damagetracker.cleardata")) {
            MessageUtils.sendMessage(sender, "&cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 2) {
            MessageUtils.sendMessage(sender, "&cUsage: /damagetracker clear <bossId>");
            return true;
        }

        String bossId = args[1].toUpperCase();
        if (!trackedBossManager.isTrackedBoss(bossId)) {
            MessageUtils.sendMessage(sender, "&cThat boss is not being tracked.");
            return true;
        }

        if (trackedBossManager.getDamageData(bossId).isEmpty()) {
            String noDataMessage = plugin.getConfig().getString("tracked_boss_messages.clear_data.no_data",
                    "&cThere is no data to clear.");
            MessageUtils.sendMessage(sender, noDataMessage);
            return true;
        }

        trackedBossManager.clearBossData(bossId);
        String successMessage = plugin.getConfig().getString("tracked_boss_messages.clear_data.success",
                        "&aDamage data for {boss_name} has been cleared.")
                .replace("{boss_name}", bossId);
        MessageUtils.sendMessage(sender, successMessage);
        return true;
    }

    /**
     * Provides tab completion suggestions for commands.
     * @param args The command arguments.
     * @return A list of tab completion suggestions.
     */
    public List<String> onTabComplete(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            // Suggestions for subcommands check, top, and clear
            String lowercaseArg = args[1].toLowerCase();
            trackedBossManager.getTrackedBossIds().stream()
                    .filter(bossId -> bossId.toLowerCase().startsWith(lowercaseArg))
                    .forEach(completions::add);
        }

        return completions;
    }
}