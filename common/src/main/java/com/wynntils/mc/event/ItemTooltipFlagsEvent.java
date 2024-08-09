/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.Event;

public abstract class ItemTooltipFlagsEvent extends Event {
    private final ItemStack itemStack;

    protected ItemTooltipFlagsEvent(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public static final class Advanced extends ItemTooltipFlagsEvent {
        private TooltipFlag flags;

        public Advanced(ItemStack itemStack, TooltipFlag flags) {
            super(itemStack);
            this.flags = flags;
        }

        public TooltipFlag getFlags() {
            return flags;
        }

        public void setFlags(TooltipFlag flags) {
            this.flags = flags;
        }
    }

    public static final class HideAdditionalTooltip extends ItemTooltipFlagsEvent {
        private boolean hideTooltip;

        public HideAdditionalTooltip(ItemStack itemStack, boolean hideTooltip) {
            super(itemStack);
            this.hideTooltip = hideTooltip;
        }

        public boolean getHideAdditionalTooltip() {
            return hideTooltip;
        }

        public void setHideAdditionalTooltip(boolean hideTooltip) {
            this.hideTooltip = hideTooltip;
        }
    }
}
