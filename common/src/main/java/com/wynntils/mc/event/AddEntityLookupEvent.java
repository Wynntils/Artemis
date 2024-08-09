/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import java.util.Map;
import java.util.UUID;
import net.minecraft.world.level.entity.EntityAccess;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class AddEntityLookupEvent extends Event implements ICancellableEvent {
    private final UUID uuid;
    private final Map<UUID, EntityAccess> entityMap;

    public AddEntityLookupEvent(UUID uuid, Map<UUID, EntityAccess> entityMap) {
        this.uuid = uuid;
        this.entityMap = entityMap;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Map<UUID, EntityAccess> getEntityMap() {
        return entityMap;
    }
}
