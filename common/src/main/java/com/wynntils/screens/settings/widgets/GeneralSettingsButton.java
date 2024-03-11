/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings.widgets;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class GeneralSettingsButton extends WynntilsButton {
    public static final CustomColor BACKGROUND_COLOR = new CustomColor(98, 34, 8);
    public static final CustomColor HOVER_BACKGROUND_COLOR = new CustomColor(158, 52, 16);
    private final List<Component> tooltip;
    private final float translationX;
    private final float translationY;

    protected GeneralSettingsButton(
            int x,
            int y,
            int width,
            int height,
            Component title,
            List<Component> tooltip,
            float translationX,
            float translationY) {
        super(x, y, width, height, title);
        this.tooltip = tooltip;
        this.translationX = translationX;
        this.translationY = translationY;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();
        RenderUtils.drawRoundedRectWithBorder(
                poseStack,
                CommonColors.BLACK,
                getBackgroundColor(),
                this.getX(),
                this.getY(),
                0,
                this.width,
                this.height,
                1,
                3,
                3);

        FontRenderer.getInstance()
                .renderScrollingAlignedTextInBox(
                        poseStack,
                        StyledText.fromComponent(getMessage()),
                        this.getX(),
                        this.getX() + this.width,
                        this.getY(),
                        this.getY() + this.height,
                        this.width - 2,
                        translationX,
                        translationY,
                        getTextColor(),
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.OUTLINE);

        if (isHovered) {
            McUtils.mc().screen.setTooltipForNextRenderPass(Lists.transform(tooltip, Component::getVisualOrderText));
        }
    }

    protected CustomColor getBackgroundColor() {
        return isHovered ? HOVER_BACKGROUND_COLOR : BACKGROUND_COLOR;
    }

    protected CustomColor getTextColor() {
        return isHovered ? CommonColors.YELLOW : CommonColors.WHITE;
    }
}
