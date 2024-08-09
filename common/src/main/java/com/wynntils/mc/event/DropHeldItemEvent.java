/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class DropHeldItemEvent extends Event implements ICancellableEvent {
    private final boolean fullStack;

    public DropHeldItemEvent(boolean fullStack) {
        this.fullStack = fullStack;
    }

    public boolean isFullStack() {
        return fullStack;
    }
}
