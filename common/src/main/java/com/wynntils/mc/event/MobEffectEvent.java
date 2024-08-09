/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;

public abstract class MobEffectEvent extends Event {
    private final Entity entity;
    private final MobEffect effect;

    protected MobEffectEvent(Entity entity, MobEffect effect) {
        this.entity = entity;
        this.effect = effect;
    }

    public Entity getEntity() {
        return entity;
    }

    public MobEffect getEffect() {
        return effect;
    }

    public static final class Update extends MobEffectEvent {
        private final int effectAmplifier;
        private final int effectDurationTicks;

        public Update(Entity entity, MobEffect effect, int effectAmplifier, int effectDurationTicks) {
            super(entity, effect);
            this.effectAmplifier = effectAmplifier;
            this.effectDurationTicks = effectDurationTicks;
        }

        public int getEffectAmplifier() {
            return effectAmplifier;
        }

        public int getEffectDurationTicks() {
            return effectDurationTicks;
        }
    }

    public static final class Remove extends MobEffectEvent {
        public Remove(Entity entity, MobEffect effect) {
            super(entity, effect);
        }
    }
}
