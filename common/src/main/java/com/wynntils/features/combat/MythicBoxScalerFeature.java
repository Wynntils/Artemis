/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.GroundItemEntityTransformEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;

@ConfigCategory(Category.COMBAT)
public class MythicBoxScalerFeature extends Feature {
    @Persisted
    public final Config<Float> scale = new Config<>(1.5f);

    @SubscribeEvent
    public void onItemRendering(GroundItemEntityTransformEvent e) {
        if (!isMythicBox(e.getItemStack())) return;

        PoseStack stack = e.getPoseStack();

        // Essentially vanilla has a line that translate by 0.25 times the model scale
        // However, we want to factor in our own custom scale
        // Since this code runs after the model scale is applied, we first do
        // -0.25 to remove the original translation and then multiply our scale before
        // reapplying the transformation
        stack.translate(0f, -0.25f, 0f);

        stack.scale(scale.get(), scale.get(), scale.get());

        stack.translate(0f, 0.25f, 0f);
    }

    private boolean isMythicBox(ItemStack itemStack) {
        // Since this item is not in any container/inventory, it does not
        // have a WynnItem annotation
        return itemStack.is(Items.STONE_SHOVEL) && itemStack.getDamageValue() == 6;
    }
}
