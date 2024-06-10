/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.render.buffered;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.mixin.accessors.MinecraftAccessor;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.wynntils.utils.type.IterationDecision;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public final class BufferedFontRenderer {
    private static final BufferedFontRenderer INSTANCE = new BufferedFontRenderer();
    private final Font font;

    private static final int NEWLINE_OFFSET = 10;
    private static final CustomColor SHADOW_COLOR = CommonColors.BLACK;

    private BufferedFontRenderer() {
        this.font = ((MinecraftAccessor) McUtils.mc()).getFont();
    }

    public static BufferedFontRenderer getInstance() {
        return INSTANCE;
    }

    public Font getFont() {
        return font;
    }

    public void renderText(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            StyledText text,
            float x,
            float y,
            CustomColor customColor,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment,
            TextShadow shadow,
            float textScale) {
        float renderX;
        float renderY;

        if (text == null) return;

        // TODO: Add rainbow color support

        renderX = switch (horizontalAlignment) {
            case LEFT -> x;
            case CENTER -> x - (font.width(text.getString()) / 2f * textScale);
            case RIGHT -> x - font.width(text.getString()) * textScale;
        };

        renderY = switch (verticalAlignment) {
            case TOP -> y;
            case MIDDLE -> y - (font.lineHeight / 2f * textScale);
            case BOTTOM -> y - font.lineHeight * textScale;
        };

        poseStack.pushPose();
        poseStack.translate(renderX, renderY, 0);
        poseStack.scale(textScale, textScale, 0);

        switch (shadow) {
            case NONE -> font.drawInBatch(
                    text.getString(),
                    0,
                    0,
                    customColor.asInt(),
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    0xF000F0,
                    font.isBidirectional());
            case NORMAL -> font.drawInBatch(
                    text.getString(),
                    0,
                    0,
                    customColor.asInt(),
                    true,
                    poseStack.last().pose(),
                    bufferSource,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    0xF000F0,
                    font.isBidirectional());
            case OUTLINE -> {
                int shadowColor = SHADOW_COLOR.withAlpha(customColor.a).asInt();
                String strippedText = text.iterate((part, changes) -> {
                            changes.remove(part);
                            changes.add(part.withStyle(partStyle -> partStyle.withColor(ChatFormatting.BLACK)));
                            return IterationDecision.CONTINUE;
                        })
                        .getString();

                font.drawInBatch(
                        strippedText,
                        -1,
                        0,
                        shadowColor,
                        false,
                        poseStack.last().pose(),
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0,
                        0xF000F0,
                        font.isBidirectional());
                font.drawInBatch(
                        strippedText,
                        1,
                        0,
                        shadowColor,
                        false,
                        poseStack.last().pose(),
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0,
                        0xF000F0,
                        font.isBidirectional());
                font.drawInBatch(
                        strippedText,
                        0,
                        -1,
                        shadowColor,
                        false,
                        poseStack.last().pose(),
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0,
                        0xF000F0,
                        font.isBidirectional());
                font.drawInBatch(
                        strippedText,
                        0,
                        1,
                        shadowColor,
                        false,
                        poseStack.last().pose(),
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0,
                        0xF000F0,
                        font.isBidirectional());

                font.drawInBatch(
                        text.getString(),
                        0,
                        0,
                        customColor.asInt(),
                        false,
                        poseStack.last().pose(),
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0,
                        0xF000F0,
                        font.isBidirectional());
            }
        }

        poseStack.popPose();
    }

    public void renderAlignedTextInBox(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            StyledText text,
            float x1,
            float x2,
            float y1,
            float y2,
            float maxWidth,
            CustomColor customColor,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment,
            TextShadow textShadow,
            float textScale) {
        float renderX =
                switch (horizontalAlignment) {
                    case LEFT -> x1;
                    case CENTER -> (x1 + x2) / 2f;
                    case RIGHT -> x2;
                };

        float renderY =
                switch (verticalAlignment) {
                    case TOP -> y1;
                    case MIDDLE -> (y1 + y2) / 2f;
                    case BOTTOM -> y2;
                };

        renderText(
                poseStack,
                bufferSource,
                text,
                renderX,
                renderY,
                maxWidth,
                customColor,
                horizontalAlignment,
                verticalAlignment,
                textShadow,
                textScale);
    }

    public void renderAlignedTextInBox(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            StyledText text,
            float x1,
            float x2,
            float y1,
            float y2,
            float maxWidth,
            CustomColor customColor,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment,
            TextShadow textShadow) {
        renderAlignedTextInBox(
                poseStack,
                bufferSource,
                text,
                x1,
                x2,
                y1,
                y2,
                maxWidth,
                customColor,
                horizontalAlignment,
                verticalAlignment,
                textShadow,
                1f);
    }

    public void renderAlignedTextInBox(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            StyledText text,
            float x1,
            float x2,
            float y,
            float maxWidth,
            CustomColor customColor,
            HorizontalAlignment horizontalAlignment,
            TextShadow textShadow) {
        renderAlignedTextInBox(
                poseStack,
                bufferSource,
                text,
                x1,
                x2,
                y,
                y,
                maxWidth,
                customColor,
                horizontalAlignment,
                VerticalAlignment.TOP,
                textShadow,
                1f);
    }

    public void renderAlignedTextInBox(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            StyledText text,
            float x,
            float y1,
            float y2,
            float maxWidth,
            CustomColor customColor,
            VerticalAlignment verticalAlignment,
            TextShadow textShadow) {
        renderAlignedTextInBox(
                poseStack,
                bufferSource,
                text,
                x,
                x,
                y1,
                y2,
                maxWidth,
                customColor,
                HorizontalAlignment.LEFT,
                verticalAlignment,
                textShadow,
                1f);
    }

    public void renderText(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            StyledText text,
            float x,
            float y,
            CustomColor customColor,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment,
            TextShadow shadow) {
        renderText(
                poseStack, bufferSource, text, x, y, customColor, horizontalAlignment, verticalAlignment, shadow, 1f);
    }

    public void renderText(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            StyledText text,
            float x,
            float y,
            float maxWidth,
            CustomColor customColor,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment,
            TextShadow shadow,
            float textScale) {
        if (text == null) return;

        if (maxWidth == 0 || font.width(text.getString()) < maxWidth / textScale) {
            renderText(
                    poseStack,
                    bufferSource,
                    text,
                    x,
                    y,
                    customColor,
                    horizontalAlignment,
                    verticalAlignment,
                    shadow,
                    textScale);
            return;
        }

        List<FormattedText> parts =
                font.getSplitter().splitLines(text.getComponent(), (int) (maxWidth / textScale), Style.EMPTY);

        StyledText lastPart = StyledText.EMPTY;
        for (int i = 0; i < parts.size(); i++) {
            // copy the format codes to this part as well
            Style lastStyle = ComponentUtils.getLastPartCodes(lastPart);

            StyledText part = StyledText.fromComponent(Component.literal("").withStyle(lastStyle))
                    .append(StyledText.fromComponent(ComponentUtils.formattedTextToComponent(parts.get(i))));
            lastPart = part;

            renderText(
                    poseStack,
                    bufferSource,
                    part,
                    x,
                    y + (i * font.lineHeight * textScale),
                    customColor,
                    horizontalAlignment,
                    verticalAlignment,
                    shadow,
                    textScale);
        }
    }

    public void renderTextsWithAlignment(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float x,
            float y,
            List<TextRenderTask> toRender,
            float width,
            float height,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment) {
        renderTextsWithAlignment(
                poseStack, bufferSource, x, y, toRender, width, height, horizontalAlignment, verticalAlignment, 1f);
    }

    public void renderTextsWithAlignment(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float x,
            float y,
            List<TextRenderTask> toRender,
            float width,
            float height,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment,
            float textScale) {
        float renderX =
                switch (horizontalAlignment) {
                    case LEFT -> x;
                    case CENTER -> x + width / 2;
                    case RIGHT -> x + width;
                };

        float renderY =
                switch (verticalAlignment) {
                    case TOP -> y;
                    case MIDDLE -> y + (height - FontRenderer.getInstance().calculateRenderHeight(toRender)) / 2;
                    case BOTTOM -> y + (height - FontRenderer.getInstance().calculateRenderHeight(toRender));
                };

        renderTexts(poseStack, bufferSource, renderX, renderY, toRender, textScale);
    }

    public void renderTexts(
            PoseStack poseStack, MultiBufferSource bufferSource, float x, float y, List<TextRenderTask> lines) {
        renderTexts(poseStack, bufferSource, x, y, lines, 1f);
    }

    private void renderTexts(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float x,
            float y,
            List<TextRenderTask> lines,
            float textScale) {
        float currentY = y;
        for (TextRenderTask line : lines) {
            renderText(poseStack, bufferSource, x, currentY, line, textScale);
            currentY += FontRenderer.getInstance()
                            .calculateRenderHeight(
                                    line.getText(), line.getSetting().maxWidth() / textScale)
                    * textScale;
        }
    }

    public void renderText(PoseStack poseStack, MultiBufferSource bufferSource, float x, float y, TextRenderTask line) {
        renderText(
                poseStack,
                bufferSource,
                line.getText(),
                x,
                y,
                line.getSetting().maxWidth(),
                line.getSetting().customColor(),
                line.getSetting().horizontalAlignment(),
                line.getSetting().verticalAlignment(),
                line.getSetting().shadow());
    }

    private void renderText(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float x,
            float y,
            TextRenderTask line,
            float textScale) {
        renderText(
                poseStack,
                bufferSource,
                line.getText(),
                x,
                y,
                line.getSetting().maxWidth(),
                line.getSetting().customColor(),
                line.getSetting().horizontalAlignment(),
                line.getSetting().verticalAlignment(),
                line.getSetting().shadow(),
                textScale);
    }

    private void renderText(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            StyledText text,
            float x,
            float y,
            float maxWidth,
            CustomColor customColor,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment,
            TextShadow shadow) {
        renderText(
                poseStack,
                bufferSource,
                text,
                x,
                y,
                maxWidth,
                customColor,
                horizontalAlignment,
                verticalAlignment,
                shadow,
                1f);
    }

    public void renderTextWithAlignment(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float renderX,
            float renderY,
            TextRenderTask toRender,
            float width,
            float height,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment) {
        renderTextsWithAlignment(
                poseStack,
                bufferSource,
                renderX,
                renderY,
                List.of(toRender),
                width,
                height,
                horizontalAlignment,
                verticalAlignment);
    }
}
