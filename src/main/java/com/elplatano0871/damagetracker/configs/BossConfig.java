package com.elplatano0871.damagetracker.configs;

import java.util.Objects;

public class BossConfig {
    private String victoryMessageId;
    private String positionFormatId;
    private String personalMessageId;
    private String nonParticipantMessageId;
    private int topPlayersToShow;
    private boolean broadcastMessage;
    private String hologramType; 

    /**
     * Constructor for BossConfig with all parameters
     *
     * @param victoryMessageId The ID of the victory message to use
     * @param positionFormatId The ID of the position format to use
     * @param personalMessageId The ID of the personal message to use
     * @param nonParticipantMessageId The ID of the non-participant message to use
     * @param topPlayersToShow Number of top players to display in the ranking
     * @param broadcastMessage Whether to broadcast the message to all players
     */
    public BossConfig(String victoryMessageId, String positionFormatId, String personalMessageId,
                      String nonParticipantMessageId, int topPlayersToShow, boolean broadcastMessage, String hologramType) {
        this.victoryMessageId = victoryMessageId;
        this.positionFormatId = positionFormatId;
        this.personalMessageId = personalMessageId;
        this.nonParticipantMessageId = nonParticipantMessageId;
        this.topPlayersToShow = topPlayersToShow;
        this.broadcastMessage = broadcastMessage;
        this.hologramType = hologramType;
    }

    /**
     * Default constructor with default values
     */
    public BossConfig() {
        this.victoryMessageId = "DEFAULT_VICTORY";
        this.positionFormatId = "DEFAULT";
        this.personalMessageId = "DEFAULT";
        this.nonParticipantMessageId = "DEFAULT";
        this.topPlayersToShow = 3;
        this.broadcastMessage = true;
        this.hologramType = "NONE";
    }

    /**
     * Gets whether to broadcast the message to all players
     *
     * @return true if message should be broadcast, false if only for participants
     */
    public boolean isBroadcastMessage() {
        return broadcastMessage;
    }

    /**
     * Sets whether to broadcast the message to all players
     *
     * @param broadcastMessage true to broadcast, false for participants only
     */
    public void setBroadcastMessage(boolean broadcastMessage) {
        this.broadcastMessage = broadcastMessage;
    }

    /**
     * Gets the ID of the personal message format
     *
     * @return Personal message ID
     */
    public String getPersonalMessageId() {
        return personalMessageId;
    }

    /**
     * Sets the ID of the personal message format
     *
     * @param personalMessageId New personal message ID
     */
    public void setPersonalMessageId(String personalMessageId) {
        this.personalMessageId = personalMessageId;
    }

    /**
     * Gets the ID of the message for non-participants
     *
     * @return Non-participant message ID
     */
    public String getNonParticipantMessageId() {
        return nonParticipantMessageId;
    }

    /**
     * Sets the ID of the message for non-participants
     *
     * @param nonParticipantMessageId New non-participant message ID
     */
    public void setNonParticipantMessageId(String nonParticipantMessageId) {
        this.nonParticipantMessageId = nonParticipantMessageId;
    }

    /**
     * Gets the victory message ID
     *
     * @return Victory message ID
     */
    public String getVictoryMessageId() {
        return victoryMessageId;
    }

    /**
     * Sets the victory message ID
     *
     * @param victoryMessageId New victory message ID
     */
    public void setVictoryMessageId(String victoryMessageId) {
        this.victoryMessageId = victoryMessageId;
    }

    /**
     * Gets the number of top players to show
     *
     * @return Number of top players
     */
    public int getTopPlayersToShow() {
        return topPlayersToShow;
    }

    /**
     * Sets the number of top players to show
     *
     * @param topPlayersToShow New number of top players
     */
    public void setTopPlayersToShow(int topPlayersToShow) {
        this.topPlayersToShow = Math.max(1, topPlayersToShow); // Ensure at least 1 player is shown
    }

    /**
     * Gets the position format ID
     *
     * @return Position format ID
     */
    public String getPositionFormatId() {
        return positionFormatId;
    }

    /**
     * Sets the position format ID
     *
     * @param positionFormatId New position format ID
     */
    public void setPositionFormatId(String positionFormatId) {
        this.positionFormatId = positionFormatId;
    }

    /**
     * Gets the hologram type
     *
     * @return Hologram type (NONE, FANCY)
     */
    public String getHologramType() {
        return hologramType;
    }

    /**
     * Sets the hologram type
     *
     * @param hologramType New hologram type
     */
    public void setHologramType(String hologramType) {
        this.hologramType = hologramType;
    }

    /**
     * Validates and fixes the configuration
     * Ensures all required fields have at least default values
     */
    public void validate() {
        if (victoryMessageId == null || victoryMessageId.trim().isEmpty()) {
            victoryMessageId = "DEFAULT_VICTORY";
        }

        if (positionFormatId == null || positionFormatId.trim().isEmpty()) {
            positionFormatId = "DEFAULT";
        }

        if (personalMessageId == null || personalMessageId.trim().isEmpty()) {
            personalMessageId = "DEFAULT";
        }

        if (nonParticipantMessageId == null || nonParticipantMessageId.trim().isEmpty()) {
            nonParticipantMessageId = "DEFAULT";
        }

        if (topPlayersToShow < 1) {
            topPlayersToShow = 3;
        }
        
        if (hologramType == null || hologramType.trim().isEmpty()) {
            hologramType = "NONE";
        }
        
        // Validate hologram type
        if (!hologramType.equals("NONE") && !hologramType.equals("FANCY")) {
            hologramType = "NONE";
        }
    }

    /**
     * Creates a copy of this BossConfig
     */
    public BossConfig copy() {
        return new BossConfig(
                this.victoryMessageId,
                this.positionFormatId,
                this.personalMessageId,
                this.nonParticipantMessageId,
                this.topPlayersToShow,
                this.broadcastMessage,
                this.hologramType
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BossConfig that = (BossConfig) o;
        return topPlayersToShow == that.topPlayersToShow &&
                broadcastMessage == that.broadcastMessage &&
                Objects.equals(victoryMessageId, that.victoryMessageId) &&
                Objects.equals(positionFormatId, that.positionFormatId) &&
                Objects.equals(personalMessageId, that.personalMessageId) &&
                Objects.equals(nonParticipantMessageId, that.nonParticipantMessageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(victoryMessageId, positionFormatId, personalMessageId,
                nonParticipantMessageId, topPlayersToShow, broadcastMessage);
    }

    @Override
    public String toString() {
        return "BossConfig{" +
                "victoryMessageId='" + victoryMessageId + '\'' +
                ", positionFormatId='" + positionFormatId + '\'' +
                ", personalMessageId='" + personalMessageId + '\'' +
                ", nonParticipantMessageId='" + nonParticipantMessageId + '\'' +
                ", topPlayersToShow=" + topPlayersToShow +
                ", broadcastMessage=" + broadcastMessage +
                '}';
    }
}