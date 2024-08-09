/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.players.event;

import net.neoforged.bus.api.Event;

/**
 * These events correspond to data from PartyModel
 */
public abstract class PartyEvent extends Event {
    /**
     * Fired upon obtaining a new party list.
     * Get the party list from the party model manually if required.
     */
    public static class Listed extends PartyEvent {}

    /**
     * Fired when someone invites you to their party
     * @field playerName the name of the player who invited your
     */
    public static class Invited extends PartyEvent {
        private final String playerName;

        public Invited(String playerName) {
            this.playerName = playerName;
        }

        public String getPlayerName() {
            return playerName;
        }
    }

    /**
     * Fired upon someone else joining the user's party
     * @field playerName the name of the player who joined
     */
    public static class OtherJoined extends PartyEvent {
        private final String playerName;

        public OtherJoined(String playerName) {
            this.playerName = playerName;
        }

        public String getPlayerName() {
            return playerName;
        }
    }

    /**
     * Fired upon someone else leaving the user's party
     * @field playerName the name of the player who left
     */
    public static class OtherLeft extends PartyEvent {
        private final String playerName;

        public OtherLeft(String playerName) {
            this.playerName = playerName;
        }

        public String getPlayerName() {
            return playerName;
        }
    }

    /**
     * Fired upon a party member disconnecting
     * @field playerName the name of the player who disconnected
     */
    public static class OtherDisconnected extends PartyEvent {
        private final String playerName;

        public OtherDisconnected(String playerName) {
            this.playerName = playerName;
        }

        public String getPlayerName() {
            return playerName;
        }
    }

    /**
     * Fired upon a party member reconnecting
     */
    public static class OtherReconnected extends PartyEvent {
        private final String playerName;

        public OtherReconnected(String playerName) {
            this.playerName = playerName;
        }

        public String getPlayerName() {
            return playerName;
        }
    }

    public static class PriorityChanged extends PartyEvent {
        private final String playerName;
        private final int priority;

        public PriorityChanged(String playerName, int priority) {
            this.playerName = playerName;
            this.priority = priority;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getPriority() {
            return priority;
        }
    }
}
