/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.base.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.screens.base.TextboxScreen;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.Pair;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class SearchWidget extends TextInputBoxWidget {
    protected static final Component DEFAULT_TEXT =
            Component.translatable("screens.wynntils.searchWidget.defaultSearchText");
    private static final float VERTICAL_OFFSET = 6.5f;

    public SearchWidget(
            int x, int y, int width, int height, Consumer<String> onUpdateConsumer, TextboxScreen textboxScreen) {
        super(x, y, width, height, Component.literal("Search Box"), onUpdateConsumer, textboxScreen);
        textPadding = 5;
    }

    @Override
    public void renderWidget(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        RenderUtils.drawRect(poseStack, CommonColors.BLACK, this.getX(), this.getY(), 0, this.width, this.height);
        RenderUtils.drawRectBorders(
                poseStack,
                CommonColors.GRAY,
                this.getX(),
                this.getY(),
                this.getX() + this.width,
                this.getY() + this.height,
                0,
                1f);

        boolean defaultText = Objects.equals(textBoxInput, "") && !isFocused();

        Pair<String, Integer> renderedTextDetails = getRenderedText(this.width - 18);
        String renderedText = renderedTextDetails.a();

        Pair<Integer, Integer> highlightedVisibleInterval = getRenderedHighlighedInterval(renderedText);

        String firstPortion = renderedText.substring(0, highlightedVisibleInterval.a());
        String highlightedPortion =
                renderedText.substring(highlightedVisibleInterval.a(), highlightedVisibleInterval.b());
        String lastPortion = renderedText.substring(highlightedVisibleInterval.b());

        Font font = FontRenderer.getInstance().getFont();
        int firstWidth = font.width(firstPortion);
        int highlightedWidth = font.width(highlightedPortion);
        int lastWidth = font.width(lastPortion);

        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        defaultText ? DEFAULT_TEXT.getString() : firstPortion,
                        this.getX() + textPadding,
                        this.getX() + this.width - textPadding - lastWidth - highlightedWidth,
                        this.getY() + VERTICAL_OFFSET,
                        0,
                        defaultText ? CommonColors.LIGHT_GRAY : CommonColors.WHITE,
                        HorizontalAlignment.Left,
                        TextShadow.NORMAL);

        if (defaultText) return;

        FontRenderer.getInstance()
                .renderAlignedHighlightedTextInBox(
                        poseStack,
                        highlightedPortion,
                        this.getX() + textPadding + firstWidth,
                        this.getX() + this.width - textPadding - lastWidth,
                        this.getY() + VERTICAL_OFFSET,
                        this.getY() + VERTICAL_OFFSET,
                        0,
                        CommonColors.BLUE,
                        CommonColors.WHITE,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top);

        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        lastPortion,
                        this.getX() + textPadding + firstWidth + highlightedWidth,
                        this.getX() + this.width - textPadding,
                        this.getY() + VERTICAL_OFFSET,
                        0,
                        defaultText ? CommonColors.LIGHT_GRAY : CommonColors.WHITE,
                        HorizontalAlignment.Left,
                        TextShadow.NORMAL);

        drawCursor(
                poseStack,
                this.getX()
                        + font.width(renderedText.substring(0, Math.min(cursorPosition, renderedText.length())))
                        + textPadding
                        - 2,
                this.getY() + VERTICAL_OFFSET,
                VerticalAlignment.Top,
                false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= this.getX()
                && mouseX <= this.getX() + this.width
                && mouseY >= this.getY()
                && mouseY <= this.getY() + this.height) {
            McUtils.playSound(SoundEvents.UI_BUTTON_CLICK.value());
            setCursorAndHighlightPositions(getIndexAtPosition(mouseX, 0));
            isDragging = true;
            textboxScreen.setFocusedTextInput(this);
            return true;
        } else {
            textboxScreen.setFocusedTextInput(null);
        }

        return false;
    }

    @Override
    protected void removeFocus() {
        this.setTextBoxInput("");
        super.removeFocus();
    }
}
