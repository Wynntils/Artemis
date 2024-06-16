/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.maps.widgets;

import com.wynntils.core.text.StyledText;
import com.wynntils.screens.maps.WaypointCategoryScreen;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class CategoryWidget extends AbstractWidget {
    private final boolean rootCategory;
    private final String category;

    public CategoryWidget(int y, int width, int height, String category, boolean rootCategory) {
        super(0, y, width, height, Component.literal(category));

        this.category = category;
        this.rootCategory = rootCategory;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderUtils.drawRect(
                guiGraphics.pose(),
                CommonColors.BROWN.withAlpha(isHovered ? 150 : 100),
                this.getX(),
                this.getY(),
                0,
                width,
                height);

        FontRenderer.getInstance()
                .renderScrollingText(
                        guiGraphics.pose(),
                        StyledText.fromString(rootCategory ? "⏎ " + category : category),
                        getX() + 10,
                        getY() + getHeight() / 2f,
                        getWidth(),
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL,
                        1);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (McUtils.mc().screen instanceof WaypointCategoryScreen categoryScreen) {
            if (rootCategory) {
                categoryScreen.selectPreviousCategory();
            } else {
                categoryScreen.selectCategory(category);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
