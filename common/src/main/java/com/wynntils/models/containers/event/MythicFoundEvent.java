/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.containers.event;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

public class MythicFoundEvent extends Event {
    private final ItemStack mythicBoxItem;

    public MythicFoundEvent(ItemStack mythicBoxItem) {
        this.mythicBoxItem = mythicBoxItem;
    }

    public ItemStack getMythicBoxItem() {
        return mythicBoxItem;
    }
}
