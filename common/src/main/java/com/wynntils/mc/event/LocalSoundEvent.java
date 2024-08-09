/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class LocalSoundEvent extends Event implements ICancellableEvent {
    private final SoundEvent sound;

    protected LocalSoundEvent(SoundEvent sound) {
        this.sound = sound;
    }

    public SoundEvent getSound() {
        return sound;
    }

    public static final class Client extends LocalSoundEvent {
        private final SoundSource source;

        public Client(SoundEvent sound, SoundSource source) {
            super(sound);
            this.source = source;
        }

        public SoundSource getSource() {
            return source;
        }
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
