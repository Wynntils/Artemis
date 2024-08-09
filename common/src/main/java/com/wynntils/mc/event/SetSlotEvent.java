/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/** Fired when an item is set in a slot */
public abstract class SetSlotEvent extends Event {
    private final Container container;
    private final int slot;
    ItemStack itemStack;

    protected SetSlotEvent(Container container, int slot, ItemStack itemStack) {
        this.container = container;
        this.slot = slot;
        this.itemStack = itemStack;
    }

    public Container getContainer() {
        return container;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * Note: This get's called after {@link ContainerSetContentEvent.Pre} and before {@link ContainerSetContentEvent.Post}
     * This event is also commonly confused with {@link ContainerSetSlotEvent}
     * If you are experiencing "duplication" of items,
     * seeing the same items being set multiple times,
     * when listening to both of these events, consider using {@link ContainerSetContentEvent.Post}
     */
    public static class Pre extends SetSlotEvent {
        public Pre(Container container, int slot, ItemStack itemStack) {
            super(container, slot, itemStack);
        }

        public void setItemStack(ItemStack itemStack) {
            this.itemStack = itemStack;
        }
    }

    public static class Post extends SetSlotEvent {
        private final ItemStack oldItemStack;

        public Post(Container container, int slot, ItemStack newItemStack, ItemStack oldItemStack) {
            super(container, slot, newItemStack);
            this.oldItemStack = oldItemStack;
        }

        public ItemStack getOldItemStack() {
            return oldItemStack;
        }
    }
}
