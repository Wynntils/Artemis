/*
 * Copyright © Wynntils 2021-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Fired on click in a container */
public class ContainerClickEvent extends Event implements ICancellableEvent {
    private final AbstractContainerMenu containerMenu;
    private final int slotNum;
    private final ClickType clickType;
    private final int mouseButton;

    public ContainerClickEvent(AbstractContainerMenu containerMenu, int slotNum, ClickType clickType, int mouseButton) {
        this.containerMenu = containerMenu;
        this.slotNum = slotNum;
        this.clickType = clickType;
        this.mouseButton = mouseButton;
    }

    public AbstractContainerMenu getContainerMenu() {
        return containerMenu;
    }

    public int getSlotNum() {
        return slotNum;
    }

    public ClickType getClickType() {
        return clickType;
    }

    public int getMouseButton() {
        return mouseButton;
    }

    public ItemStack getItemStack() {
        if (slotNum >= 0) {
            return containerMenu.getSlot(slotNum).getItem();
        } else {
            return ItemStack.EMPTY;
        }
    }
}
