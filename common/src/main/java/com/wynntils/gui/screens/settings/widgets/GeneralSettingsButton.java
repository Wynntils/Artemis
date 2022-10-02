/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.screens.settings.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.gui.render.FontRenderer;
import com.wynntils.gui.render.HorizontalAlignment;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.VerticalAlignment;
import com.wynntils.mc.objects.CommonColors;
import com.wynntils.mc.objects.CustomColor;
import com.wynntils.mc.utils.ComponentUtils;
import java.util.List;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class GeneralSettingsButton extends AbstractButton {
    private static final CustomColor BACKGROUND_COLOR = new CustomColor(98, 34, 8);
    private static final CustomColor HOVER_BACKGROUND_COLOR = new CustomColor(158, 52, 16);
    private final Runnable onClick;
    private final String title;
    private final List<Component> tooltip;

    public GeneralSettingsButton(
            int x, int y, int width, int height, Component title, Runnable onClick, List<Component> tooltip) {
        super(x, y, width, height, title);
        this.onClick = onClick;
        this.title = ComponentUtils.getUnformatted(title);
        this.tooltip = tooltip;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        RenderUtils.drawRoundedRectWithBorder(
                poseStack,
                CommonColors.BLACK,
                isHovered ? HOVER_BACKGROUND_COLOR : BACKGROUND_COLOR,
                this.x,
                this.y,
                0,
                this.width,
                this.height,
                2,
                4,
                6);

        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        title,
                        this.x,
                        this.x + this.width,
                        this.y,
                        this.y + this.height,
                        0,
                        isHovered ? CommonColors.YELLOW : CommonColors.WHITE,
                        HorizontalAlignment.Center,
                        VerticalAlignment.Middle,
                        FontRenderer.TextShadow.OUTLINE);

        if (isHovered) {
            RenderUtils.drawTooltipAt(
                    poseStack,
                    mouseX,
                    mouseY,
                    0,
                    tooltip,
                    FontRenderer.getInstance().getFont(),
                    true);
        }
    }

    @Override
    public void onPress() {
        onClick.run();
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}
}
