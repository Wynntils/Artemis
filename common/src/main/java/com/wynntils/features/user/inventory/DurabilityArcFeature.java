/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user.inventory;

import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.FeatureInfo.Stability;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.mc.render.RenderUtils;
import com.wynntils.utils.objects.CustomColor;
import com.wynntils.wc.custom.item.WynnItemStack;
import com.wynntils.wc.custom.item.properties.DurabilityProperty;
import com.wynntils.wc.custom.item.properties.ItemProperty;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@FeatureInfo(stability = Stability.STABLE, category = "Inventory")
public class DurabilityArcFeature extends UserFeature {

    @SubscribeEvent
    public void onRenderSlot(SlotRenderEvent.Pre e) {
        ItemStack item = e.getSlot().getItem();
        if (!(item instanceof WynnItemStack wynnItem)) return;

        if (!wynnItem.hasProperty(ItemProperty.DURABILITY)) return; // no durability info
        DurabilityProperty durability = wynnItem.getProperty(ItemProperty.DURABILITY);

        // calculate color of arc
        float durabilityPercent = durability.getDurabilityPercent();
        int colorInt = Mth.hsvToRgb(Math.max(0f, durabilityPercent) / 3f, 1f, 1f);
        CustomColor color = CustomColor.fromInt(colorInt).setAlpha(160);

        // draw
        RenderUtils.drawArc(color, e.getSlot().x, e.getSlot().y, 200, durabilityPercent, 8);
    }
}
