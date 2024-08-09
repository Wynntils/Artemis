/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.item.event;

import com.wynntils.core.text.StyledText;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class ItemRenamedEvent extends Event implements ICancellableEvent {
    private final ItemStack itemStack;
    private final StyledText oldName;
    private final StyledText newName;

    public ItemRenamedEvent(ItemStack itemStack, StyledText oldName, StyledText newName) {
        this.itemStack = itemStack;
        this.oldName = oldName;
        this.newName = newName;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public StyledText getOldName() {
        return oldName;
    }

    public StyledText getNewName() {
        return newName;
    }
}
