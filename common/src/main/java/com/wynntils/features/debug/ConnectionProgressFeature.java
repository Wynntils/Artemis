/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.debug;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.features.properties.StartDisabled;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.ServerResourcePackEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import com.wynntils.models.worlds.type.WorldState;
import net.neoforged.bus.api.SubscribeEvent;

@StartDisabled
@ConfigCategory(Category.DEBUG)
public class ConnectionProgressFeature extends Feature {
    @SubscribeEvent
    public void onResourcePack(ServerResourcePackEvent.Load e) {
        if (!Managers.Connection.onServer()) return;
        WynntilsMod.info("Connection confirmed");
    }

    @SubscribeEvent
    public void onStateChange(WorldStateEvent e) {
        if (e.getNewState() == WorldState.WORLD) {
            WynntilsMod.info("Entering world " + e.getWorldName());
        } else if (e.getOldState() == WorldState.WORLD) {
            WynntilsMod.info("Leaving world");
        }
        String msg =
                switch (e.getNewState()) {
                    case NOT_CONNECTED -> "Disconnected";
                    case CONNECTING -> "Connecting";
                    case CHARACTER_SELECTION -> "In character selection";
                    case HUB -> "On Hub";
                    case INTERIM -> "Between states";
                    default -> null;
                };

        if (msg != null) {
            WynntilsMod.info("WorldState change: " + msg);
        }
    }
}
