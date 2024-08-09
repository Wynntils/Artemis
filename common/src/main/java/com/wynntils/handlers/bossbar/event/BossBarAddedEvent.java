/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.bossbar.event;

import com.wynntils.handlers.bossbar.TrackedBar;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class BossBarAddedEvent extends Event implements ICancellableEvent {
    private final TrackedBar trackedBar;

    public BossBarAddedEvent(TrackedBar trackedBar) {
        this.trackedBar = trackedBar;
    }

    public TrackedBar getTrackedBar() {
        return trackedBar;
    }
}
