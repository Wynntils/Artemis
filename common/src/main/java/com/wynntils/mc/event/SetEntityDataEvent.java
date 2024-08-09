/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.neoforged.bus.api.Event;

public class SetEntityDataEvent extends Event {
    private final int id;
    private final List<SynchedEntityData.DataValue<?>> packedItems;

    public SetEntityDataEvent(ClientboundSetEntityDataPacket packet) {
        this.id = packet.id();
        this.packedItems = new ArrayList<>(packet.packedItems());
    }

    public int getId() {
        return id;
    }

    public List<SynchedEntityData.DataValue<?>> getPackedItems() {
        return Collections.unmodifiableList(packedItems);
    }

    public void addPackedItem(SynchedEntityData.DataValue<?> packedItem) {
        packedItems.add(packedItem);
    }

    public void removePackedItem(SynchedEntityData.DataValue<?> packedItem) {
        packedItems.remove(packedItem);
    }
}
