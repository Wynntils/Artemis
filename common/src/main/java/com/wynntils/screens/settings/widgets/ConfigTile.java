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
import com.wynntils.screens.overlays.selection.OverlaySelectionScreen;
import com.wynntils.screens.settings.WynntilsBookSettingsScreen;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
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
    private final TextboxScreen screen;
    private final int maskTopY;
    private final int maskBottomY;
    private final float translationX;
    private final float translationY;
    private final GeneralSettingsButton resetButton;
    private final StyledText displayName;
    private AbstractWidget configOptionElement;

    public ConfigTile(int x, int y, int width, int height, TextboxScreen screen, Config<?> config) {
        super(x, y, width, height, Component.literal(config.getJsonName()));
        this.screen = screen;

        if (screen instanceof WynntilsBookSettingsScreen settingsScreen) {
            maskTopY = settingsScreen.getMaskTopY();
            maskBottomY = settingsScreen.getConfigMaskBottomY();
            displayName = settingsScreen.configOptionContains(config)
                    ? StyledText.fromString(ChatFormatting.UNDERLINE + config.getDisplayName())
                    : StyledText.fromString(config.getDisplayName());
            translationX = settingsScreen.getTranslationX();
            translationY = settingsScreen.getTranslationY();
        } else if (screen instanceof OverlaySelectionScreen overlaySelectionScreen) {
            maskTopY = overlaySelectionScreen.getConfigMaskTopY();
            maskBottomY = overlaySelectionScreen.getConfigMaskBottomY();
            displayName = overlaySelectionScreen.configOptionContains(config)
                    ? StyledText.fromString(ChatFormatting.UNDERLINE + config.getDisplayName())
                    : StyledText.fromString(config.getDisplayName());
            translationX = overlaySelectionScreen.getTranslationX();
            translationY = overlaySelectionScreen.getTranslationY();
        } else {
            maskTopY = 0;
            maskBottomY = McUtils.mc().screen.height;
            displayName = StyledText.fromString(config.getDisplayName());
            translationX = 0;
            translationY = 0;
        }

        this.configOptionElement = getWidgetFromConfig(config);
        this.resetButton = new ResetButton(
                config,
                () -> configOptionElement = getWidgetFromConfig(config),
                x + width - 40,
                getRenderY(),
                maskTopY,
                maskBottomY);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        resetButton.render(guiGraphics, mouseX, mouseY, partialTick);

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

        configOptionElement.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderDisplayName(PoseStack poseStack) {
        FontRenderer.getInstance()
                .renderScrollingText(
                        poseStack,
                        displayName,
                        getRenderX(),
                        this.getY() + 3,
                        this.width - 3,
                        CommonColors.BLACK,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NONE,
                        0.8f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Prevent interaction when the tile is outside of the mask from the screen, same applies to drag and released
        if ((mouseY <= maskTopY || mouseY >= maskBottomY)) return false;

        return resetButton.mouseClicked(mouseX, mouseY, button)
                || configOptionElement.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if ((mouseY <= maskTopY || mouseY >= maskBottomY)) return false;

        return configOptionElement.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
                || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if ((mouseY <= maskTopY || mouseY >= maskBottomY)) return false;

        return configOptionElement.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void setY(int y) {
        super.setY(y);

        configOptionElement.setY(getRenderY());
        resetButton.setY(getRenderY());
    }

    @Override
    public void onPress() {
        // noop
    }

    private int getRenderY() {
        return this.getY() + 19;
    }

    private int getRenderX() {
        return this.getX() + 3;
    }

    private <E extends Enum<E>> AbstractWidget getWidgetFromConfig(Config<?> configOption) {
        if (configOption.getType().equals(Boolean.class)) {
            return new BooleanSettingsButton(
                    getRenderX(),
                    getRenderY(),
                    (Config<Boolean>) configOption,
                    maskTopY,
                    maskBottomY,
                    translationX,
                    translationY);
        } else if (configOption.isEnum()) {
            return new EnumSettingsButton<>(
                    getRenderX(),
                    getRenderY(),
                    (Config<E>) configOption,
                    maskTopY,
                    maskBottomY,
                    translationX,
                    translationY);
        } else if (configOption.getType().equals(CustomColor.class)) {
            return new CustomColorSettingsButton(
                    getRenderX(), getRenderY(), (Config<CustomColor>) configOption, screen, maskTopY, maskBottomY);
        } else {
            return new TextInputBoxSettingsWidget<>(
                    getRenderX(), getRenderY(), configOption, screen, maskTopY, maskBottomY);
        }
    }
}
