/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.base.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.screens.base.WynntilsPagedScreen;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class PageSelectorButton extends WynntilsButton {
    private static final ResourceLocation BOOK_TURN_PAGE_ID =
            ResourceLocation.fromNamespaceAndPath("wynntils", "ui.book.turn-page");
    private static final SoundEvent BOOK_TURN_PAGE_SOUND = SoundEvent.createVariableRangeEvent(BOOK_TURN_PAGE_ID);

    private final boolean forward;
    private final WynntilsPagedScreen screen;

    public PageSelectorButton(int x, int y, int width, int height, boolean forward, WynntilsPagedScreen screen) {
        super(x, y, width, height, Component.literal("Page Selector Button"));
        this.forward = forward;
        this.screen = screen;
    }

    @Override
    public void onPress() {
        if (!isValid()) return;

        McUtils.playSoundUI(BOOK_TURN_PAGE_SOUND);

        if (forward) {
            screen.setCurrentPage(screen.getCurrentPage() + 1);
        } else {
            screen.setCurrentPage(screen.getCurrentPage() - 1);
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        Texture arrowTexture = this.forward ? Texture.FORWARD_ARROW_OFFSET : Texture.BACKWARD_ARROW_OFFSET;

        if (isValid()) {
            drawTexture(poseStack, arrowTexture, arrowTexture.width() / 2);
        } else {
            drawTexture(poseStack, arrowTexture, 0);
        }
    }

    private void drawTexture(PoseStack poseStack, Texture texture, int uOffset) {
        RenderUtils.drawTexturedRect(
                poseStack,
                texture.resource(),
                this.getX(),
                this.getY(),
                0,
                this.width,
                this.height,
                uOffset,
                0,
                texture.width() / 2,
                texture.height(),
                texture.width(),
                texture.height());
    }

    private boolean isValid() {
        return forward ? screen.getCurrentPage() < screen.getMaxPage() : screen.getCurrentPage() > 0;
    }
}
