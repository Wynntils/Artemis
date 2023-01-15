/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.events.WynntilsEvent;
import net.minecraft.world.item.ItemStack;

public class GroundItemEntityTransformEvent extends WynntilsEvent {

    private final PoseStack poseStack;
    private final ItemStack itemStack;

    public GroundItemEntityTransformEvent(PoseStack poseStack, ItemStack itemStack) {
        this.poseStack = poseStack;
        this.itemStack = itemStack;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
