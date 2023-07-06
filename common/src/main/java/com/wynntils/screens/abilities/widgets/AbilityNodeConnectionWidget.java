/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.abilities.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.models.abilitytree.type.AbilityTreeConnectionNode;
import com.wynntils.utils.render.RenderUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class AbilityNodeConnectionWidget extends AbstractWidget {
    public static final int SIZE = 20;

    private final AbilityTreeConnectionNode node;

    public AbilityNodeConnectionWidget(int x, int y, int width, int height, AbilityTreeConnectionNode node) {
        super(x, y, width, height, Component.literal("Connection Node"));
        this.node = node;
    }

    @Override
    public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        RenderUtils.renderItem(poseStack, node.getItemStack(), this.getX(), this.getY());
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

    public AbilityTreeConnectionNode getNode() {
        return node;
    }
}
