/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.base.widgets;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class WynntilsCheckbox extends Checkbox {
    private final boolean showLabel;
    private final int maxTextWidth;
    private final int color;

    private Consumer<Integer> onClick;
    private List<Component> tooltip;

    public WynntilsCheckbox(
            int x,
            int y,
            int width,
            int height,
            Component message,
            boolean selected,
            int maxTextWidth,
            boolean showLabel) {
        super(x, y, width, height, message, selected);
        this.maxTextWidth = maxTextWidth;
        this.showLabel = showLabel;
        this.color = CommonColors.WHITE.asInt();
    }

    public WynntilsCheckbox(
            int x,
            int y,
            int width,
            int height,
            Component message,
            boolean selected,
            int maxTextWidth,
            boolean showLabel,
            Consumer<Integer> onClick,
            List<Component> tooltip) {
        super(x, y, width, height, message, selected);
        this.maxTextWidth = maxTextWidth;
        this.showLabel = showLabel;
        this.color = CommonColors.WHITE.asInt();
        this.onClick = onClick;
        this.tooltip = tooltip;
    }

    public WynntilsCheckbox(
            int x,
            int y,
            int width,
            int height,
            Component message,
            boolean selected,
            int maxTextWidth,
            boolean showLabel,
            CustomColor color) {
        super(x, y, width, height, message, selected);
        this.maxTextWidth = maxTextWidth;
        this.showLabel = showLabel;
        this.color = color.asInt();
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.enableDepthTest();
        Font font = FontRenderer.getInstance().getFont();
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        ResourceLocation resourceLocation;
        if (this.selected) {
            resourceLocation = this.isFocused() ? CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE : CHECKBOX_SELECTED_SPRITE;
        } else {
            resourceLocation = this.isFocused() ? CHECKBOX_HIGHLIGHTED_SPRITE : CHECKBOX_SPRITE;
        }

        guiGraphics.blitSprite(resourceLocation, this.getX(), this.getY(), this.width, this.height);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.showLabel) {
            int start = this.getX() + this.width + 2;
            int end = start + this.maxTextWidth;

            renderScrollingString(
                    guiGraphics,
                    font,
                    this.getMessage(),
                    start,
                    start,
                    this.getY(),
                    end,
                    this.getY() + this.getHeight(),
                    this.color);
        }

        if (isHovered && tooltip != null) {
            McUtils.mc().screen.setTooltipForNextRenderPass(Lists.transform(tooltip, Component::getVisualOrderText));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        if (onClick != null) {
            onClick.accept(button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
