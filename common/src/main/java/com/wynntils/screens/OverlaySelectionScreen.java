/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.mc.render.FontRenderer;
import com.wynntils.mc.render.Texture;
import com.wynntils.screens.lists.OverlayList;
import com.wynntils.utils.objects.CommonColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.TranslatableComponent;

public class OverlaySelectionScreen extends Screen {
    private OverlayList overlayList;

    public OverlaySelectionScreen() {
        super(new TranslatableComponent("screens.wynntils.overlaySelection.name"));
    }

    @Override
    protected void init() {
        overlayList = new OverlayList(this);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        poseStack.pushPose();

        this.fillGradient(poseStack, 0, 0, this.width, this.height, -1072689136, -804253680);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Texture.OVERLAY_SELECTION_GUI.resource());
        int x = (this.width - Texture.OVERLAY_SELECTION_GUI.width()) / 2;
        int y = this.height / 10;

        poseStack.translate(x, y, 0);

        blit(
                poseStack,
                0,
                0,
                0,
                0,
                Texture.OVERLAY_SELECTION_GUI.width(),
                Texture.OVERLAY_SELECTION_GUI.height(),
                Texture.OVERLAY_SELECTION_GUI.width(),
                Texture.OVERLAY_SELECTION_GUI.height());

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        "Overlays",
                        5,
                        4,
                        CommonColors.WHITE,
                        FontRenderer.TextAlignment.LEFT_ALIGNED,
                        FontRenderer.TextShadow.NORMAL);

        poseStack.popPose();

        overlayList.render(poseStack, mouseX, mouseY, partialTick);

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.init();
        super.resize(minecraft, width, height);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return overlayList.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return overlayList.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return overlayList.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return overlayList.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && this.shouldCloseOnEsc()) {
            this.onClose();
            return true;
        }

        return overlayList.keyPressed(keyCode, scanCode, modifiers);
    }
}
