/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.itemfilter.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.itemfilter.ItemFilterScreen;
import com.wynntils.services.itemfilter.type.SortDirection;
import com.wynntils.services.itemfilter.type.SortInfo;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class SortWidget extends AbstractWidget {
    private final ItemFilterScreen filterScreen;
    private final float translationX;
    private final float translationY;

    private final List<Button> buttons = new ArrayList<>();

    private SortInfo sortInfo;

    public SortWidget(
            int x, int y, ItemFilterScreen filterScreen, float translationX, float translationY, SortInfo sortInfo) {
        super(x, y, 170, 20, Component.literal("Sort Widget"));

        this.filterScreen = filterScreen;
        this.translationX = translationX;
        this.translationY = translationY;

        this.sortInfo = sortInfo;

        Button sortButton = new Button.Builder(
                        Component.literal(sortInfo.direction() == SortDirection.DESCENDING ? "v" : "ʌ"),
                        (button) -> toggleSortDirection())
                .pos(x + width - 50, y)
                .size(30, 20)
                .build();

        Button upButton = new Button.Builder(
                        Component.literal("🠝"), (button) -> filterScreen.reorderSort(sortInfo, -1))
                .pos(x + width - 20, y)
                .size(10, 20)
                .build();

        Button downButton = new Button.Builder(
                        Component.literal("🠟"), (button) -> filterScreen.reorderSort(sortInfo, 1))
                .pos(x + width - 10, y)
                .size(10, 20)
                .build();

        Pair<Boolean, Boolean> canSortMove = filterScreen.canSortMove(sortInfo);
        upButton.active = canSortMove.a();
        downButton.active = canSortMove.b();

        buttons.add(sortButton);
        buttons.add(upButton);
        buttons.add(downButton);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        RenderUtils.drawRectBorders(
                poseStack,
                sortInfo.direction() == SortDirection.DESCENDING ? CommonColors.WHITE : CommonColors.BLACK,
                getX(),
                getY(),
                getX() + width,
                getY() + height,
                1,
                1);

        FontRenderer.getInstance()
                .renderScrollingText(
                        poseStack,
                        StyledText.fromString(sortInfo.provider().getDisplayName()),
                        getX() + 2,
                        getY() + (height / 2f),
                        width - 54,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL,
                        1.0f);

        for (Button button : buttons) {
            button.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Prevent interaction when the button is outside of the mask from the screen
        if ((mouseY <= filterScreen.getProviderMaskTopY() || mouseY >= filterScreen.getProviderMaskBottomY())) {
            return false;
        }

        for (GuiEventListener listener : buttons) {
            if (listener.isMouseOver(mouseX, mouseY)) {
                return listener.mouseClicked(mouseX, mouseY, button);
            }
        }

        return false;
    }

    @Override
    public void setY(int y) {
        super.setY(y);

        for (Button button : buttons) {
            button.setY(y);
        }
    }

    private void toggleSortDirection() {
        SortInfo oldSortInfo = sortInfo;
        sortInfo = new SortInfo(
                sortInfo.direction() == SortDirection.DESCENDING ? SortDirection.ASCENDING : SortDirection.DESCENDING,
                sortInfo.provider());
        filterScreen.changeSort(oldSortInfo, sortInfo);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
