/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.characterselector;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.text.StyledText2;
import com.wynntils.screens.base.WynntilsScreen;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class LoadingScreen extends WynntilsScreen {
    private static final String LOGO_STRING = "\u2060\u2064\u2061";
    private static final CustomColor MOSS_GREEN = CustomColor.fromInt(0x527529).withAlpha(255);
    public static final int SPINNER_SPEED = 1200;

    private String message;

    private LoadingScreen() {
        super(Component.translatable("screens.wynntils.characterSelection.name"));
    }

    public static LoadingScreen create() {
        return new LoadingScreen();
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void doRender(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        // Draw background
        RenderUtils.drawScalingTexturedRect(
                poseStack,
                Texture.BACKGROUND_SPLASH.resource(),
                0,
                0,
                0,
                this.width,
                this.height,
                Texture.BACKGROUND_SPLASH.width(),
                Texture.BACKGROUND_SPLASH.height());

        poseStack.pushPose();

        // Draw notebook background
        poseStack.translate(
                (this.width - Texture.CHANGELOG_BACKGROUND.width()) / 2f,
                (this.height - Texture.CHANGELOG_BACKGROUND.height()) / 2f,
                0);

        RenderUtils.drawTexturedRect(poseStack, Texture.CHANGELOG_BACKGROUND, 0, 0);

        // Draw logo
        int centerX = Texture.CHANGELOG_BACKGROUND.width() / 2 + 15;
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText2.of(LOGO_STRING),
                        centerX,
                        60,
                        CommonColors.WHITE,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.TOP,
                        TextShadow.NONE);

        // Draw loading progress
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText2.of(message),
                        centerX,
                        120,
                        MOSS_GREEN,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.TOP,
                        TextShadow.NONE);

        // Draw spinner
        boolean state = (System.currentTimeMillis() % SPINNER_SPEED) < SPINNER_SPEED / 2;
        drawSpinner(poseStack, centerX, 150, state);

        poseStack.popPose();
    }

    private void drawSpinner(PoseStack poseStack, float x, float y, boolean state) {
        ResourceLocation resource = Texture.RELOAD_BUTTON.resource();

        int fullWidth = Texture.RELOAD_BUTTON.width();
        int width = fullWidth / 2;
        int height = Texture.RELOAD_BUTTON.height();
        int uOffset = state ? width : 0;

        RenderUtils.drawTexturedRect(
                poseStack, resource, x - width / 2, y, 0, width, height, uOffset, 0, width, height, fullWidth, height);
    }
}
