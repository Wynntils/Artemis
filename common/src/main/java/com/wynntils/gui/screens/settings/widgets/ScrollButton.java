/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.screens.settings.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.Texture;
import com.wynntils.mc.objects.CustomColor;
import com.wynntils.utils.MathUtils;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TextComponent;

public class ScrollButton extends AbstractButton {
    private final Consumer<Integer> onScroll;
    private final int y2;
    private final int maxScroll;
    private final int perScrollIncrement;
    private final CustomColor scrollAreaColor;
    private int currentScroll = 0;

    private boolean dragging = false;

    public ScrollButton(
            int x,
            int y,
            int y2,
            int width,
            int height,
            int maxScroll,
            int perScrollIncrement,
            Consumer<Integer> onScroll,
            CustomColor scrollAreaColor) {
        super(x, y, width, height, new TextComponent("Scroll Button"));
        this.y2 = y2;
        this.maxScroll = maxScroll;
        this.perScrollIncrement = perScrollIncrement;
        this.onScroll = onScroll;
        this.scrollAreaColor = scrollAreaColor;
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        if (maxScroll == 0) return;

        if (scrollAreaColor != CustomColor.NONE) {
            RenderUtils.drawRect(
                    poseStack, scrollAreaColor, this.x, this.y + 2, 0, this.width, this.y2 - this.y - 4 + this.height);
        }

        float renderY = MathUtils.map(currentScroll, 0, maxScroll, y, y2);

        RenderUtils.drawHoverableTexturedRect(poseStack, Texture.SETTING_SCROLL_BUTTON, this.x, renderY, isHovered);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        float renderY = MathUtils.map(currentScroll, 0, maxScroll, y, y2);

        return mouseX >= this.x
                && mouseX <= this.x + this.width
                && mouseY >= renderY
                && mouseY <= renderY + this.height;
    }

    @Override
    public void onPress() {}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        dragging = true;
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragY == 0) return true;

        if (dragging) {
            scroll(dragY > 0 ? -1 : 1);
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll(delta);
        return true;
    }

    private void scroll(double delta) {
        onScroll.accept((int) delta * perScrollIncrement);
        currentScroll = MathUtils.clamp((int) (currentScroll - delta * perScrollIncrement), 0, maxScroll);
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {}
}
