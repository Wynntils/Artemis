/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.screens.base.TextboxScreen;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

public class CustomColorSettingsButton extends TextInputBoxSettingsWidget<CustomColor> {
    public CustomColorSettingsButton(Config<CustomColor> config, TextboxScreen textboxScreen) {
        super(config, textboxScreen, 100);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        PoseStack poseStack = guiGraphics.pose();

        CustomColor value = config.get();
        RenderUtils.drawRect(poseStack, value, width + 9, 7, 0, height, height);
    }
}
