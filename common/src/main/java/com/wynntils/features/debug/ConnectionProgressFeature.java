/*
 * Copyright © Wynntils 2021.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.debug;

import com.wynntils.core.features.AbstractFeature;
import com.wynntils.mc.event.ResourcePackEvent;
import com.wynntils.wc.event.WorldStateEvent;
import com.wynntils.wc.model.WorldState.State;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ConnectionProgressFeature extends AbstractFeature {
    @SubscribeEvent
    public void onResourcePack(ResourcePackEvent e) {
        System.out.println("Connection confirmed");
    }

    @SubscribeEvent
    public void onStateChange(WorldStateEvent e) {
        if (e.getNewState() == State.WORLD) {
            System.out.println("Entering world " + e.getWorldName());
        } else if (e.getOldState() == State.WORLD) {
            System.out.println("Leaving world");
        }
        String msg =
                switch (e.getNewState()) {
                    case NOT_CONNECTED -> "Disconnected";
                    case CONNECTING -> "Connecting";
                    case CHARACTER_SELECTION -> "In character selection";
                    case HUB -> "On Hub";
                    default -> null;
                };

        if (msg != null) {
            System.out.println("WorldState change: " + msg);
        }
    }
}
