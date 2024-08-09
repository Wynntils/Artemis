/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.neoforged.bus.api.Event;

public final class LoadingProgressEvent extends Event {
    private final String message;

    public LoadingProgressEvent(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
