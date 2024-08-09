/*
 * Copyright © Wynntils 2023-2024.
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
import com.wynntils.models.items.items.game.EmeraldPouchItem;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.type.CappedValue;
import java.util.Optional;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.INVENTORY)
public class EmeraldPouchFillArcFeature extends Feature {
    @Persisted
    public final Config<Boolean> renderFillArcHotbar = new Config<>(true);

    @Persisted
    public final Config<Boolean> renderFillArcInventory = new Config<>(true);

    @SubscribeEvent
    public void onRenderHotbarSlot(HotbarSlotRenderEvent.Pre e) {
        if (!renderFillArcHotbar.get()) return;
        drawFilledArc(e.getPoseStack(), e.getItemStack(), e.getX(), e.getY(), true);
    }

    @SubscribeEvent
    public void onRenderSlot(SlotRenderEvent.Pre e) {
        if (!renderFillArcInventory.get()) return;
        drawFilledArc(e.getPoseStack(), e.getSlot().getItem(), e.getSlot().x, e.getSlot().y, false);
    }

    private void drawFilledArc(PoseStack poseStack, ItemStack itemStack, int slotX, int slotY, boolean hotbar) {
        Optional<EmeraldPouchItem> optionalItem = Models.Item.asWynnItem(itemStack, EmeraldPouchItem.class);

        if (optionalItem.isEmpty()) return;

        CappedValue capacity = new CappedValue(
                optionalItem.get().getValue(), optionalItem.get().getCapacity());

        // calculate color of arc
        float capacityFraction = (float) capacity.current() / capacity.max();
        int colorInt = Mth.hsvToRgb((1 - capacityFraction) / 3f, 1f, 1f);
        CustomColor color = CustomColor.fromInt(colorInt).withAlpha(160);

        // The amount of ring to render
        float ringFraction = Math.min(1f, capacityFraction);

        // draw
        RenderUtils.drawArc(poseStack, color, slotX - 2, slotY - 2, hotbar ? 0 : 200, ringFraction, 8, 10);
    }
}
