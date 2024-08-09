/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.labels.event;

import com.wynntils.core.text.StyledText;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;

/**
 * These events is rarely useful for Wynncraft after the 2.1 update.
 * Check out {@link TextDisplayChangedEvent} for the new way to handle entity labels.
 */
public abstract class EntityLabelEvent extends Event {
    private final Entity entity;

    protected EntityLabelEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    /**
     * Event fired when the label of an entity changes.
     */
    public static class Changed extends EntityLabelEvent {
        private final StyledText name;

        public Changed(Entity entity, StyledText name) {
            super(entity);
            this.name = name;
        }

        public StyledText getName() {
            return name;
        }
    }

    /**
     * Event fired when the visibility of an entity label changes.
     */
    public static class Visibility extends EntityLabelEvent {
        private final boolean value;

        public Visibility(Entity entity, boolean value) {
            super(entity);
            this.value = value;
        }

        public boolean getVisibility() {
            return value;
        }
    }
}
