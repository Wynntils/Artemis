/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.gui.render.FontRenderer;
import com.wynntils.gui.render.HorizontalAlignment;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.screens.TextboxScreen;
import com.wynntils.mc.objects.CommonColors;
import com.wynntils.mc.utils.McUtils;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;

public class SearchWidget extends TextInputBoxWidget {
    protected static final Component DEFAULT_TEXT =
            new TranslatableComponent("screens.wynntils.searchWidget.defaultSearchText");

    public SearchWidget(
            int x, int y, int width, int height, Consumer<String> onUpdateConsumer, TextboxScreen textboxScreen) {
        super(x, y, width, height, new TextComponent("Search Box"), onUpdateConsumer, textboxScreen);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBg(poseStack, McUtils.mc(), mouseX, mouseY);

        boolean defaultText = Objects.equals(textBoxInput, "") && !isFocused();

        String renderedText = getRenderedText(this.width - 18);

        FontRenderer.getInstance()
                .renderAlignedTextInBox(
                        poseStack,
                        defaultText ? DEFAULT_TEXT.getString() : renderedText,
                        this.x + 5,
                        this.x + this.width - 5,
                        this.y + 6.5f,
                        this.width,
                        defaultText ? CommonColors.LIGHT_GRAY : CommonColors.WHITE,
                        HorizontalAlignment.Left,
                        FontRenderer.TextShadow.NORMAL);
    }

    @Override
    protected void renderBg(PoseStack poseStack, Minecraft minecraft, int mouseX, int mouseY) {
        RenderUtils.drawRect(poseStack, CommonColors.BLACK, this.x, this.y, 0, this.width, this.height);
        RenderUtils.drawRectBorders(
                poseStack, CommonColors.GRAY, this.x, this.y, this.x + this.width, this.y + this.height, 0, 1f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height) {
            McUtils.playSound(SoundEvents.UI_BUTTON_CLICK);
            textboxScreen.setFocusedTextInput(this);

            return true;
        }

        return false;
    }

    @Override
    protected void removeFocus() {
        this.setTextBoxInput("");
        super.removeFocus();
    }
}
