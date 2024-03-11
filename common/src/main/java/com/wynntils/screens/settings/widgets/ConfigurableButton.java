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
    private final float translationX;
    private final float translationY;
    private final List<Component> descriptionTooltip;
    private final String textToRender;
    private final WynntilsCheckbox enabledCheckbox;

    public ConfigurableButton(
            int x,
            int y,
            int width,
            int height,
            Configurable configurable,
            int matchingConfigs,
            float translationX,
            float translationY) {
        super(x, y, width, height, Component.literal(configurable.getTranslatedName()));
        this.configurable = configurable;
        this.translationX = translationX;
        this.translationY = translationY;

        boolean enabled = false;
        String name = configurable.getTranslatedName();

        if (configurable instanceof Overlay selectedOverlay) {
            enabled = Managers.Overlay.isEnabled(selectedOverlay);

            // Show the custom name for info boxes/custom bars if given
            if (selectedOverlay instanceof CustomNameProperty customNameProperty) {
                if (!customNameProperty.getCustomName().get().isEmpty()) {
                    name = customNameProperty.getCustomName().get();
                }
            }
        } else if (configurable instanceof Feature selectedFeature) {
            enabled = selectedFeature.isEnabled();
        }

        this.enabledCheckbox =
                new WynntilsCheckbox(x + width - 10, y, 10, 10, Component.literal(""), enabled, 0, false);

        if (configurable instanceof Feature feature) {
            descriptionTooltip =
                    ComponentUtils.wrapTooltips(List.of(Component.literal(feature.getTranslatedDescription())), 150);
        } else {
            descriptionTooltip = List.of();
        }

        String text = (configurable instanceof Overlay ? "   " : "") + name;

        // Display a counter of how many configs match the current search query after
        // the configurable name.
        if (matchingConfigs > 0) {
            text += ChatFormatting.GRAY + " [" + matchingConfigs + "]";
        }

        this.textToRender = text;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        CustomColor color = isHovered ? CommonColors.YELLOW : CommonColors.WHITE;

        if (McUtils.mc().screen instanceof WynntilsBookSettingsScreen bookSettingsScreen) {
            if (bookSettingsScreen.getSelectedConfigurable() == configurable) {
                color = CommonColors.GRAY;
            }
        }

        FontRenderer.getInstance()
                .renderScrollingString(
                        poseStack,
                        StyledText.fromString(textToRender),
                        getX(),
                        getY(),
                        this.width - 15,
                        translationX,
                        translationY,
                        color,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.TOP,
                        TextShadow.NORMAL);

        enabledCheckbox.render(guiGraphics, mouseX, mouseY, partialTick);

        if (isHovered && configurable instanceof Feature) {
            McUtils.mc()
                    .screen
                    .setTooltipForNextRenderPass(Lists.transform(descriptionTooltip, Component::getVisualOrderText));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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
                bookSettingsScreen.populateConfigs();
            }

            return enabledCheckbox.mouseClicked(mouseX, mouseY, button);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onPress() {
        if (McUtils.mc().screen instanceof WynntilsBookSettingsScreen bookSettingsScreen) {
            bookSettingsScreen.setSelected(configurable);
        }
    }
}
