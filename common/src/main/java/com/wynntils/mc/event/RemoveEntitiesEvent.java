/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.neoforged.bus.api.Event;

public class RemoveEntitiesEvent extends Event {
    private final List<Integer> entityIds;

    public RemoveEntitiesEvent(ClientboundRemoveEntitiesPacket packet) {
        this.entityIds = packet.getEntityIds();
    }

    public List<Integer> getEntityIds() {
        return entityIds;
    }
}
