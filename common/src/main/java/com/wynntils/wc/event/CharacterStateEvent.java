/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wc.event;

import net.minecraftforge.eventbus.api.Event;

public class CharacterStateEvent extends Event {

    public static class ManaUpdateEvent extends CharacterStateEvent {
        int newMana;
        int oldMana;

        public ManaUpdateEvent(int newMana, int oldMana) {
            this.newMana = newMana;
            this.oldMana = oldMana;
        }

        public int getNewMana() {
            return newMana;
        }

        public int getOldMana() {
            return oldMana;
        }
    }

    public static class HealthUpdateEvent extends CharacterStateEvent {
        int newHealth;
        int oldHealth;

        public HealthUpdateEvent(int newHealth, int oldHealth) {
            this.newHealth = newHealth;
            this.oldHealth = oldHealth;
        }

        public int getNewHealth() {
            return newHealth;
        }

        public int getOldHealth() {
            return oldHealth;
        }
    }

    public static class MaxHealthUpdateEvent extends CharacterStateEvent {
        int newMaxHealth;
        int oldMaxHealth;

        public MaxHealthUpdateEvent(int newMaxHealth, int oldMaxHealth) {
            this.newMaxHealth = newMaxHealth;
            this.oldMaxHealth = oldMaxHealth;
        }

        public int getNewMaxHealth() {
            return newMaxHealth;
        }

        public int getOldMaxHealth() {
            return oldMaxHealth;
        }
    }
}
