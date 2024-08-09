/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class SoundPlayedEvent extends Event implements ICancellableEvent {
    private final SoundInstance soundName;

    public SoundPlayedEvent(SoundInstance soundName) {
        this.soundName = soundName;
    }

    public SoundInstance getSoundInstance() {
        return soundName;
    }
}
