/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.character.event;

import com.wynntils.utils.mc.type.Location;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class CharacterDeathEvent extends Event implements ICancellableEvent {
    private final Location location;

    public CharacterDeathEvent(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
