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
    private static final int CHAT_WIDTH = 320; // Minecraft's chat width in pixels
    private static final int SPACE_WIDTH = 4; // Width of a space character in pixels
    
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
            // Process centered tags before MiniMessage deserialization
            convertedMessage = processCenteredTags(convertedMessage);
            return miniMessage.deserialize(convertedMessage);
        } catch (Exception e) {
            return Component.text(message);
        }
    }

    private static String processCenteredTags(String input) {
        StringBuilder result = new StringBuilder();
        int lastIndex = 0;
        
        while (true) {
            int startTag = input.indexOf("<centered>", lastIndex);
            if (startTag == -1) {
                result.append(input.substring(lastIndex));
                break;
            }
            
            int endTag = input.indexOf("</centered>", startTag);
            if (endTag == -1) {
                result.append(input.substring(lastIndex));
                break;
            }
            
            // Add text before the tag
            result.append(input.substring(lastIndex, startTag));
            
            // Get the text to be centered
            String textToCenter = input.substring(startTag + 10, endTag);
            
            // Center the text and add it
            result.append(centerText(textToCenter));
            
            lastIndex = endTag + 11; // 11 is length of "</centered>"
        }
        
        return result.toString();
    }

    private static String centerText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Calculate the width of the text (excluding color codes)
        int messageWidth = getMessageWidth(stripColor(text));
        
        // Calculate needed spaces before the message
        int spacesBefore = (CHAT_WIDTH - messageWidth) / (2 * SPACE_WIDTH);
        
        // Create the spaces prefix
        StringBuilder spaces = new StringBuilder();
        for (int i = 0; i < spacesBefore; i++) {
            spaces.append(" ");
        }
        
        return spaces + text;
    }

    private static int getMessageWidth(String message) {
        int width = 0;
        for (char c : message.toCharArray()) {
            width += getCharWidth(c);
        }
        return width;
    }

    private static int getCharWidth(char c) {
        // Simplified character width calculation
        // In a real implementation, you might want to have a more complete mapping
        if (c >= '!' && c <= '~') {
            return 6; // Most standard characters
        } else if (c == ' ') {
            return SPACE_WIDTH;
        }
        return 7; // Default for other characters
    }

    private static String stripColor(String input) {
        // Remove legacy color codes
        input = input.replaceAll("§[0-9a-fk-or]", "");
        // Remove MiniMessage tags
        input = input.replaceAll("<[^>]*>", "");
        // Remove hex color codes
        input = input.replaceAll("&#[A-Fa-f0-9]{6}", "");
        return input;
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