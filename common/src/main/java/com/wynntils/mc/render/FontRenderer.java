/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.mc.mixin.accessors.MinecraftAccessor;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.utils.StringUtils;
import com.wynntils.utils.objects.CommonColors;
import com.wynntils.utils.objects.CustomColor;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;

public class FontRenderer {
    private static final FontRenderer INSTANCE = new FontRenderer();
    private final Font font;

    private static final int NEWLINE_OFFSET = 10;
    private static final CustomColor SHADOW_COLOR = CommonColors.BLACK;

    public FontRenderer() {
        this.font = ((MinecraftAccessor) McUtils.mc()).getFont();
    }

    public static FontRenderer getInstance() {
        return INSTANCE;
    }

    public int renderText(
            PoseStack poseStack,
            String text,
            float x,
            float y,
            CustomColor customColor,
            TextAlignment alignment,
            TextShadow shadow) {
        if (text == null) return 0;

        // TODO: Add rainbow color support

        switch (alignment) {
            case CENTER_ALIGNED:
                return renderText(
                        poseStack,
                        text,
                        x - font.width(text) / 2.0f,
                        y,
                        customColor,
                        TextAlignment.LEFT_ALIGNED,
                        shadow);
            case RIGHT_ALIGNED:
                return renderText(
                        poseStack, text, x - font.width(text), y, customColor, TextAlignment.LEFT_ALIGNED, shadow);
            default:
                switch (shadow) {
                    case OUTLINE:
                        for (int i = -1; i <= 1; i++) {
                            for (int j = -1; j <= 1; j++) {
                                font.draw(
                                        poseStack,
                                        ChatFormatting.stripFormatting(text),
                                        x + i,
                                        y + j,
                                        SHADOW_COLOR.withAlpha(customColor.a).asInt());
                            }
                        }
                        return font.draw(poseStack, text, x, y, customColor.asInt());
                    case NORMAL:
                        return font.drawShadow(poseStack, text, x, y, customColor.asInt());
                    default:
                        return font.draw(poseStack, text, x, y, customColor.asInt());
                }
        }
    }

    public void renderText(
            PoseStack poseStack,
            String text,
            float x,
            float y,
            float maxWidth,
            CustomColor customColor,
            TextAlignment alignment,
            TextShadow shadow) {
        if (text == null) return;

        if (maxWidth == 0 || font.width(text) < maxWidth) {
            renderText(poseStack, text, x, y, customColor, alignment, shadow);
            return;
        }

        List<String> parts = Arrays.stream(StringUtils.wrapTextBySize(text, (int) maxWidth))
                .filter(s -> !s.isBlank())
                .toList();
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            renderText(poseStack, part, x, y + (i * NEWLINE_OFFSET), customColor, alignment, shadow);
        }
    }

    private void renderText(PoseStack poseStack, float x, float y, TextRenderTask line) {
        renderText(
                poseStack,
                line.getText(),
                x,
                y,
                line.getSetting().maxWidth(),
                line.getSetting().customColor(),
                line.getSetting().alignment(),
                line.getSetting().shadow());
    }

    public void renderTexts(PoseStack poseStack, float x, float y, List<TextRenderTask> lines) {
        for (TextRenderTask line : lines) {
            renderText(poseStack, x, y, line);
            y += calculateRenderHeight(List.of(line));
        }
    }

    public void renderTextsWithAlignment(
            PoseStack poseStack,
            float renderX,
            float renderY,
            List<TextRenderTask> toRender,
            float width,
            float height,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment) {
        switch (horizontalAlignment) {
            case Center -> renderX += width / 2 / McUtils.window().getGuiScale();
            case Right -> renderX += width / McUtils.window().getGuiScale();
        }

        float renderHeight = calculateRenderHeight(toRender);

        switch (verticalAlignment) {
            case Middle -> renderY +=
                    (height - renderHeight) / 2 / McUtils.window().getGuiScale();
            case Bottom -> renderY += (height - renderHeight) / McUtils.window().getGuiScale();
        }

        renderTexts(poseStack, renderX, renderY, toRender);
    }

    private float calculateRenderHeight(List<TextRenderTask> toRender) {
        if (toRender.isEmpty()) return 0f;

        float height = 0;
        for (TextRenderTask textRenderTask : toRender) {
            if (textRenderTask.getSetting().maxWidth() == 0) {
                height += font.lineHeight + NEWLINE_OFFSET;
            } else {
                int lines = 1;
                if (textRenderTask.getText().contains(" ")) {
                    lines = Arrays.stream(StringUtils.wrapTextBySize(textRenderTask.getText(), (int)
                                    textRenderTask.getSetting().maxWidth()))
                            .filter(s -> !s.isBlank())
                            .toList()
                            .size();
                }

                height += lines * (font.lineHeight + NEWLINE_OFFSET);
            }
        }

        return (float) (height - NEWLINE_OFFSET / 2 * McUtils.window().getGuiScale());
    }

    public enum TextAlignment {
        LEFT_ALIGNED,
        CENTER_ALIGNED,
        RIGHT_ALIGNED
    }

    public enum TextShadow {
        NONE,
        NORMAL,
        OUTLINE
    }
}
