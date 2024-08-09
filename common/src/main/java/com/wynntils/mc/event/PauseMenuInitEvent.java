/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.client.gui.screens.PauseScreen;
import net.neoforged.bus.api.Event;

/** Fired on initialization of {@link PauseScreen} */
public final class PauseMenuInitEvent extends Event {
    private final PauseScreen pauseScreen;

    public PauseMenuInitEvent(PauseScreen pauseScreen) {
        this.pauseScreen = pauseScreen;
    }

    public PauseScreen getPauseScreen() {
        return pauseScreen;
    }
}
