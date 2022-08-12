/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.world.item.PotionItem;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class DrawPotionGlintEvent extends Event {
    private final PotionItem item;

    public DrawPotionGlintEvent(PotionItem item) {
        this.item = item;
    }

    public PotionItem getItem() {
        return item;
    }
}
