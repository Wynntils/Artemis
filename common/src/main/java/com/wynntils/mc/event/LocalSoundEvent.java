/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public abstract class LocalSoundEvent extends Event {
    private final SoundEvent sound;

    protected LocalSoundEvent(SoundEvent sound) {
        this.sound = sound;
    }

    public SoundEvent getSound() {
        return sound;
    }

    public static final class Player extends LocalSoundEvent {
        public Player(SoundEvent sound) {
            super(sound);
        }
    }

    public static final class LocalEntity extends LocalSoundEvent {
        private final Entity entity;

        public LocalEntity(SoundEvent sound, Entity entity) {
            super(sound);
            this.entity = entity;
        }

        public Entity getEntity() {
            return entity;
        }
    }
}
