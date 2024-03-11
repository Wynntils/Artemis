/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.screens.base.widgets.BasicTexturedButton;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class GeneralSettingsTabButton extends BasicTexturedButton {
    private static final int MAX_OFFSET = -8;
    private static final int MIN_OFFSET = 0;

    protected boolean selectedTab = false;
    protected Texture tagTexture;

    private final OffsetDirection offsetDirection;
    private final Texture iconTexture;

    private int hoverOffset = 0;

    GeneralSettingsTabButton(
            int x,
            int y,
            int width,
            int height,
            Consumer<Integer> onClick,
            List<Component> tooltip,
            Texture tagTexture,
            Texture iconTexture,
            OffsetDirection offsetDirection) {
        super(x, y, width, height, tagTexture, onClick, tooltip);
        this.tagTexture = tagTexture;
        this.iconTexture = iconTexture;
        this.offsetDirection = offsetDirection;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        // Don't count as hovered if mouse is hovering the book as the tags render
        // slightly underneath the book
        if (isHovered
                && ((offsetDirection == OffsetDirection.UP && mouseY >= 0)
                        || (offsetDirection == OffsetDirection.RIGHT
                                && mouseX <= Texture.CONFIG_BOOK_BACKGROUND.width())
                        || (offsetDirection == OffsetDirection.DOWN
                                && mouseY <= Texture.CONFIG_BOOK_BACKGROUND.height())
                        || (offsetDirection == OffsetDirection.LEFT && mouseX >= 0))) {
            isHovered = false;
        }

        // Determine the offset of the tag. When selected the max offset should be used, otherwise when hovered
        // increase until limit reached.
        if (selectedTab) {
            this.hoverOffset = (offsetDirection == OffsetDirection.UP || offsetDirection == OffsetDirection.LEFT)
                    ? MAX_OFFSET
                    : -MAX_OFFSET;
        } else if (this.isHovered) {
            hoverOffset += (offsetDirection == OffsetDirection.UP || offsetDirection == OffsetDirection.LEFT) ? -1 : 1;
            hoverOffset = (offsetDirection == OffsetDirection.UP || offsetDirection == OffsetDirection.LEFT)
                    ? Math.max(hoverOffset, MAX_OFFSET)
                    : Math.min(hoverOffset, -MAX_OFFSET);
        } else {
            hoverOffset += (offsetDirection == OffsetDirection.UP || offsetDirection == OffsetDirection.LEFT) ? 1 : -1;
            hoverOffset = (offsetDirection == OffsetDirection.UP || offsetDirection == OffsetDirection.LEFT)
                    ? Math.min(hoverOffset, MIN_OFFSET)
                    : Math.max(hoverOffset, MIN_OFFSET);
        }

        // Move the tag render position
        poseStack.pushPose();
        poseStack.translate(
                (offsetDirection == OffsetDirection.RIGHT || offsetDirection == OffsetDirection.LEFT) ? hoverOffset : 0,
                (offsetDirection == OffsetDirection.UP || offsetDirection == OffsetDirection.DOWN) ? hoverOffset : 0,
                0);

        RenderUtils.drawTexturedRect(poseStack, tagTexture, this.getX(), this.getY());

        // Render icon on tag
        if (offsetDirection == OffsetDirection.UP || offsetDirection == OffsetDirection.DOWN) {
            RenderUtils.drawTexturedRect(
                    poseStack, iconTexture, getX() + (getWidth() - iconTexture.width()) / 2f, getY() + 14);
        } else {
            RenderUtils.drawTexturedRect(
                    poseStack, iconTexture, getX() + 14, getY() + (getHeight() - iconTexture.height()) / 2f);
        }

        poseStack.popPose();
    }

    protected enum OffsetDirection {
        UP,
        RIGHT,
        DOWN,
        LEFT
    }
}
