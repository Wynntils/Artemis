/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.inventory;

import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.mc.event.DrawPotionGlintEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(category = "Inventory")
public class HidePotionGlintFeature extends UserFeature {
    @SubscribeEvent
    public void onPotionGlint(DrawPotionGlintEvent e) {
        e.setCanceled(true);
    }
}
