/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.Texture;
import com.wynntils.mc.objects.CustomColor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public class DiscoveryFilterButton extends AbstractButton {
    private static final CustomColor BUTTON_COLOR = new CustomColor(181, 174, 151);
    private static final CustomColor BUTTON_COLOR_HOVERED = new CustomColor(121, 116, 101);
    private static final CustomColor BUTTON_COLOR_ENABLED = new CustomColor(164, 212, 142);

    private final Texture texture;
    private final boolean dynamicTexture;
    private final List<Component> tooltipList;
    private final Runnable onPress;
    private final Supplier<Boolean> isEnabled;

    public DiscoveryFilterButton(
            int x,
            int y,
            int width,
            int height,
            Texture texture,
            boolean dynamicTexture,
            List<Component> tooltipList,
            Runnable onPress,
            Supplier<Boolean> isEnabled) {
        super(x, y, width, height, new TextComponent("Discovery Filter Button"));

        this.texture = texture;
        this.dynamicTexture = dynamicTexture;
        this.tooltipList = tooltipList;
        this.onPress = onPress;
        this.isEnabled = isEnabled;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        RenderUtils.drawRect(
                poseStack,
                isEnabled.get() ? BUTTON_COLOR_ENABLED : isHovered ? BUTTON_COLOR_HOVERED : BUTTON_COLOR,
                x,
                y,
                0,
                width,
                height);

        if (!this.dynamicTexture) {
            RenderUtils.drawTexturedRect(
                    poseStack,
                    texture.resource(),
                    x + (width - texture.width()) / 2f,
                    y + (height - texture.height()) / 2f,
                    1,
                    texture.width(),
                    texture.height(),
                    0,
                    0,
                    texture.width(),
                    texture.height(),
                    texture.width(),
                    texture.height());
            ;
        } else {
            if (this.isHovered) {
                RenderUtils.drawTexturedRect(
                        poseStack,
                        texture.resource(),
                        x + (width - texture.width()) / 2f,
                        y + (height - texture.height() / 2f) / 2f,
                        1,
                        texture.width(),
                        texture.height() / 2f,
                        0,
                        texture.height() / 2f,
                        texture.width(),
                        texture.height() / 2f,
                        texture.width(),
                        texture.height());
            } else {
                RenderUtils.drawTexturedRect(
                        poseStack,
                        texture.resource(),
                        x + (width - texture.width()) / 2f,
                        y + (height - texture.height() / 2f) / 2f,
                        1,
                        texture.width(),
                        texture.height() / 2f,
                        0,
                        0,
                        texture.width(),
                        texture.height() / 2f,
                        texture.width(),
                        texture.height());
            }
        }
    }

    public List<Component> getTooltipLines() {
        List<Component> renderedTooltip = new ArrayList<>(tooltipList);

        if (isEnabled.get()) {
            renderedTooltip.add(new TranslatableComponent("screens.wynntils.wynntilsDiscoveries.clickToHide")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            renderedTooltip.add(new TranslatableComponent("screens.wynntils.wynntilsDiscoveries.clickToShow")
                    .withStyle(ChatFormatting.GRAY));
        }

        return renderedTooltip;
    }

    @Override
    public void onPress() {
        onPress.run();
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}
}
