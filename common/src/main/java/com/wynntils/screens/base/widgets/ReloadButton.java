/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.base.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.screens.base.TooltipProvider;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class ReloadButton extends WynntilsButton implements TooltipProvider {
    private static final List<Component> RELOAD_TOOLTIP = List.of(
            Component.translatable("screens.wynntils.wynntilsDiscoveries.reload.name")
                    .withStyle(ChatFormatting.WHITE),
            Component.translatable("screens.wynntils.wynntilsDiscoveries.reload.description")
                    .withStyle(ChatFormatting.GRAY));

    private final Runnable onClickRunnable;

    public ReloadButton(int x, int y, int width, int height, Runnable onClickRunnable) {
        super(x, y, width, height, Component.literal("Reload Button"));
        this.onClickRunnable = onClickRunnable;
    }

    @Override
    public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        Texture reloadButton = Texture.RELOAD_BUTTON;
        if (this.isHovered) {
            RenderUtils.drawTexturedRect(
                    poseStack,
                    reloadButton.resource(),
                    this.getX(),
                    this.getY(),
                    0,
                    this.width,
                    this.height,
                    reloadButton.width() / 2,
                    0,
                    reloadButton.width() / 2,
                    reloadButton.height(),
                    reloadButton.width(),
                    reloadButton.height());
        } else {
            RenderUtils.drawTexturedRect(
                    poseStack,
                    reloadButton.resource(),
                    this.getX(),
                    this.getY(),
                    0,
                    this.width,
                    this.height,
                    0,
                    0,
                    reloadButton.width() / 2,
                    reloadButton.height(),
                    reloadButton.width(),
                    reloadButton.height());
        }
    }

    @Override
    public void onPress() {
        onClickRunnable.run();
    }

    @Override
    public List<Component> getTooltipLines() {
        return RELOAD_TOOLTIP;
    }
}
