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

        if (!plugin.getBossConfigs().containsKey(mobInternalName.toUpperCase())) {
            return;
        }

        BossConfig bossConfig = plugin.getBossConfigs().get(mobInternalName.toUpperCase());
        if (bossConfig == null) {
            plugin.getLogger().info("Using default configuration for boss: " + mobInternalName);
            bossConfig = plugin.getDefaultBossConfig();
        }

        if (bossConfig == null || bossConfig.victoryMessage == null) {
            plugin.getLogger().warning("No victory message configured for boss: " + mobInternalName);
            return;
        }

        Map<UUID, Double> bossDamageMap = plugin.getBossDamageMap(mobUniqueId);
        List<Map.Entry<UUID, Double>> topPlayers = plugin.getTopDamage(bossDamageMap, bossConfig.topPlayersToShow);

        StringBuilder topPlayersMessage = new StringBuilder();
        double maxHealth = plugin.getBossMaxHealth(mobUniqueId);
        double totalDamage = bossDamageMap.values().stream().mapToDouble(Double::doubleValue).sum();

        for (int i = 0; i < Math.min(bossConfig.topPlayersToShow, topPlayers.size()); i++) {
            Map.Entry<UUID, Double> entry = topPlayers.get(i);
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                String format = i < bossConfig.topPlayersFormat.size() ?
                        bossConfig.topPlayersFormat.get(i) :
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

                topPlayersMessage.append(format).append("\n");

                // Ödülleri oyunculara gönder
                List<String> rewards = bossConfig.getRewardsForPosition(i);
                for (String rewardCommand : rewards) {
                    String finalCommand = rewardCommand.replace("{player_name}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                }
            }
        }

        String message = bossConfig.victoryMessage
                .replace("{boss_name}", activeMob.getDisplayName())
                .replace("{top_players}", topPlayersMessage.toString());

        for (Player player : Bukkit.getOnlinePlayers()) {
            MessageUtils.sendMessage(player, message);
        }

        // Kişisel mesajlar
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
                
                plugin.addDamage(mobUniqueId, damager, event.getFinalDamage());
                
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