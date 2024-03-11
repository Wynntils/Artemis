/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.settings.widgets;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.settings.WynntilsBookSettingsScreen;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;

public class ResetButton extends WynntilsButton {
    private static final CustomColor BACKGROUND_COLOR = new CustomColor(98, 34, 8);

    private final Config<?> config;
    private final List<Component> tooltip;
    private final Runnable onClick;

    ResetButton(Config<?> config, Runnable onClick, int x, int y) {
        super(x, y, 20, 20, Component.translatable("screens.wynntils.settingsScreen.reset.name"));
        this.config = config;
        this.tooltip = List.of(Component.translatable("screens.wynntils.settingsScreen.reset.description"));
        this.onClick = onClick;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();
        RenderUtils.drawRoundedRectWithBorder(
                poseStack,
                CommonColors.BLACK,
                BACKGROUND_COLOR,
                this.getX(),
                this.getY(),
                0,
                this.width,
                this.height,
                1,
                3,
                3);

        RenderUtils.drawTexturedRect(poseStack, Texture.RESET, this.getX() + 2, this.getY() + 2);

        if (isHovered) {
            McUtils.mc().screen.setTooltipForNextRenderPass(Lists.transform(tooltip, Component::getVisualOrderText));
        }
    }

    @Override
    public void playDownSound(SoundManager handler) {
        if (!config.valueChanged()) return;
        super.playDownSound(handler);
    }

    @Override
    public void onPress() {
        if (!config.valueChanged()) return;
        config.reset();
        onClick.run();

        // Reload configurables to update checkbox
        if (McUtils.mc().screen instanceof WynntilsBookSettingsScreen bookSettingsScreen) {
            bookSettingsScreen.populateConfigurables();
        }
    }
}
