/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.render.buffered;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.Texture;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class BufferedRenderUtils {
    public static void drawLine(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CustomColor color,
            float x1,
            float y1,
            float x2,
            float y2,
            float z,
            float width) {
        Matrix4f matrix = poseStack.last().pose();

        float halfWidth = width / 2;

        VertexConsumer buffer = bufferSource.getBuffer(CustomRenderType.POSITION_COLOR_TRIANGLE_STRIP);

        if (x1 == x2) {
            if (y2 < y1) {
                float tmp = y1;
                y1 = y2;
                y2 = tmp;
            }
            buffer.addVertex(matrix, x1 - halfWidth, y1, z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex(matrix, x2 - halfWidth, y2, z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex(matrix, x1 + halfWidth, y1, z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex(matrix, x2 + halfWidth, y2, z).setColor(color.r, color.g, color.b, color.a);
        } else if (y1 == y2) {
            if (x2 < x1) {
                float tmp = x1;
                x1 = x2;
                x2 = tmp;
            }

            buffer.addVertex(matrix, x1, y1 - halfWidth, z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex(matrix, x1, y1 + halfWidth, z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex(matrix, x2, y2 - halfWidth, z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex(matrix, x2, y2 + halfWidth, z).setColor(color.r, color.g, color.b, color.a);
        } else if ((x1 < x2 && y1 < y2) || (x2 < x1 && y2 < y1)) { // Top Left to Bottom Right line
            if (x2 < x1) {
                float tmp = x1;
                x1 = x2;
                x2 = tmp;

                tmp = y1;
                y1 = y2;
                y2 = tmp;
            }

            buffer.addVertex(matrix, x1 + halfWidth, y1 - halfWidth, z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex(matrix, x1 - halfWidth, y1 + halfWidth, z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex(matrix, x2 + halfWidth, y2 - halfWidth, z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex(matrix, x2 - halfWidth, y2 + halfWidth, z).setColor(color.r, color.g, color.b, color.a);
        } else { // Top Right to Bottom Left Line
            if (x1 < x2) {
                float tmp = x1;
                x1 = x2;
                x2 = tmp;

                tmp = y1;
                y1 = y2;
                y2 = tmp;
            }

            buffer.addVertex(matrix, x1 + halfWidth, y1 + halfWidth, z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex(matrix, x1 - halfWidth, y1 - halfWidth, z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex(matrix, x2 + halfWidth, y2 + halfWidth, z).setColor(color.r, color.g, color.b, color.a);
            buffer.addVertex(matrix, x2 - halfWidth, y2 - halfWidth, z).setColor(color.r, color.g, color.b, color.a);
        }
    }

    public static void drawRectBorders(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CustomColor color,
            float x1,
            float y1,
            float x2,
            float y2,
            float z,
            float lineWidth) {
        drawLine(poseStack, bufferSource, color, x1, y1, x2, y1, z, lineWidth);
        drawLine(poseStack, bufferSource, color, x2, y1, x2, y2, z, lineWidth);
        drawLine(poseStack, bufferSource, color, x2, y2, x1, y2, z, lineWidth);
        drawLine(poseStack, bufferSource, color, x1, y2, x1, y1, z, lineWidth);
    }

    public static void drawRect(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            CustomColor color,
            float x,
            float y,
            float z,
            float width,
            float height) {
        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer buffer = bufferSource.getBuffer(CustomRenderType.POSITION_COLOR_QUAD);

        buffer.addVertex(matrix, x, y + height, z).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(matrix, x + width, y + height, z).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(matrix, x + width, y, z).setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(matrix, x, y, z).setColor(color.r, color.g, color.b, color.a);
    }

    public static void drawMulticoloredRectBorders(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            List<CustomColor> colors,
            float x,
            float y,
            float z,
            float width,
            float height,
            float externalLineWidth,
            float internalLineWidth) {
        if (colors.size() == 1) {
            drawRectBorders(
                    poseStack, bufferSource, colors.getFirst(), x, y, x + width, y + height, z, externalLineWidth);
            return;
        }
        float splitX = width / (colors.size() - 1);

        for (int i = 0; i < colors.size(); i++) {
            CustomColor color = colors.get(i);
            float leftX = Mth.clamp(x + splitX * (i - 1), x, x + width);
            float centerX = Mth.clamp(x + splitX * i, x, x + width);
            float rightX = Mth.clamp(x + splitX * (i + 1), x, x + width);

            // bottom left to bottom center (always drawn)
            drawLine(poseStack, bufferSource, color, leftX, y + height, centerX, y + height, z, externalLineWidth);
            // bottom center to top right (drawn on i!=colors.size()-1)
            drawLine(
                    poseStack,
                    bufferSource,
                    color,
                    centerX,
                    y + height,
                    rightX,
                    y,
                    z,
                    (i != colors.size() - 1 ? internalLineWidth : externalLineWidth));
            // top right to top center (always drawn)
            drawLine(poseStack, bufferSource, color, rightX, y, centerX, y, z, externalLineWidth);
            // top center to bottom left (drawn on i!=0)
            drawLine(
                    poseStack,
                    bufferSource,
                    color,
                    centerX,
                    y,
                    leftX,
                    y + height,
                    z,
                    (i != 0 ? internalLineWidth : externalLineWidth));
        }
    }

    /**
     * Draws a rectangle with multiple colors, each being an equal-ish portion of the rectangle as a parallelogram
     */
    public static void drawMulticoloredRect(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            List<CustomColor> colors,
            float x,
            float y,
            float z,
            float width,
            float height) {
        if (colors.size() == 1) {
            drawRect(poseStack, bufferSource, colors.getFirst(), x, y, z, width, height);
            return;
        }
        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer buffer = bufferSource.getBuffer(CustomRenderType.POSITION_COLOR_QUAD);

        float splitX = width / (colors.size() - 1);

        for (int i = 0; i < colors.size(); i++) {
            CustomColor color = colors.get(i);
            float leftX = Mth.clamp(x + splitX * (i - 1), x, x + width);
            float centerX = Mth.clamp(x + splitX * i, x, x + width);
            float rightX = Mth.clamp(x + splitX * (i + 1), x, x + width);

            // bottom left
            buffer.addVertex(matrix, leftX, y + height, z).setColor(color.r, color.g, color.b, color.a);
            // bottom right
            buffer.addVertex(matrix, centerX, y + height, z).setColor(color.r, color.g, color.b, color.a);
            // top right
            buffer.addVertex(matrix, rightX, y, z).setColor(color.r, color.g, color.b, color.a);
            // top left
            buffer.addVertex(matrix, centerX, y, z).setColor(color.r, color.g, color.b, color.a);
        }
    }

    public static void drawColoredTexturedRect(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ResourceLocation tex,
            CustomColor color,
            float alpha,
            float x,
            float y,
            float z,
            float width,
            float height) {
        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer buffer = bufferSource.getBuffer(CustomRenderType.getPositionColorTextureQuad(tex));

        float[] colorArray = color.asFloatArray();

        buffer.addVertex(matrix, x, y + height, z)
                .setUv(0, 1)
                .setColor(colorArray[0], colorArray[1], colorArray[2], alpha);
        buffer.addVertex(matrix, x + width, y + height, z)
                .setUv(1, 1)
                .setColor(colorArray[0], colorArray[1], colorArray[2], alpha);
        buffer.addVertex(matrix, x + width, y, z)
                .setUv(1, 0)
                .setColor(colorArray[0], colorArray[1], colorArray[2], alpha);
        buffer.addVertex(matrix, x, y, z).setUv(0, 0).setColor(colorArray[0], colorArray[1], colorArray[2], alpha);
    }

    public static void drawTexturedRect(
            PoseStack poseStack, MultiBufferSource bufferSource, Texture texture, float x, float y) {
        drawTexturedRect(
                poseStack,
                bufferSource,
                texture.resource(),
                x,
                y,
                texture.width(),
                texture.height(),
                texture.width(),
                texture.height());
    }

    private static void drawTexturedRect(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ResourceLocation tex,
            float x,
            float y,
            float width,
            float height,
            int textureWidth,
            int textureHeight) {
        drawTexturedRect(
                poseStack,
                bufferSource,
                tex,
                x,
                y,
                0,
                width,
                height,
                0,
                0,
                (int) width,
                (int) height,
                textureWidth,
                textureHeight);
    }

    public static void drawTexturedRect(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ResourceLocation tex,
            float x,
            float y,
            float z,
            float width,
            float height,
            int uOffset,
            int vOffset,
            int u,
            int v,
            int textureWidth,
            int textureHeight) {
        float uScale = 1f / textureWidth;
        float vScale = 1f / textureHeight;

        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer buffer = bufferSource.getBuffer(CustomRenderType.getPositionTextureQuad(tex));

        buffer.addVertex(matrix, x, y + height, z).setUv(uOffset * uScale, (vOffset + v) * vScale);
        buffer.addVertex(matrix, x + width, y + height, z).setUv((uOffset + u) * uScale, (vOffset + v) * vScale);
        buffer.addVertex(matrix, x + width, y, z).setUv((uOffset + u) * uScale, vOffset * vScale);
        buffer.addVertex(matrix, x, y, z).setUv(uOffset * uScale, vOffset * vScale);
    }

    public static void drawTexturedRectWithColor(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            ResourceLocation tex,
            CustomColor color,
            float x,
            float y,
            float z,
            float width,
            float height,
            int uOffset,
            int vOffset,
            int u,
            int v,
            int textureWidth,
            int textureHeight) {
        float uScale = 1f / textureWidth;
        float vScale = 1f / textureHeight;

        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer buffer = bufferSource.getBuffer(CustomRenderType.getPositionColorTextureQuad(tex));

        buffer.addVertex(matrix, x, y + height, z)
                .setUv(uOffset * uScale, (vOffset + v) * vScale)
                .setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(matrix, x + width, y + height, z)
                .setUv((uOffset + u) * uScale, (vOffset + v) * vScale)
                .setColor(color.r, color.g, color.b, color.a);
        buffer.addVertex(matrix, x + width, y, z)
                .setUv((uOffset + u) * uScale, vOffset * vScale)
                .setColor(color.r, color.g, color.b, color.a);

        buffer.addVertex(matrix, x, y, z)
                .setUv(uOffset * uScale, vOffset * vScale)
                .setColor(color.r, color.g, color.b, color.a);
    }

    /**
     * drawProgressBar
     * Draws a progress bar (textureY1 and textureY2 now specify both textures with background being on top of the bar)
     *
     * @param poseStack poseStack to use
     * @param bufferSource bufferSource to use
     * @param texture   the texture to use
     * @param customColor the color for the bar
     * @param x1        left x on screen
     * @param y1        top y on screen
     * @param x2        right x on screen
     * @param y2        bottom right on screen
     * @param textureX1 texture left x for the part
     * @param textureY1 texture top y for the part (top of background)
     * @param textureX2 texture right x for the part
     * @param textureY2 texture bottom y for the part (bottom of bar)
     * @param progress  progress of the bar, 0.0f to 1.0f is left to right and 0.0f to -1.0f is right to left
     */
    public static void drawColoredProgressBar(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Texture texture,
            CustomColor customColor,
            float x1,
            float y1,
            float x2,
            float y2,
            int textureX1,
            int textureY1,
            int textureX2,
            int textureY2,
            float progress) {
        int half = (textureY1 + textureY2) / 2 + (textureY2 - textureY1) % 2;
        drawProgressBarBackground(
                poseStack, bufferSource, texture, x1, y1, x2, y2, textureX1, textureY1, textureX2, half);
        drawProgressBarForegroundWithColor(
                poseStack,
                bufferSource,
                texture,
                customColor,
                x1,
                y1,
                x2,
                y2,
                textureX1,
                half,
                textureX2,
                textureY2 + (textureY2 - textureY1) % 2,
                progress);
    }

    private static void drawProgressBarForegroundWithColor(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Texture texture,
            CustomColor customColor,
            float x1,
            float y1,
            float x2,
            float y2,
            int textureX1,
            int textureY1,
            int textureX2,
            int textureY2,
            float progress) {
        if (progress == 0f) {
            return;
        }

        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer buffer =
                bufferSource.getBuffer(CustomRenderType.getPositionColorTextureQuad(texture.resource()));

        float xMin = Math.min(x1, x2),
                xMax = Math.max(x1, x2),
                yMin = Math.min(y1, y2),
                yMax = Math.max(y1, y2),
                txMin = (float) Math.min(textureX1, textureX2) / texture.width(),
                txMax = (float) Math.max(textureX1, textureX2) / texture.width(),
                tyMin = (float) Math.min(textureY1, textureY2) / texture.height(),
                tyMax = (float) Math.max(textureY1, textureY2) / texture.height();

        if (progress < 1.0f && progress > -1.0f) {
            if (progress < 0.0f) {
                xMin += (1.0f + progress) * (xMax - xMin);
                txMin += (1.0f + progress) * (txMax - txMin);
            } else {
                xMax -= (1.0f - progress) * (xMax - xMin);
                txMax -= (1.0f - progress) * (txMax - txMin);
            }
        }

        buffer.addVertex(matrix, xMin, yMin, 0).setUv(txMin, tyMin).setColor(customColor.asInt());
        buffer.addVertex(matrix, xMin, yMax, 0).setUv(txMin, tyMax).setColor(customColor.asInt());
        buffer.addVertex(matrix, xMax, yMax, 0).setUv(txMax, tyMax).setColor(customColor.asInt());
        buffer.addVertex(matrix, xMax, yMin, 0).setUv(txMax, tyMin).setColor(customColor.asInt());
    }

    /**
     * drawProgressBar
     * Draws a progress bar (textureY1 and textureY2 now specify both textures with background being on top of the bar)
     *
     * @param poseStack poseStack to use
     * @param bufferSource bufferSource to use
     * @param texture   the texture to use
     * @param x1        left x on screen
     * @param y1        top y on screen
     * @param x2        right x on screen
     * @param y2        bottom right on screen
     * @param textureX1 texture left x for the part
     * @param textureY1 texture top y for the part (top of background)
     * @param textureX2 texture right x for the part
     * @param textureY2 texture bottom y for the part (bottom of bar)
     * @param progress  progress of the bar, 0.0f to 1.0f is left to right and 0.0f to -1.0f is right to left
     */
    public static void drawProgressBar(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Texture texture,
            float x1,
            float y1,
            float x2,
            float y2,
            int textureX1,
            int textureY1,
            int textureX2,
            int textureY2,
            float progress) {
        int half = (textureY1 + textureY2) / 2 + (textureY2 - textureY1) % 2;
        drawProgressBarBackground(
                poseStack, bufferSource, texture, x1, y1, x2, y2, textureX1, textureY1, textureX2, half);
        drawProgressBarForeground(
                poseStack,
                bufferSource,
                texture,
                x1,
                y1,
                x2,
                y2,
                textureX1,
                half,
                textureX2,
                textureY2 + (textureY2 - textureY1) % 2,
                progress);
    }

    public static void drawProgressBarForeground(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Texture texture,
            float x1,
            float y1,
            float x2,
            float y2,
            int textureX1,
            int textureY1,
            int textureX2,
            int textureY2,
            float progress) {
        if (progress == 0f) {
            return;
        }

        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer buffer = bufferSource.getBuffer(CustomRenderType.getPositionTextureQuad(texture.resource()));

        float xMin = Math.min(x1, x2),
                xMax = Math.max(x1, x2),
                yMin = Math.min(y1, y2),
                yMax = Math.max(y1, y2),
                txMin = (float) Math.min(textureX1, textureX2) / texture.width(),
                txMax = (float) Math.max(textureX1, textureX2) / texture.width(),
                tyMin = (float) Math.min(textureY1, textureY2) / texture.height(),
                tyMax = (float) Math.max(textureY1, textureY2) / texture.height();

        if (progress < 1.0f && progress > -1.0f) {
            if (progress < 0.0f) {
                xMin += (1.0f + progress) * (xMax - xMin);
                txMin += (1.0f + progress) * (txMax - txMin);
            } else {
                xMax -= (1.0f - progress) * (xMax - xMin);
                txMax -= (1.0f - progress) * (txMax - txMin);
            }
        }

        buffer.addVertex(matrix, xMin, yMin, 0).setUv(txMin, tyMin);
        buffer.addVertex(matrix, xMin, yMax, 0).setUv(txMin, tyMax);
        buffer.addVertex(matrix, xMax, yMax, 0).setUv(txMax, tyMax);
        buffer.addVertex(matrix, xMax, yMin, 0).setUv(txMax, tyMin);
    }

    public static void drawProgressBarBackground(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Texture texture,
            float x1,
            float y1,
            float x2,
            float y2,
            int textureX1,
            int textureY1,
            int textureX2,
            int textureY2) {
        Matrix4f matrix = poseStack.last().pose();

        VertexConsumer buffer = bufferSource.getBuffer(CustomRenderType.getPositionTextureQuad(texture.resource()));

        float xMin = Math.min(x1, x2),
                xMax = Math.max(x1, x2),
                yMin = Math.min(y1, y2),
                yMax = Math.max(y1, y2),
                txMin = (float) Math.min(textureX1, textureX2) / texture.width(),
                txMax = (float) Math.max(textureX1, textureX2) / texture.width(),
                tyMin = (float) Math.min(textureY1, textureY2) / texture.height(),
                tyMax = (float) Math.max(textureY1, textureY2) / texture.height();

        buffer.addVertex(matrix, xMin, yMin, 0).setUv(txMin, tyMin);
        buffer.addVertex(matrix, xMin, yMax, 0).setUv(txMin, tyMax);
        buffer.addVertex(matrix, xMax, yMax, 0).setUv(txMax, tyMax);
        buffer.addVertex(matrix, xMax, yMin, 0).setUv(txMax, tyMin);
    }

    public static void createMask(
            PoseStack poseStack, MultiBufferSource bufferSource, Texture texture, int x1, int y1, int x2, int y2) {
        createMask(poseStack, bufferSource, texture, x1, y1, x2, y2, 0, 0, texture.width(), texture.height());
    }

    /**
     * Creates a mask that will remove anything drawn after
     * this and before the next {clearMask()}(or {endGL()})
     * and is not inside the mask.
     * A mask, is a clear and white texture where anything
     * white will allow drawing.
     *
     * @param texture mask texture(please use Textures.Masks)
     * @param x1 bottom-left x(on screen)
     * @param y1 bottom-left y(on screen)
     * @param x2 top-right x(on screen)
     * @param y2 top-right y(on screen)
     */
    private static void createMask(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Texture texture,
            float x1,
            float y1,
            float x2,
            float y2,
            int tx1,
            int ty1,
            int tx2,
            int ty2) {
        // See https://gist.github.com/burgerguy/8233170683ad93eea6aa27ee02a5c4d1

        GL11.glEnable(GL11.GL_STENCIL_TEST);

        // Enable writing to stencil
        RenderSystem.stencilMask(0xff);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, true);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        // Disable writing to color or depth
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);

        // Draw textured image
        int width = texture.width();
        int height = texture.height();
        drawTexturedRect(
                poseStack,
                bufferSource,
                texture.resource(),
                x1,
                y1,
                0f,
                x2 - x1,
                y2 - y1,
                tx1,
                ty1,
                tx2 - tx1,
                ty2 - ty1,
                width,
                height);

        // Reenable color and depth
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);

        // Only write to stencil area
        RenderSystem.stencilMask(0x00);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xff);
    }
}
