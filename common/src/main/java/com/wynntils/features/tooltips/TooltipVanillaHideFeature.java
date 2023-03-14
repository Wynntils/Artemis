/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.tooltips;

import com.wynntils.core.config.Category;
import com.wynntils.core.config.Config;
import com.wynntils.core.config.ConfigCategory;
import com.wynntils.core.config.RegisterConfig;
import com.wynntils.core.features.UserFeature;
import com.wynntils.mc.event.ItemTooltipFlagsEvent;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ConfigCategory(Category.TOOLTIPS)
public class TooltipVanillaHideFeature extends UserFeature {
    @RegisterConfig
    public final Config<Boolean> hideAdvanced = new Config<>(true);

    @RegisterConfig
    public final Config<Boolean> hideAdditionalnfo = new Config<>(true);

    @SubscribeEvent
    public void onTooltipFlagsAdvanced(ItemTooltipFlagsEvent.Advanced event) {
        if (!hideAdvanced.get()) return;

        event.setFlags(TooltipFlag.NORMAL);
    }

    @SubscribeEvent
    public void onTooltipFlagsMask(ItemTooltipFlagsEvent.Mask event) {
        if (!hideAdditionalnfo.get()) return;

        event.setMask(-1);
    }
}
