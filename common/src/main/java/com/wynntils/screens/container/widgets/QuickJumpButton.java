/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.container.widgets;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class QuickJumpButton extends WynntilsButton {
    private final int destination;
    private final PersonalStorageUtilitiesWidget parent;

    public QuickJumpButton(int x, int y, int destination, PersonalStorageUtilitiesWidget parent) {
        super(x, y, 16, 16, Component.literal("Container Quick Jump Button"));

        this.destination = destination;
        this.parent = parent;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        RenderUtils.drawHoverableTexturedRect(poseStack, Texture.QUICK_JUMP_BUTTON, getX(), getY(), isHovered);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(String.valueOf(destination)),
                        getX() + 8,
                        getY() + 8,
                        CommonColors.WHITE,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);

        if (isHovered) {
            McUtils.mc()
                    .screen
                    .setTooltipForNextRenderPass(Lists.transform(
                            List.of(Component.translatable(
                                    "feature.wynntils.personalStorageUtilities.clickToJump",
                                    Models.Bank.getPageName(destination))),
                            Component::getVisualOrderText));
        }
    }

    @Override
    public void onPress() {
        parent.jumpToPage(destination);
    }
}
