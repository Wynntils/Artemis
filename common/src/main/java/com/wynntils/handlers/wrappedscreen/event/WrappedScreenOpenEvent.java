/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.wrappedscreen.event;

import com.wynntils.handlers.wrappedscreen.WrappedScreen;
import net.neoforged.bus.api.Event;

/**
 * An event that is fired when a wrapped screen is opened.
 * Consumers can listen to this event to allow opening the wrapped screen.
 * If the event is explicitly accepted, the wrapped screen will not be opened.
 */
public class WrappedScreenOpenEvent extends Event {
    private final Class<? extends WrappedScreen> wrappedScreenClass;

    private boolean openScreen = false;

    public WrappedScreenOpenEvent(Class<? extends WrappedScreen> wrappedScreenClass) {
        this.wrappedScreenClass = wrappedScreenClass;
    }

    public Class<? extends WrappedScreen> getWrappedScreenClass() {
        return wrappedScreenClass;
    }

    public void setOpenScreen(boolean openScreen) {
        this.openScreen = openScreen;
    }

    public boolean shouldOpenScreen() {
        return openScreen;
    }
}
