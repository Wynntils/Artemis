package com.wynntils.mc.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

public class GroundItemEntityRenderEvent extends Event {

    private final PoseStack poseStack;

    private final ItemStack itemStack;

    public GroundItemEntityRenderEvent(PoseStack poseStack, ItemStack itemStack) {
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
