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

        // First check if the boss is being tracked
        boolean isTrackedBoss = plugin.getTrackedBossManager().isTrackedBoss(mobInternalName.toUpperCase());

        // If the boss is not being tracked, ignore it completely
        if (!isTrackedBoss) {
            return;
        }

        // Get the damage map and calculate event data
        Map<UUID, Double> bossDamageMap = plugin.getDamageManager().getTrackedBossDamageMap(mobInternalName.toUpperCase());
        double maxHealth = plugin.getDamageManager().getTrackedBossMaxHealth(mobInternalName.toUpperCase());

        // Fire the BossDamageCompletedEvent
        BossDamageCompletedEvent damageEvent = new BossDamageCompletedEvent(
                mobInternalName,
                mobUniqueId,
                new HashMap<>(bossDamageMap),
                maxHealth
        );
        Bukkit.getPluginManager().callEvent(damageEvent);

        // Check if the tracked boss has a message configuration
        boolean hasMessageConfig = plugin.getBossConfigs().containsKey(mobInternalName.toUpperCase());

        // Only process victory message if the boss has a message configuration
        if (hasMessageConfig) {
            processVictoryMessage(mobInternalName, mobUniqueId, activeMob, bossDamageMap, maxHealth);
        }

        // Schedule data cleanup
        plugin.getTrackedBossManager().scheduleDataCleanup(mobInternalName.toUpperCase());
    }

    private void processVictoryMessage(String mobInternalName, UUID mobUniqueId, ActiveMob activeMob,
                                       Map<UUID, Double> bossDamageMap, double maxHealth) {
        // Get the boss configuration or use the default if not found
        BossConfig bossConfig = plugin.getBossConfigs().get(mobInternalName.toUpperCase());
        if (bossConfig == null) {
            plugin.getLogger().info("Using default configuration for boss: " + mobInternalName);
            bossConfig = plugin.getDefaultBossConfig();
        }

        // Check if the boss configuration is valid
        if (bossConfig == null) {
            plugin.getLogger().warning("No configuration found for boss: " + mobInternalName);
            return;
        }

        VictoryMessageManager messageManager = plugin.getVictoryMessageManager();

        // Get victory message template
        String victoryMessage = messageManager.getVictoryMessage(bossConfig.getVictoryMessageId());
        if (victoryMessage == null) {
            plugin.getLogger().warning("No victory message found for ID: " + bossConfig.getVictoryMessageId());
            return;
        }

        // Get position formats
        List<String> positionFormats = messageManager.getPositionFormat(bossConfig.getPositionFormatId());

        // Get top players and prepare messages
        List<Map.Entry<UUID, Double>> topPlayers = plugin.getTopDamage(bossDamageMap, bossConfig.getTopPlayersToShow());
        double totalDamage = bossDamageMap.values().stream().mapToDouble(Double::doubleValue).sum();

        // Create the top players message
        StringBuilder topPlayersMessage = new StringBuilder();
        for (int i = 0; i < Math.min(bossConfig.getTopPlayersToShow(), topPlayers.size()); i++) {
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

                topPlayersMessage.append(format).append("\n");
            }
        }

        // Sort all players by damage for position calculation
        List<Map.Entry<UUID, Double>> sortedDamageList = new ArrayList<>(bossDamageMap.entrySet());
        sortedDamageList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        // Send messages based on broadcast configuration
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (bossConfig.isBroadcastMessage() || bossDamageMap.containsKey(player.getUniqueId())) {
                // Create personalized victory message for each player
                String personalizedMessage = victoryMessage
                        .replace("{boss_name}", activeMob.getDisplayName())
                        .replace("{top_players}", topPlayersMessage.toString());

                // Only process personal damage if the placeholder exists
                if (personalizedMessage.contains("{personal_damage}")) {
                    String personalDamageMessage;
                    if (bossDamageMap.containsKey(player.getUniqueId())) {
                        // Player participated in the fight
                        double playerDamage = bossDamageMap.get(player.getUniqueId());
                        double percentage = (playerDamage / totalDamage) * 100;
                        int position = sortedDamageList.indexOf(sortedDamageList.stream()
                                .filter(e -> e.getKey().equals(player.getUniqueId()))
                                .findFirst()
                                .orElse(null)) + 1;

                        String personalMessageTemplate = messageManager.getPersonalMessage(bossConfig.getPersonalMessageId());
                        personalDamageMessage = personalMessageTemplate
                                .replace("{position}", String.valueOf(position))
                                .replace("{damage}", plugin.formatDamage(playerDamage, maxHealth, "numeric"))
                                .replace("{percentage}", String.format(plugin.percentageFormat, percentage));
                    } else {
                        // Player didn't participate
                        personalDamageMessage = messageManager.getNonParticipantMessage(
                                bossConfig.getNonParticipantMessageId());
                    }
                    personalizedMessage = personalizedMessage.replace("{personal_damage}", personalDamageMessage);
                }

                MessageUtils.sendMessage(player, personalizedMessage);
            }
        }
    }

    @EventHandler
    public void onMythicMobDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity entity = (LivingEntity) event.getEntity();
        Player damager = getPlayerDamager(event.getDamager());

        if (damager == null) return;

        try {
            ActiveMob activeMob = MythicBukkit.inst().getMobManager().getMythicMobInstance(entity);
            if (activeMob == null) return;

            String mobInternalName = activeMob.getMobType();

            // Only track damage if the boss is in tracked_bosses.yml
            if (!plugin.getTrackedBossManager().isTrackedBoss(mobInternalName.toUpperCase())) {
                return;
            }

            double maxHealth = ((LivingEntity) activeMob.getEntity().getBukkitEntity()).getMaxHealth();

            // Add damage to tracked boss
            plugin.getTrackedBossManager().addDamage(mobInternalName.toUpperCase(), damager, event.getFinalDamage());
            // Set max health if not already set
            plugin.getTrackedBossManager().setBossMaxHealth(mobInternalName.toUpperCase(), maxHealth);

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