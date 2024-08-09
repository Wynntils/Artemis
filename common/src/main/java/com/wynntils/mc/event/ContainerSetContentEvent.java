/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ContainerSetContentEvent extends Event {
    private final List<ItemStack> items;
    private final ItemStack carriedItem;
    private final int containerId;
    private final int stateId;

    protected ContainerSetContentEvent(List<ItemStack> items, ItemStack carriedItem, int containerId, int stateId) {
        this.items = items;
        this.carriedItem = carriedItem;
        this.containerId = containerId;
        this.stateId = stateId;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public ItemStack getCarriedItem() {
        return carriedItem;
    }

    public int getContainerId() {
        return containerId;
    }

    public int getStateId() {
        return stateId;
    }

    public static class Pre extends ContainerSetContentEvent implements ICancellableEvent {
        public Pre(List<ItemStack> items, ItemStack carriedItem, int containerId, int stateId) {
            super(items, carriedItem, containerId, stateId);
        }
    }

    public static class Post extends ContainerSetContentEvent {
        public Post(List<ItemStack> items, ItemStack carriedItem, int containerId, int stateId) {
            super(items, carriedItem, containerId, stateId);
        }
    }
}
