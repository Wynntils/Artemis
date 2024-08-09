/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.event;

import net.neoforged.bus.api.Event;

public class CombatXpGainEvent extends Event {
    private final float gainedXpRaw;
    private final float gainedXpPercentage;

    public CombatXpGainEvent(float gainedXpRaw, float gainedXpPercentage) {
        this.gainedXpRaw = gainedXpRaw;
        this.gainedXpPercentage = gainedXpPercentage;
    }

    public float getGainedXpPercentage() {
        return gainedXpPercentage;
    }

    public float getGainedXpRaw() {
        return gainedXpRaw;
    }
}
