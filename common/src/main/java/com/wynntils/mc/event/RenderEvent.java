/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public abstract class RenderEvent extends Event {
    private final ElementType type;

    public RenderEvent(ElementType type) {
        this.type = type;
    }

    public ElementType getType() {
        return type;
    }

    public enum ElementType {}

    @Cancelable
    public static class Pre extends RenderEvent {
        public Pre(ElementType type) {
            super(type);
        }
    }

    public static class Post extends RenderEvent {
        public Post(ElementType type) {
            super(type);
        }
    }
}
