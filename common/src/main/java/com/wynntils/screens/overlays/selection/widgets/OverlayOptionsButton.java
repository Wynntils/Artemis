/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.overlays.selection.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.base.widgets.BasicTexturedButton;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class OverlayOptionsButton extends BasicTexturedButton {
    private final StyledText message;
    private final Texture texture;

    private boolean isSelected;

    public OverlayOptionsButton(
            int x,
            int y,
            int width,
            int height,
            StyledText message,
            Consumer<Integer> onClick,
            List<Component> tooltip,
            Texture texture,
            boolean isSelected) {
        super(x, y, width, height, texture, onClick, tooltip);

        this.message = message;
        this.texture = texture;
        this.isSelected = isSelected;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        // Only count as hovered if the mouse is outside of the background area as a slight bit
        // of the button is rendered underneath the background
        if (isHovered
                && MathUtils.isInside(
                        mouseX,
                        mouseY,
                        0,
                        Texture.OVERLAY_SELECTION_GUI.width(),
                        0,
                        Texture.OVERLAY_SELECTION_GUI.height())) {
            isHovered = false;
        }

        // When selected or hovered it should use the alternate texture
        RenderUtils.drawHoverableTexturedRect(poseStack, texture, getX(), getY(), this.isHovered || this.isSelected);

        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        message,
                        getX() + 2,
                        getX() + getWidth() - 4,
                        getY() + 10,
                        getY() + getHeight() - 10,
                        getWidth() - 4,
                        CommonColors.WHITE,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.TOP,
                        TextShadow.NORMAL);
    }

    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }
}
