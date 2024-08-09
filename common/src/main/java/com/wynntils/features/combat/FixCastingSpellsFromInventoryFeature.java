/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.ArmSwingEvent;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * This feature fixes an issue where a Archers holding bows may start
 * casting spells just by dropping items from the inventory screen.
 */
@ConfigCategory(Category.COMBAT)
public class FixCastingSpellsFromInventoryFeature extends Feature {
    @SubscribeEvent
    public void onSetSlot(ArmSwingEvent event) {
        if (event.getActionContext() == ArmSwingEvent.ArmSwingContext.DROP_ITEM_FROM_INVENTORY_SCREEN
                && event.getHand() == InteractionHand.MAIN_HAND) {
            event.setCanceled(true);
        }
    }
}
