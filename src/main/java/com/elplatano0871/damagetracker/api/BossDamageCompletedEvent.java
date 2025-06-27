package com.elplatano0871.damagetracker.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public class BossDamageCompletedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final String bossId;
    private final UUID mobUuid;
    private final Map<UUID, Double> playerDamageMap;
    private final double maxHealth;

    /**
     * Constructor for the BossDamageCompletedEvent
     *
     * @param bossId          The internal ID/name of the boss
     * @param mobUuid         The UUID of the mob instance
     * @param playerDamageMap Map of player UUIDs to their damage values
     * @param maxHealth       The maximum health of the boss
     */
    public BossDamageCompletedEvent(String bossId, UUID mobUuid, Map<UUID, Double> playerDamageMap, double maxHealth) {
        this.bossId = bossId;
        this.mobUuid = mobUuid;
        this.playerDamageMap = Map.copyOf(playerDamageMap); // Create an unmodifiable copy
        this.maxHealth = maxHealth;
    }

    /**
     * Gets the internal ID/name of the boss
     *
     * @return Boss ID string
     */
    public String getBossId() {
        return bossId;
    }

    /**
     * Gets the UUID of the mob instance
     *
     * @return Mob UUID
     */
    public UUID getMobUuid() {
        return mobUuid;
    }

    /**
     * Gets an unmodifiable map of player damage
     *
     * @return Map of player UUIDs to damage values
     */
    public Map<UUID, Double> getPlayerDamageMap() {
        return playerDamageMap;
    }

    /**
     * Gets the maximum health of the boss
     *
     * @return Boss max health value
     */
    public double getMaxHealth() {
        return maxHealth;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}