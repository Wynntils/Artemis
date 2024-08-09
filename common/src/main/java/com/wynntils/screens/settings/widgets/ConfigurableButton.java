/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings.widgets;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Configurable;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.overlays.CustomNameProperty;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.base.widgets.WynntilsCheckbox;
import com.wynntils.screens.settings.WynntilsBookSettingsScreen;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class ConfigurableButton extends WynntilsButton {
    private final Configurable configurable;
    private final WynntilsCheckbox enabledCheckbox;
    private final int maskTopY;
    private final int maskBottomY;
    private final int matchingConfigs;
    private final List<Component> descriptionTooltip;
    private final WynntilsBookSettingsScreen settingsScreen;

    public ConfigurableButton(
            int x,
            int y,
            int width,
            int height,
            Configurable configurable,
            WynntilsBookSettingsScreen screen,
            int matchingConfigs) {
        super(x, y, width, height, Component.literal(configurable.getTranslatedName()));
        this.configurable = configurable;
        this.settingsScreen = screen;

        if (configurable instanceof Feature feature) {
            descriptionTooltip =
                    ComponentUtils.wrapTooltips(List.of(Component.literal(feature.getTranslatedDescription())), 150);
        } else {
            descriptionTooltip = List.of();
        }

        boolean enabled = false;
        if (configurable instanceof Overlay selectedOverlay) {
            enabled = Managers.Overlay.isEnabled(selectedOverlay);
        } else if (configurable instanceof Feature selectedFeature) {
            enabled = selectedFeature.isEnabled();
        }

        this.enabledCheckbox = new WynntilsCheckbox(x + width - 10, y, 10, Component.literal(""), enabled, 0);

        this.maskTopY = settingsScreen.getMaskTopY();
        this.maskBottomY = settingsScreen.getConfigurableMaskBottomY();
        this.matchingConfigs = matchingConfigs;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        // Don't want to display tooltip when the tile is outside the mask from the screen
        if (isHovered && (mouseY <= maskTopY || mouseY >= maskBottomY)) {
            isHovered = false;
        }

        CustomColor color = isHovered ? CommonColors.YELLOW : CommonColors.WHITE;

        if (McUtils.mc().screen instanceof WynntilsBookSettingsScreen bookSettingsScreen) {
            if (bookSettingsScreen.getSelectedConfigurable() == configurable) {
                color = CommonColors.GRAY;
            }
        }

        boolean isOverlay = configurable instanceof Overlay;

        String textToRender = configurable.getTranslatedName();

        if (configurable instanceof CustomNameProperty customNameProperty) {
            if (!customNameProperty.getCustomName().get().isEmpty()) {
                textToRender = customNameProperty.getCustomName().get();
            }
        }

        if (matchingConfigs > 0) {
            textToRender += ChatFormatting.GRAY + " [" + matchingConfigs + "]";
        }

        FontRenderer.getInstance()
                .renderScrollingText(
                        poseStack,
                        StyledText.fromString(textToRender),
                        (isOverlay ? this.getX() + 12 : this.getX()),
                        this.getY(),
                        (isOverlay ? this.width - 12 : this.width) - 11,
                        color,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NORMAL,
                        1f);

        enabledCheckbox.render(guiGraphics, mouseX, mouseY, partialTick);

        if (isHovered && configurable instanceof Feature) {
            McUtils.mc()
                    .screen
                    .setTooltipForNextRenderPass(Lists.transform(descriptionTooltip, Component::getVisualOrderText));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Prevent interaction when the tile is outside of the mask from the screen
        if ((mouseY <= maskTopY || mouseY >= maskBottomY)) return false;

        // Toggle the enabled state of the configurable when toggling the checkbox
        if (enabledCheckbox.isMouseOver(mouseX, mouseY)) {
            if (configurable instanceof Feature feature) {
                feature.setUserEnabled(!feature.isEnabled());
            } else if (configurable instanceof Overlay) {
                Optional<Config<?>> configOpt = configurable.getConfigOptionFromString("userEnabled");

                if (configOpt.isPresent()) {
                    Config<Boolean> config = (Config<Boolean>) configOpt.get();
                    config.setValue(!config.get());
                } else {
                    return false;
                }
            }

            // Repopulate screen to update new enabled/disabled states
            if (McUtils.mc().screen instanceof WynntilsBookSettingsScreen bookSettingsScreen) {
                bookSettingsScreen.populateConfigurables();
            }

            return enabledCheckbox.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onPress() {
        if (McUtils.mc().screen instanceof WynntilsBookSettingsScreen bookSettingsScreen) {
            bookSettingsScreen.setSelectedConfigurable(configurable);
        }
    }

    @Override
    public void setY(int y) {
        super.setY(y);

        enabledCheckbox.setY(y);
    }
}
