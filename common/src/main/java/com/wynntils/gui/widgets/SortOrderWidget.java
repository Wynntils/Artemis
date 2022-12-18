/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.Texture;
import com.wynntils.gui.screens.WynntilsQuestBookScreen;
import com.wynntils.wynn.model.quests.QuestSortOrder;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class SortOrderWidget extends AbstractButton {
    private final WynntilsQuestBookScreen questBookScreen;

    public SortOrderWidget(int x, int y, int width, int height, WynntilsQuestBookScreen questBookScreen) {
        super(x, y, width, height, Component.literal("Sort Order Button"));
        this.questBookScreen = questBookScreen;
    }

    @Override
    public void onPress() {
        QuestSortOrder questSortOrder = questBookScreen.getQuestSortOrder();

        QuestSortOrder[] questSortOrders = QuestSortOrder.values();

        QuestSortOrder newSort = questSortOrders[(questSortOrder.ordinal() + 1) % questSortOrders.length];

        questBookScreen.setQuestSortOrder(newSort);
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        Texture sortTexture =
                switch (questBookScreen.getQuestSortOrder()) {
                    case LEVEL -> Texture.SORT_LEVEL;
                    case DISTANCE -> Texture.SORT_DISTANCE;
                    case ALPHABETIC -> Texture.SORT_ALPHABETICALLY;
                };

        float renderX = this.x;
        float renderY = this.y;

        if (this.isHovered) {
            RenderUtils.drawTexturedRect(
                    poseStack,
                    sortTexture.resource(),
                    renderX,
                    renderY,
                    0,
                    this.width,
                    this.height,
                    0,
                    sortTexture.height() / 2,
                    sortTexture.width(),
                    sortTexture.height() / 2,
                    sortTexture.width(),
                    sortTexture.height());
        } else {
            RenderUtils.drawTexturedRect(
                    poseStack,
                    sortTexture.resource(),
                    renderX,
                    renderY,
                    0,
                    this.width,
                    this.height,
                    0,
                    0,
                    sortTexture.width(),
                    sortTexture.height() / 2,
                    sortTexture.width(),
                    sortTexture.height());
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}
}
