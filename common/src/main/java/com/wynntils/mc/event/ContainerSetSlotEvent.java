/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.wynntils.core.events.WynntilsEvent;
import net.minecraft.world.item.ItemStack;

public class ContainerSetSlotEvent extends WynntilsEvent {
    private final int containerId;
    private final int stateId;
    private final int slot;
    private final ItemStack itemStack;

    public ContainerSetSlotEvent(int containerId, int stateId, int slot, ItemStack itemStack) {
        this.containerId = containerId;
        this.stateId = stateId;
        this.slot = slot;
        this.itemStack = itemStack;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getContainerId() {
        return containerId;
    }

    public int getSlot() {
        return slot;
    }

    public int getStateId() {
        return stateId;
    }
}
