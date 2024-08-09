/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public final class ItemCountOverlayRenderEvent extends Event {
    private final ItemStack itemStack;
    private String countString;
    private int countColor;

    public ItemCountOverlayRenderEvent(ItemStack itemStack, String countString, int countColor) {
        this.itemStack = itemStack;
        this.countString = countString;
        this.countColor = countColor;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public String getCountString() {
        return countString;
    }

    public void setCountString(String countString) {
        this.countString = countString;
    }

    public int getCountColor() {
        return countColor;
    }

    public void setCountColor(int countColor) {
        this.countColor = countColor;
    }
}
