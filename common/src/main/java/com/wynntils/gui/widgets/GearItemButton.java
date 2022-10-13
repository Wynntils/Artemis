/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.screens.GearViewerScreen;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.item.ItemStack;

public class GearItemButton extends AbstractButton {
    private final GearViewerScreen gearViewerScreen;
    private final ItemStack itemStack;

    public GearItemButton(int x, int y, int width, int height, GearViewerScreen gearViewerScreen, ItemStack itemStack) {
        super(x, y, width, height, new TextComponent("Gear Item Button"));
        this.gearViewerScreen = gearViewerScreen;
        this.itemStack = itemStack;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (itemStack == null) return;

        RenderUtils.renderGuiItem(
                itemStack,
                (int) (gearViewerScreen.getTranslationX() + this.x),
                (int) (gearViewerScreen.getTranslationY() + this.y),
                1);
    }

    @Override
    public void onPress() {}

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}

    public ItemStack getItemStack() {
        return itemStack;
    }
}
