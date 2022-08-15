/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.event;

import com.wynntils.wynn.model.WorldStateManager.State;
import net.minecraftforge.eventbus.api.Event;

public class WorldStateEvent extends Event {
    private final State newState;
    private final State oldState;
    private final String worldName;

    public WorldStateEvent(State newState, State oldState, String worldName) {
        this.newState = newState;
        this.oldState = oldState;
        this.worldName = worldName;
    }

    public State getNewState() {
        return newState;
    }

    public State getOldState() {
        return oldState;
    }

    public String getWorldName() {
        return worldName;
    }
}
