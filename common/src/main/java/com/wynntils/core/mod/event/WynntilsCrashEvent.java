/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.mod.event;

import com.wynntils.core.mod.type.CrashType;
import net.neoforged.bus.api.Event;

public class WynntilsCrashEvent extends Event {
    private final String name;
    private final CrashType type;
    private final Throwable throwable;

    public WynntilsCrashEvent(String name, CrashType type, Throwable throwable) {
        this.name = name;
        this.type = type;
        this.throwable = throwable;
    }

    public String getName() {
        return name;
    }

    public CrashType getType() {
        return type;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
