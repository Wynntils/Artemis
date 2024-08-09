/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.core.events;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.utils.mc.McUtils;
import net.neoforged.bus.api.Event;

public final class MixinHelper {
    public static boolean onWynncraft() {
        return Managers.Connection.onServer();
    }

    public static void post(Event event) {
        if (!onWynncraft()) return;
        if (McUtils.player() == null) return;

        WynntilsMod.postEvent(event);
    }

    /**
     * Post event without checking if we are connected to a Wynncraft server
     */
    public static void postAlways(Event event) {
        WynntilsMod.postEvent(event);
    }
}
