package com.elplatano0871.damagetracker;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import java.util.*;

public class MythicMobListeners implements Listener {
    private final DamageTracker plugin;

    public MythicMobListeners(DamageTracker plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        ActiveMob activeMob = event.getMob();
        String mobInternalName = activeMob.getMobType();
        UUID mobUniqueId = activeMob.getUniqueId();
    
        // Check if the boss configuration exists for the mob
        if (!plugin.getBossConfigs().containsKey(mobInternalName.toUpperCase())) {
            return;
        }
    
        // Get the boss configuration or use the default if not found
        BossConfig bossConfig = plugin.getBossConfigs().get(mobInternalName.toUpperCase());
        if (bossConfig == null) {
            plugin.getLogger().info("Using default configuration for boss: " + mobInternalName);
            bossConfig = plugin.getDefaultBossConfig();
        }
    
        // Check if the boss configuration or victory message is null
        if (bossConfig == null || bossConfig.victoryMessage == null) {
            plugin.getLogger().warning("No victory message configured for boss: " + mobInternalName);
            return;
        }
    
        // Get the damage map and top players for the boss
        Map<UUID, Double> bossDamageMap = plugin.getBossDamageMap(mobUniqueId);
        List<Map.Entry<UUID, Double>> topPlayers = plugin.getTopDamage(bossDamageMap, bossConfig.topPlayersToShow);
        
        // Get max health before firing event
        double maxHealth = plugin.getBossMaxHealth(mobUniqueId);
        
        // Fire the BossDamageCompletedEvent
        BossDamageCompletedEvent damageEvent = new BossDamageCompletedEvent(
            mobInternalName,
            mobUniqueId,
            new HashMap<>(bossDamageMap), // Create a copy of the damage map
            maxHealth
        );
        Bukkit.getPluginManager().callEvent(damageEvent);
    
        StringBuilder topPlayersMessage = new StringBuilder();
        double totalDamage = bossDamageMap.values().stream().mapToDouble(Double::doubleValue).sum();
    
        // Iterate over the top players up to the configured maximum or the size of the top players list
        for (int i = 0; i < Math.min(bossConfig.topPlayersToShow, topPlayers.size()); i++) {
            Map.Entry<UUID, Double> entry = topPlayers.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                // Select the appropriate format for the current player
                String format = i < bossConfig.topPlayersFormat.size() ? 
                    bossConfig.topPlayersFormat.get(i) : 
                    "<gray>{player_name}: {damage} ({percentage}%)";
                
                // Format the player's damage
                String damageStr = plugin.formatDamage(entry.getValue(), maxHealth, "numeric");
                // Calculate the player's damage percentage relative to the total damage
                double percentage = (entry.getValue() / totalDamage) * 100;
                // Format the damage percentage
                String percentageStr = String.format(plugin.percentageFormat, percentage);
                
                // Get the player's prefix and replace color codes
                String prefix = plugin.getPlayerPrefix(player);
                prefix = prefix.replaceAll("§([0-9a-fk-or])", "<$1>");
                
                // Replace placeholders in the format with actual values
                format = format.replace("{player_name}", player.getName())
                             .replace("{damage}", damageStr)
                             .replace("{percentage}", percentageStr)
                             .replace("{prefix}", prefix);
                
                // Append the formatted player information to the top players message
                topPlayersMessage.append(format).append("\n");
            }
        }
    
        // Replace placeholders in the victory message with actual values
        String message = bossConfig.victoryMessage
            .replace("{boss_name}", activeMob.getDisplayName())
            .replace("{top_players}", topPlayersMessage.toString());
    
        // Send the victory message to all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            MessageUtils.sendMessage(player, message);
        }
    
        // Handle personal messages
        List<Map.Entry<UUID, Double>> sortedDamageList = new ArrayList<>(bossDamageMap.entrySet());
        sortedDamageList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
    
        for (int i = 0; i < sortedDamageList.size(); i++) {
            Map.Entry<UUID, Double> entry = sortedDamageList.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                double damage = entry.getValue();
                double percentage = (damage / totalDamage) * 100;
                String personalMessage = plugin.getPersonalMessageFormat()
                    .replace("{position}", String.valueOf(i + 1))
                    .replace("{damage}", plugin.formatDamage(damage, maxHealth, "numeric"))
                    .replace("{percentage}", String.format(plugin.percentageFormat, percentage));
                MessageUtils.sendMessage(player, personalMessage);
            }
        }
    
        // Remove boss data after handling the death event
        plugin.removeBossData(mobUniqueId);
    }

    @EventHandler
    public void onMythicMobDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        
        LivingEntity entity = (LivingEntity) event.getEntity();
        Player damager = getPlayerDamager(event.getDamager());
        
        if (damager == null) return;

        try {
            ActiveMob activeMob = MythicBukkit.inst().getMobManager().getMythicMobInstance(entity);
            if (activeMob != null && plugin.getBossConfigs().containsKey(activeMob.getMobType().toUpperCase())) {
                UUID mobUniqueId = activeMob.getUniqueId();
                
                // Add damage to the boss damage map
                plugin.addDamage(mobUniqueId, damager, event.getFinalDamage());
                
                // Set the boss's max health if not already set
                if (!plugin.hasBossMaxHealth(mobUniqueId)) {
                    plugin.setBossMaxHealth(mobUniqueId, 
                        ((LivingEntity) activeMob.getEntity().getBukkitEntity()).getMaxHealth());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error processing damage event: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Player getPlayerDamager(Entity damager) {
        if (damager instanceof Player) {
            return (Player) damager;
        }
        
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) {
                return (Player) shooter;
            }
        }
        
        if (damager instanceof TNTPrimed) {
            Entity source = ((TNTPrimed) damager).getSource();
            if (source instanceof Player) {
                return (Player) source;
            }
        }
        
        if (damager instanceof AreaEffectCloud) {
            ProjectileSource source = ((AreaEffectCloud) damager).getSource();
            if (source instanceof Player) {
                return (Player) source;
            }
        }

        if (damager instanceof Tameable) {
            AnimalTamer owner = ((Tameable) damager).getOwner();
            if (owner instanceof Player) {
                return (Player) owner;
            }
        }

        return null;
    }
}