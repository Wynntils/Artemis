/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.screens.settings.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.config.ConfigHolder;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.screens.settings.WynntilsBookSettingsScreen;
import com.wynntils.gui.widgets.TextInputBoxWidget;
import com.wynntils.mc.objects.CustomColor;

public class CustomColorConfigOptionElement extends TextConfigOptionElement {
    public CustomColorConfigOptionElement(ConfigHolder configHolder, WynntilsBookSettingsScreen screen) {
        super(configHolder, screen);
        this.textInputBoxWidget =
                new TextInputBoxWidget(0, 0, 80, (int) this.renderHeight, this::onTextInputUpdate, screen);
        this.textInputBoxWidget.setTextBoxInput(configHolder.getValue().toString());
    }

    @Override
    public void renderConfigAppropriateButton(
            PoseStack poseStack, float width, float height, int mouseX, int mouseY, float partialTicks) {
        poseStack.pushPose();
        poseStack.translate(0f, (height - renderHeight) / 2f - 5, 0f);

        textInputBoxWidget.render(poseStack, mouseX, mouseY, partialTicks);

        CustomColor value = (CustomColor) configHolder.getValue();

        RenderUtils.drawRect(poseStack, value, 85, 0, 0, renderHeight, renderHeight);

        poseStack.popPose();
    }
}
