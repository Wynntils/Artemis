/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import java.util.Collections;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraftforge.eventbus.api.Event;

public class RemoveEntitiesEvent extends Event {
    private final List<Integer> entityIds;

    public RemoveEntitiesEvent(ClientboundRemoveEntitiesPacket packet) {
        this.entityIds = Collections.unmodifiableList(packet.getEntityIds());
    }

    public List<Integer> getEntityIds() {
        return entityIds;
    }
}
