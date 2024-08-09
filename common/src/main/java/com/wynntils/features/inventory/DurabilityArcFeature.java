/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.inventory;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.HotbarSlotRenderEvent;
import com.wynntils.mc.event.SlotRenderEvent;
import com.wynntils.models.items.properties.DurableItemProperty;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.type.CappedValue;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.INVENTORY)
public class DurabilityArcFeature extends Feature {
    @Persisted
    public final Config<Boolean> renderDurabilityArcInventories = new Config<>(true);

    @Persisted
    public final Config<Boolean> renderDurabilityArcHotbar = new Config<>(true);

    @SubscribeEvent
    public void onRenderHotbarSlot(HotbarSlotRenderEvent.Pre e) {
        if (!renderDurabilityArcHotbar.get()) return;
        drawDurabilityArc(e.getPoseStack(), e.getItemStack(), e.getX(), e.getY(), true);
    }

    @SubscribeEvent
    public void onRenderSlot(SlotRenderEvent.Pre e) {
        if (!renderDurabilityArcInventories.get()) return;
        drawDurabilityArc(e.getPoseStack(), e.getSlot().getItem(), e.getSlot().x, e.getSlot().y, false);
    }

    private void drawDurabilityArc(PoseStack poseStack, ItemStack itemStack, int slotX, int slotY, boolean hotbar) {
        Optional<DurableItemProperty> durableItemOpt =
                Models.Item.asWynnItemProperty(itemStack, DurableItemProperty.class);
        if (durableItemOpt.isEmpty()) return;

        CappedValue durability = durableItemOpt.get().getDurability();

        // calculate color of arc
        float durabilityFraction = (float) durability.current() / durability.max();
        int colorInt = Mth.hsvToRgb(Math.max(0f, durabilityFraction) / 3f, 1f, 1f);
        CustomColor color = CustomColor.fromInt(colorInt).withAlpha(160);

        // draw
        RenderUtils.drawArc(poseStack, color, slotX, slotY, hotbar ? 0 : 200, durabilityFraction, 6, 8);
    }
}
