/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

public final class RenderUtils {
    // used to render player nametags as semi-transparent
    private static final int NAMETAG_COLOR = 0x20FFFFFF;

    // number of possible segments for arc drawing
    private static final float MAX_CIRCLE_STEPS = 16f;

    // See https://github.com/MinecraftForge/MinecraftForge/issues/8083 as to why this uses TRIANGLE_STRIPS.
    // TLDR: New OpenGL only supports TRIANGLES and Minecraft patched QUADS to be usable ATM, but LINES patch is broken,
    // and you can't use it.
    // (This also means that using QUADS is probably not the best idea)
    public static void drawLine(
            PoseStack poseStack, CustomColor color, float x1, float y1, float x2, float y2, float z, float width) {
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferBuilder =
                Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float halfWidth = width / 2;

        if (x1 == x2) {
            if (y2 < y1) {
                float tmp = y1;
                y1 = y2;
                y2 = tmp;
            }
            bufferBuilder.addVertex(matrix, x1 - halfWidth, y1, z).setColor(color.r, color.g, color.b, color.a);
            bufferBuilder.addVertex(matrix, x2 - halfWidth, y2, z).setColor(color.r, color.g, color.b, color.a);
            bufferBuilder.addVertex(matrix, x1 + halfWidth, y1, z).setColor(color.r, color.g, color.b, color.a);
            bufferBuilder.addVertex(matrix, x2 + halfWidth, y2, z).setColor(color.r, color.g, color.b, color.a);
        } else if (y1 == y2) {
            if (x2 < x1) {
                float tmp = x1;
                x1 = x2;
                x2 = tmp;
            }

            bufferBuilder.addVertex(matrix, x1, y1 - halfWidth, z).setColor(color.r, color.g, color.b, color.a);
            bufferBuilder.addVertex(matrix, x1, y1 + halfWidth, z).setColor(color.r, color.g, color.b, color.a);
            bufferBuilder.addVertex(matrix, x2, y2 - halfWidth, z).setColor(color.r, color.g, color.b, color.a);
            bufferBuilder.addVertex(matrix, x2, y2 + halfWidth, z).setColor(color.r, color.g, color.b, color.a);
        } else if ((x1 < x2 && y1 < y2) || (x2 < x1 && y2 < y1)) { // Top Left to Bottom Right line
            if (x2 < x1) {
                float tmp = x1;
                x1 = x2;
                x2 = tmp;

                tmp = y1;
                y1 = y2;
                y2 = tmp;
            }

            bufferBuilder
                    .addVertex(matrix, x1 + halfWidth, y1 - halfWidth, z)
                    .setColor(color.r, color.g, color.b, color.a);
            bufferBuilder
                    .addVertex(matrix, x1 - halfWidth, y1 + halfWidth, z)
                    .setColor(color.r, color.g, color.b, color.a);
            bufferBuilder
                    .addVertex(matrix, x2 + halfWidth, y2 - halfWidth, z)
                    .setColor(color.r, color.g, color.b, color.a);
            bufferBuilder
                    .addVertex(matrix, x2 - halfWidth, y2 + halfWidth, z)
                    .setColor(color.r, color.g, color.b, color.a);
        } else { // Top Right to Bottom Left Line
            if (x1 < x2) {
                float tmp = x1;
                x1 = x2;
                x2 = tmp;

                tmp = y1;
                y1 = y2;
                y2 = tmp;
            }

            bufferBuilder
                    .addVertex(matrix, x1 + halfWidth, y1 + halfWidth, z)
                    .setColor(color.r, color.g, color.b, color.a);
            bufferBuilder
                    .addVertex(matrix, x1 - halfWidth, y1 - halfWidth, z)
                    .setColor(color.r, color.g, color.b, color.a);
            bufferBuilder
                    .addVertex(matrix, x2 + halfWidth, y2 + halfWidth, z)
                    .setColor(color.r, color.g, color.b, color.a);
            bufferBuilder
                    .addVertex(matrix, x2 - halfWidth, y2 - halfWidth, z)
                    .setColor(color.r, color.g, color.b, color.a);
        }

        BufferUploader.drawWithShader(bufferBuilder.build());
        RenderSystem.disableBlend();
    }

    public static void drawRectBorders(
            PoseStack poseStack, CustomColor color, float x1, float y1, float x2, float y2, float z, float lineWidth) {
        drawLine(poseStack, color, x1, y1, x2, y1, z, lineWidth);
        drawLine(poseStack, color, x2, y1, x2, y2, z, lineWidth);
        drawLine(poseStack, color, x2, y2, x1, y2, z, lineWidth);
        drawLine(poseStack, color, x1, y2, x1, y1, z, lineWidth);
    }

    public static void drawRect(
            PoseStack poseStack, CustomColor color, float x, float y, float z, float width, float height) {
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferBuilder =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.addVertex(matrix, x, y + height, z).setColor(color.r, color.g, color.b, color.a);
        bufferBuilder.addVertex(matrix, x + width, y + height, z).setColor(color.r, color.g, color.b, color.a);
        bufferBuilder.addVertex(matrix, x + width, y, z).setColor(color.r, color.g, color.b, color.a);
        bufferBuilder.addVertex(matrix, x, y, z).setColor(color.r, color.g, color.b, color.a);

        BufferUploader.drawWithShader(bufferBuilder.build());
        RenderSystem.disableBlend();
    }

    public static void drawHoverableTexturedRect(
            PoseStack poseStack, Texture texture, float x, float y, boolean hovered) {
        drawTexturedRect(
                poseStack,
                texture.resource(),
                x,
                y,
                0,
                texture.width(),
                texture.height() / 2f,
                0,
                hovered ? texture.height() / 2 : 0,
                texture.width(),
                texture.height() / 2,
                texture.width(),
                texture.height());
    }

    public static void drawTexturedRect(PoseStack poseStack, Texture texture, float x, float y) {
        drawTexturedRect(
                poseStack,
                texture.resource(),
                x,
                y,
                texture.width(),
                texture.height(),
                texture.width(),
                texture.height());
    }

    public static void drawTexturedRect(
            PoseStack poseStack,
            ResourceLocation tex,
            float x,
            float y,
            float width,
            float height,
            int textureWidth,
            int textureHeight) {
        drawTexturedRect(
                poseStack, tex, x, y, 0, width, height, 0, 0, (int) width, (int) height, textureWidth, textureHeight);
    }

    public static void drawTexturedRect(
            PoseStack poseStack,
            ResourceLocation tex,
            float x,
            float y,
            float z,
            float width,
            float height,
            int textureWidth,
            int textureHeight) {
        drawTexturedRect(
                poseStack, tex, x, y, z, width, height, 0, 0, (int) width, (int) height, textureWidth, textureHeight);
    }

    public static void drawTexturedRect(
            PoseStack poseStack,
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

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder bufferBuilder =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix, x, y + height, z).setUv(uOffset * uScale, (vOffset + v) * vScale);
        bufferBuilder.addVertex(matrix, x + width, y + height, z).setUv((uOffset + u) * uScale, (vOffset + v) * vScale);
        bufferBuilder.addVertex(matrix, x + width, y, z).setUv((uOffset + u) * uScale, vOffset * vScale);
        bufferBuilder.addVertex(matrix, x, y, z).setUv(uOffset * uScale, vOffset * vScale);
        BufferUploader.drawWithShader(bufferBuilder.build());
    }

    public static void drawScalingTexturedRect(
            PoseStack poseStack,
            ResourceLocation tex,
            float x,
            float y,
            float z,
            float width,
            float height,
            int textureWidth,
            int textureHeight) {
        drawTexturedRect(
                poseStack, tex, x, y, z, width, height, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
    }

    public static void drawTexturedRectWithColor(
            PoseStack poseStack,
            ResourceLocation tex,
            CustomColor color,
            float x,
            float y,
            float z,
            float width,
            float height,
            int textureWidth,
            int textureHeight) {
        drawTexturedRectWithColor(
                poseStack,
                tex,
                color,
                x,
                y,
                z,
                width,
                height,
                0,
                0,
                (int) width,
                (int) height,
                textureWidth,
                textureHeight);
    }

    public static void drawTexturedRectWithColor(
            PoseStack poseStack,
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

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder bufferBuilder =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder
                .addVertex(matrix, x, y + height, z)
                .setUv(uOffset * uScale, (vOffset + v) * vScale)
                .setColor(color.r, color.g, color.b, color.a);
        bufferBuilder
                .addVertex(matrix, x + width, y + height, z)
                .setUv((uOffset + u) * uScale, (vOffset + v) * vScale)
                .setColor(color.r, color.g, color.b, color.a);
        bufferBuilder
                .addVertex(matrix, x + width, y, z)
                .setUv((uOffset + u) * uScale, vOffset * vScale)
                .setColor(color.r, color.g, color.b, color.a);
        bufferBuilder
                .addVertex(matrix, x, y, z)
                .setUv(uOffset * uScale, vOffset * vScale)
                .setColor(color.r, color.g, color.b, color.a);
        BufferUploader.drawWithShader(bufferBuilder.build());
        RenderSystem.disableBlend();
    }

    public static void drawArc(
            PoseStack poseStack,
            CustomColor color,
            float x,
            float y,
            float z,
            float fill,
            int innerRadius,
            int outerRadius) {
        drawArc(poseStack, color, x, y, z, fill, innerRadius, outerRadius, 0);
    }

    public static void drawArc(
            PoseStack poseStack,
            CustomColor color,
            float x,
            float y,
            float z,
            float fill,
            int innerRadius,
            int outerRadius,
            float angleOffset) {
        // keeps arc from overlapping itself
        int segments = (int) Math.min(fill * MAX_CIRCLE_STEPS, MAX_CIRCLE_STEPS - 1);
        float midX = x + outerRadius;
        float midY = y + outerRadius;
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferBuilder =
                Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float angle;
        float sinAngle;
        float cosAngle;
        for (int i = 0; i <= segments; i++) {
            angle = Mth.TWO_PI * i / (MAX_CIRCLE_STEPS - 1f) + angleOffset;
            sinAngle = Mth.sin(angle);
            cosAngle = Mth.cos(angle);

            bufferBuilder
                    .addVertex(matrix, midX + sinAngle * outerRadius, midY - cosAngle * outerRadius, z)
                    .setColor(color.r, color.g, color.b, color.a);
            bufferBuilder
                    .addVertex(matrix, midX + sinAngle * innerRadius, midY - cosAngle * innerRadius, z)
                    .setColor(color.r, color.g, color.b, color.a);
        }

        BufferUploader.drawWithShader(bufferBuilder.build());
        RenderSystem.disableBlend();
    }

    public static void drawRoundedRectWithBorder(
            PoseStack poseStack,
            CustomColor borderColor,
            CustomColor fillColor,
            float x,
            float y,
            float z,
            float width,
            float height,
            float lineWidth,
            int innerRadius,
            int outerRadius) {
        float x2 = x + width;
        float y2 = y + height;

        // Fill the rect
        final int fillOffset = (int) lineWidth;
        drawRect(
                poseStack,
                fillColor,
                x + fillOffset,
                y + fillOffset,
                z,
                width - fillOffset * 2,
                height - fillOffset * 2);
        drawLine(poseStack, fillColor, x + fillOffset, y + fillOffset, x2 - fillOffset, y + fillOffset, z, lineWidth);
        drawLine(poseStack, fillColor, x2 - fillOffset, y + fillOffset, x2 - fillOffset, y2 - fillOffset, z, lineWidth);
        drawLine(poseStack, fillColor, x + fillOffset, y2 - fillOffset, x2 - fillOffset, y2 - fillOffset, z, lineWidth);
        drawLine(poseStack, fillColor, x + fillOffset, y + fillOffset, x + fillOffset, y2 - fillOffset, z, lineWidth);

        float offset = outerRadius - 1;

        // Edges
        drawLine(poseStack, borderColor, x + offset, y, x2 - offset, y, z, lineWidth);
        drawLine(poseStack, borderColor, x2, y + offset, x2, y2 - offset, z, lineWidth);
        drawLine(poseStack, borderColor, x + offset, y2, x2 - offset, y2, z, lineWidth);
        drawLine(poseStack, borderColor, x, y + offset, x, y2 - offset, z, lineWidth);

        // Corners
        poseStack.pushPose();
        poseStack.translate(-1, -1, 0);
        drawRoundedCorner(poseStack, borderColor, x, y, z, innerRadius, outerRadius, Mth.HALF_PI * 3);
        drawRoundedCorner(poseStack, borderColor, x, y2 - offset * 2, z, innerRadius, outerRadius, (float) Math.PI);
        drawRoundedCorner(
                poseStack, borderColor, x2 - offset * 2, y2 - offset * 2, z, innerRadius, outerRadius, Mth.HALF_PI);
        drawRoundedCorner(poseStack, borderColor, x2 - offset * 2, y, z, innerRadius, outerRadius, 0);
        poseStack.popPose();
    }

    private static void drawRoundedCorner(
            PoseStack poseStack,
            CustomColor color,
            float x,
            float y,
            float z,
            int innerRadius,
            int outerRadius,
            float angleOffset) {
        drawArc(poseStack, color, x, y, z, 0.25f, innerRadius, outerRadius, angleOffset);
    }

    /**
     * drawProgressBar
     * Draws a progress bar (textureY1 and textureY2 now specify both textures with background being on top of the bar)
     *
     * @param poseStack poseStack to use
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
        drawProgressBarBackground(poseStack, texture, x1, y1, x2, y2, textureX1, textureY1, textureX2, half);
        drawProgressBarForegroundWithColor(
                poseStack,
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

    /**
     * drawProgressBar
     * Draws a progress bar (textureY1 and textureY2 now specify both textures with background being on top of the bar)
     *
     * @param poseStack poseStack to use
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
        drawProgressBarBackground(poseStack, texture, x1, y1, x2, y2, textureX1, textureY1, textureX2, half);
        drawProgressBarForeground(
                poseStack,
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

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture.resource());
        BufferBuilder bufferBuilder =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
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

        bufferBuilder.addVertex(matrix, xMin, yMin, 0).setUv(txMin, tyMin);
        bufferBuilder.addVertex(matrix, xMin, yMax, 0).setUv(txMin, tyMax);
        bufferBuilder.addVertex(matrix, xMax, yMax, 0).setUv(txMax, tyMax);
        bufferBuilder.addVertex(matrix, xMax, yMin, 0).setUv(txMax, tyMin);
        BufferUploader.drawWithShader(bufferBuilder.build());
    }

    private static void drawProgressBarForegroundWithColor(
            PoseStack poseStack,
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

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture.resource());
        BufferBuilder bufferBuilder =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
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

        bufferBuilder.addVertex(matrix, xMin, yMin, 0).setUv(txMin, tyMin).setColor(customColor.asInt());
        bufferBuilder.addVertex(matrix, xMin, yMax, 0).setUv(txMin, tyMax).setColor(customColor.asInt());
        bufferBuilder.addVertex(matrix, xMax, yMax, 0).setUv(txMax, tyMax).setColor(customColor.asInt());
        bufferBuilder.addVertex(matrix, xMax, yMin, 0).setUv(txMax, tyMin).setColor(customColor.asInt());
        BufferUploader.drawWithShader(bufferBuilder.build());
    }

    public static void drawProgressBarBackground(
            PoseStack poseStack,
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

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture.resource());
        BufferBuilder bufferBuilder =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float xMin = Math.min(x1, x2),
                xMax = Math.max(x1, x2),
                yMin = Math.min(y1, y2),
                yMax = Math.max(y1, y2),
                txMin = (float) Math.min(textureX1, textureX2) / texture.width(),
                txMax = (float) Math.max(textureX1, textureX2) / texture.width(),
                tyMin = (float) Math.min(textureY1, textureY2) / texture.height(),
                tyMax = (float) Math.max(textureY1, textureY2) / texture.height();

        bufferBuilder.addVertex(matrix, xMin, yMin, 0).setUv(txMin, tyMin);
        bufferBuilder.addVertex(matrix, xMin, yMax, 0).setUv(txMin, tyMax);
        bufferBuilder.addVertex(matrix, xMax, yMax, 0).setUv(txMax, tyMax);
        bufferBuilder.addVertex(matrix, xMax, yMin, 0).setUv(txMax, tyMin);
        BufferUploader.drawWithShader(bufferBuilder.build());
    }

    public static void fillGradient(
            Matrix4f matrix,
            BufferBuilder builder,
            float x1,
            float y1,
            float x2,
            float y2,
            int blitOffset,
            CustomColor colorA,
            CustomColor colorB) {
        builder.addVertex(matrix, x2, y1, blitOffset).setColor(colorA.r, colorA.g, colorA.b, colorA.a);
        builder.addVertex(matrix, x1, y1, blitOffset).setColor(colorA.r, colorA.g, colorA.b, colorA.a);
        builder.addVertex(matrix, x1, y2, blitOffset).setColor(colorB.r, colorB.g, colorB.b, colorB.a);
        builder.addVertex(matrix, x2, y2, blitOffset).setColor(colorB.r, colorB.g, colorB.b, colorB.a);
    }

    /*
       Normal GL implementation is in screen coordinates and thus y is inverted,
       this changes it so it doesn't do that
    */
    public static void enableScissor(int x, int y, int width, int height) {
        Window window = McUtils.window();
        double scale = window.getGuiScale();
        RenderSystem.enableScissor(
                (int) (x * scale), (int) (window.getHeight() - ((y + height) * scale)), (int) (width * scale), (int)
                        (height * scale));
    }

    public static void disableScissor() {
        RenderSystem.disableScissor();
    }

    public static void rotatePose(PoseStack poseStack, float centerX, float centerZ, float angle) {
        poseStack.translate(centerX, centerZ, 0);
        // See Quaternion#fromXYZ
        poseStack.mulPose(new Quaternionf(0F, 0, (float) StrictMath.sin(Math.toRadians(angle) / 2), (float)
                StrictMath.cos(-Math.toRadians(angle) / 2)));
        poseStack.translate(-centerX, -centerZ, 0);
    }

    public static void renderItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        guiGraphics.renderItem(itemStack, x, y);
    }

    public static void renderVignetteOverlay(PoseStack poseStack, CustomColor color, float alpha) {
        float[] colorArray = color.asFloatArray();
        RenderSystem.setShaderColor(colorArray[0], colorArray[1], colorArray[2], alpha);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        Window window = McUtils.window();

        RenderUtils.drawTexturedRect(
                poseStack,
                Texture.VIGNETTE.resource(),
                0,
                0,
                0,
                window.getGuiScaledWidth(),
                window.getGuiScaledHeight(),
                0,
                0,
                Texture.VIGNETTE.width(),
                Texture.VIGNETTE.height(),
                Texture.VIGNETTE.width(),
                Texture.VIGNETTE.height());

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
    }

    public static void renderCustomNametag(
            PoseStack matrixStack,
            MultiBufferSource buffer,
            int packedLight,
            int backgroundColor,
            EntityRenderDispatcher dispatcher,
            Entity entity,
            Component nametag,
            Font font,
            float nametagScale,
            float customOffset) {
        double d = dispatcher.distanceToSqr(entity);
        if (d <= 4096.0) {
            float yOffset = entity.getBbHeight() + 0.25F + customOffset;
            float xOffset = -(font.width(nametag) / 2f);
            boolean sneaking = entity.isDiscrete();

            matrixStack.pushPose();
            matrixStack.translate(0.0F, yOffset, 0.0F);
            matrixStack.mulPose(dispatcher.cameraOrientation());
            matrixStack.scale(0.025F * nametagScale, -0.025F * nametagScale, -0.025F * nametagScale);
            Matrix4f matrix4f = matrixStack.last().pose();

            font.drawInBatch(
                    nametag,
                    xOffset,
                    0f,
                    NAMETAG_COLOR,
                    false,
                    matrix4f,
                    buffer,
                    sneaking ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
                    backgroundColor,
                    packedLight);
            if (!sneaking) {
                font.drawInBatch(
                        nametag, xOffset, 0f, -1, false, matrix4f, buffer, Font.DisplayMode.NORMAL, 0, packedLight);
            }

            matrixStack.popPose();
        }
    }

    public static void renderProfessionBadge(
            PoseStack poseStack,
            EntityRenderDispatcher dispatcher,
            Entity entity,
            ResourceLocation tex,
            float width,
            float height,
            int uOffset,
            int vOffset,
            int u,
            int v,
            int textureWidth,
            int textureHeight,
            float customOffset,
            float horizontalShift,
            float verticalShift) {
        double d = dispatcher.distanceToSqr(entity);
        if (d <= 4096.0) {
            poseStack.pushPose();

            poseStack.translate(0, entity.getBbHeight() + 0.25F + customOffset, 0);
            poseStack.mulPose(dispatcher.cameraOrientation());
            poseStack.scale(-0.025F, -0.025F, 0.025F);

            Matrix4f matrix = poseStack.last().pose();

            float halfWidth = width / 2;
            float halfHeight = height / 2;
            float uScale = 1F / textureWidth;
            float vScale = 1F / textureHeight;

            RenderSystem.enableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, tex);

            BufferBuilder bufferBuilder =
                    Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            bufferBuilder
                    .addVertex(matrix, -halfWidth + horizontalShift, -halfHeight - verticalShift, 0)
                    .setUv(uOffset * uScale, vOffset * vScale);
            bufferBuilder
                    .addVertex(matrix, -halfWidth + horizontalShift, halfHeight - verticalShift, 0)
                    .setUv(uOffset * uScale, (v + vOffset) * vScale);
            bufferBuilder
                    .addVertex(matrix, halfWidth + horizontalShift, halfHeight - verticalShift, 0)
                    .setUv((u + uOffset) * uScale, (v + vOffset) * vScale);
            bufferBuilder
                    .addVertex(matrix, halfWidth + horizontalShift, -halfHeight - verticalShift, 0)
                    .setUv((u + uOffset) * uScale, vOffset * vScale);

            BufferUploader.drawWithShader(bufferBuilder.build());

            RenderSystem.disableDepthTest();

            poseStack.popPose();
        }
    }

    public static void drawMulticoloredRect(
            PoseStack poseStack, List<CustomColor> colors, float x, float y, float z, float width, float height) {
        if (colors.size() == 1) {
            drawRect(poseStack, colors.getFirst(), x, y, z, width, height);
            return;
        }
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder bufferBuilder =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float splitX = width / (colors.size() - 1);

        for (int i = 0; i < colors.size(); i++) {
            CustomColor color = colors.get(i);
            float leftX = Mth.clamp(x + splitX * (i - 1), x, x + width);
            float centerX = Mth.clamp(x + splitX * i, x, x + width);
            float rightX = Mth.clamp(x + splitX * (i + 1), x, x + width);

            // bottom left
            bufferBuilder.addVertex(matrix, leftX, y + height, z).setColor(color.r, color.g, color.b, color.a);
            // bottom right
            bufferBuilder.addVertex(matrix, centerX, y + height, z).setColor(color.r, color.g, color.b, color.a);
            // top right
            bufferBuilder.addVertex(matrix, rightX, y, z).setColor(color.r, color.g, color.b, color.a);
            // top left
            bufferBuilder.addVertex(matrix, centerX, y, z).setColor(color.r, color.g, color.b, color.a);
        }

        BufferUploader.drawWithShader(bufferBuilder.build());

        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void createMask(PoseStack poseStack, Texture texture, int x1, int y1, int x2, int y2) {
        createMask(poseStack, texture, x1, y1, x2, y2, 0, 0, texture.width(), texture.height());
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
    public static void createMask(
            PoseStack poseStack,
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

    public static void createRectMask(PoseStack poseStack, float x, float y, float width, float height) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);

        // Enable writing to stencil
        RenderSystem.stencilMask(0xff);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, true);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        // Disable writing to color or depth
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);

        drawRect(poseStack, CommonColors.WHITE, x, y, 0, width, height);

        // Reenable color and depth
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);

        // Only write to stencil area
        RenderSystem.stencilMask(0x00);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xff);
    }

    /**
     * Clears the active rendering mask from the screen.
     * Based on Figura <a href="https://github.com/Kingdom-of-The-Moon/FiguraRewriteRewrite"> code</a>.
     */
    public static void clearMask() {
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, true);

        // Turn off writing to stencil buffer.
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.stencilMask(0x00);

        // Always succeed in the stencil test, no matter what.
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
    }

    public static void renderDebugGrid(
            PoseStack poseStack, float gridDivisions, float dividedWidth, float dividedHeight) {
        for (int i = 1; i <= gridDivisions - 1; i++) {
            double x = dividedWidth * i;
            double y = dividedHeight * i;
            RenderUtils.drawRect(poseStack, CommonColors.GRAY, (float) x, 0, 0, 1, dividedHeight * gridDivisions);
            RenderUtils.drawRect(poseStack, CommonColors.GRAY, 0, (float) y, 0, dividedWidth * gridDivisions, 1);
            if (i % 2 == 0) continue; // reduce clutter
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromString(String.valueOf(i)),
                            (float) x,
                            dividedHeight * (gridDivisions / 2),
                            CommonColors.RED,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            TextShadow.NORMAL);
            FontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            StyledText.fromString(String.valueOf(i)),
                            dividedWidth * (gridDivisions / 2),
                            (float) y,
                            CommonColors.CYAN,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            TextShadow.NORMAL);
        }
    }
}
