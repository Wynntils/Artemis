/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.hades.event;

import com.wynntils.services.hades.HadesUser;
import net.neoforged.bus.api.Event;

// Fired when a new user is added to the HadesModel in our current world
public class HadesUserAddedEvent extends Event {
    private final HadesUser hadesUser;

    public HadesUserAddedEvent(HadesUser hadesUser) {
        this.hadesUser = hadesUser;
    }

    public HadesUser getHadesUser() {
        return hadesUser;
    }
}
