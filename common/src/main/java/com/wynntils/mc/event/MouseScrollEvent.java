/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class MouseScrollEvent extends Event implements ICancellableEvent {
    private final double windowPointer;
    private final double xOffset;
    private final double yOffset;

    public MouseScrollEvent(double windowPointer, double xOffset, double yOffset) {
        this.windowPointer = windowPointer;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public boolean isScrollingUp() {
        return yOffset > 0;
    }
}
