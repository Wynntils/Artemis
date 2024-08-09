/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.lootrun.event;

import com.wynntils.models.beacons.type.Beacon;
import com.wynntils.models.lootrun.type.TaskLocation;
import net.neoforged.bus.api.Event;

public class LootrunBeaconSelectedEvent extends Event {
    private final Beacon beacon;
    private final TaskLocation taskLocation;

    public LootrunBeaconSelectedEvent(Beacon beacon, TaskLocation taskLocation) {
        this.beacon = beacon;
        this.taskLocation = taskLocation;
    }

    public Beacon getBeacon() {
        return beacon;
    }

    public TaskLocation getTaskLocation() {
        return taskLocation;
    }
}
