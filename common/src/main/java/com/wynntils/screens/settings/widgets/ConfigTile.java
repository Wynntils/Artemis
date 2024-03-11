/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.base.TextboxScreen;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.settings.WynntilsBookSettingsScreen;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

public class ConfigTile extends WynntilsButton {
    private final Config<?> config;
    private final float translationX;
    private final float translationY;
    private final ResetButton resetButton;
    private final StyledText displayName;
    private final TextboxScreen screen;

    private AbstractWidget configOptionElement;

    public ConfigTile(
            int x,
            int y,
            int width,
            int height,
            TextboxScreen screen,
            Config<?> config,
            float translationX,
            float translationY) {
        super(x, y, width, height, Component.literal(config.getJsonName()));
        this.screen = screen;
        this.config = config;
        this.translationX = translationX;
        this.translationY = translationY;
        this.configOptionElement = getWidgetFromConfig(config);
        this.resetButton = new ResetButton(
                config, () -> configOptionElement = getWidgetFromConfig(config), x + width - 20, getRenderY() + 7);

        // Searching only matches configs on the settings screen
        if (screen instanceof WynntilsBookSettingsScreen settingsScreen
                && settingsScreen.configOptionContains(config)) {
            this.displayName = StyledText.fromString(ChatFormatting.UNDERLINE + config.getDisplayName());
        } else {
            this.displayName = StyledText.fromString(config.getDisplayName());
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        // Only need to show reset button if the value has been changed
        if (config.valueChanged()) {
            resetButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        renderDisplayName(poseStack);

        RenderUtils.drawLine(
                poseStack,
                CommonColors.GRAY,
                this.getX(),
                this.getY() + this.height,
                this.getX() + this.width,
                this.getY() + this.height,
                0,
                1);

        poseStack.pushPose();
        final int renderX = getRenderX();
        final int renderY = getRenderY();
        poseStack.translate(renderX, renderY, 0);
        configOptionElement.render(guiGraphics, mouseX - renderX, mouseY - renderY, partialTick);
        poseStack.popPose();
    }

    private void renderDisplayName(PoseStack poseStack) {
        FontRenderer.getInstance()
                .renderScrollingString(
                        poseStack,
                        displayName,
                        getRenderX(),
                        this.getY() + 3,
                        this.width,
                        translationX,
                        translationY,
                        CommonColors.BLACK,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NONE,
                        0.8f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double actualMouseX = mouseX - getRenderX();
        double actualMouseY = mouseY - getRenderY();

        return resetButton.mouseClicked(mouseX, mouseY, button)
                || configOptionElement.mouseClicked(actualMouseX, actualMouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        double actualMouseX = mouseX - getRenderX();
        double actualMouseY = mouseY - getRenderY();

        return configOptionElement.mouseDragged(actualMouseX, actualMouseY, button, deltaX, deltaY)
                || super.mouseDragged(actualMouseX, actualMouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double actualMouseX = mouseX - getRenderX();
        double actualMouseY = mouseY - getRenderY();

        return configOptionElement.mouseReleased(actualMouseX, actualMouseY, button)
                || super.mouseReleased(actualMouseX, actualMouseY, button);
    }

    @Override
    public void onPress() {
        // noop
    }

    private int getRenderY() {
        return this.getY() + 12;
    }

    private int getRenderX() {
        return this.getX() + 3;
    }

    private <E extends Enum<E>> AbstractWidget getWidgetFromConfig(Config<?> configOption) {
        if (configOption.getType().equals(Boolean.class)) {
            return new BooleanSettingsButton(
                    (Config<Boolean>) configOption, getRenderX() + this.translationX, getRenderY() + this.translationY);
        } else if (configOption.isEnum()) {
            return new EnumSettingsButton<>(
                    (Config<E>) configOption, getRenderX() + this.translationX, getRenderY() + this.translationY);
        } else if (configOption.getType().equals(CustomColor.class)) {
            return new CustomColorSettingsButton((Config<CustomColor>) configOption, screen);
        } else {
            return new TextInputBoxSettingsWidget<>(configOption, screen);
        }
    }
}
