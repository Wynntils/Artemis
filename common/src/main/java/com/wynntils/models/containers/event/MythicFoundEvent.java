/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.containers.event;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public class MythicFoundEvent extends Event {
    private final ItemStack mythicBoxItem;
    private final boolean lootrunEndReward;

    public MythicFoundEvent(ItemStack mythicBoxItem, boolean lootrunEndReward) {
        this.mythicBoxItem = mythicBoxItem;
        this.lootrunEndReward = lootrunEndReward;
    }

    public ItemStack getMythicBoxItem() {
        return mythicBoxItem;
    }

    public boolean isLootrunEndReward() {
        return lootrunEndReward;
    }
}
