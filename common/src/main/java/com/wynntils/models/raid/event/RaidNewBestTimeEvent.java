/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.raid.event;

import com.wynntils.models.raid.type.RaidKind;
import net.neoforged.bus.api.Event;

public class RaidNewBestTimeEvent extends Event {
    private final RaidKind raidKind;
    private final long time;

    public RaidNewBestTimeEvent(RaidKind raidKind, long time) {
        this.raidKind = raidKind;
        this.time = time;
    }

    public RaidKind getRaid() {
        return raidKind;
    }

    public long getTime() {
        return time;
    }
}
