/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.gui.render.FontRenderer;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.Texture;
import com.wynntils.mc.utils.ComponentUtils;
import java.util.List;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class BasicTexturedButton extends AbstractButton {
    private final Texture texture;

    private final Runnable onClick;
    private final List<Component> tooltip;

    public BasicTexturedButton(
            int x, int y, int width, int height, Texture texture, Runnable onClick, List<Component> tooltip) {
        super(x, y, width, height, new TextComponent("Basic Button"));
        this.texture = texture;
        this.onClick = onClick;
        this.tooltip = ComponentUtils.wrapTooltips(tooltip, 250);
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        RenderUtils.drawTexturedRect(poseStack, texture, this.x, this.y);

        if (this.isHovered) {
            RenderUtils.drawTooltipAt(
                    poseStack,
                    mouseX,
                    mouseY - RenderUtils.getToolTipHeight(RenderUtils.componentToClientTooltipComponent(tooltip)),
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
