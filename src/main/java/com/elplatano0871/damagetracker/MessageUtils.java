package com.elplatano0871.damagetracker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtils {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static BukkitAudiences adventure;

    public static void init(DamageTracker plugin) {
        adventure = BukkitAudiences.create(plugin);
    }

    public static void close() {
        if (adventure != null) {
            adventure.close();
            adventure = null;
        }
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;

        if (sender instanceof Player && adventure != null) {
            adventure.sender(sender).sendMessage(deserialize(message));
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    public static Component deserialize(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        try {
            String convertedMessage = convertLegacyAndHexToMiniMessage(message);
            return miniMessage.deserialize(convertedMessage);
        } catch (Exception e) {
            return Component.text(message);
        }
    }

    public static String convertLegacyAndHexToMiniMessage(String input) {
        if (input == null) return null;
        String result = ChatColor.translateAlternateColorCodes('&', input);
        result = result.replaceAll("&#([A-Fa-f0-9]{6})", "<#$1>");
        return convertLegacyToMiniMessage(result);
    }

    private static String convertLegacyToMiniMessage(String input) {
        return input.replace("§0", "<black>")
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
    }
}