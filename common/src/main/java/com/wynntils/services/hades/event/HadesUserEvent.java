/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.hades.event;

import com.wynntils.services.hades.HadesUser;
import net.minecraftforge.eventbus.api.Event;

// Fired when a new user is added to the HadesModel in our current world
public abstract class HadesUserEvent extends Event {
    private final HadesUser hadesUser;

    public HadesUserEvent(HadesUser hadesUser) {
        this.hadesUser = hadesUser;
    }

    public HadesUser getHadesUser() {
        return hadesUser;
    }

    public static class Added extends HadesUserEvent {
        public Added(HadesUser hadesUser) {
            super(hadesUser);
        }
    }

    public static class Removed extends HadesUserEvent {
        public Removed(HadesUser hadesUser) {
            super(hadesUser);
        }
    }
}
