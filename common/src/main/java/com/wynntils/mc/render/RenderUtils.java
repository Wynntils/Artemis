/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.wynntils.utils.objects.CustomColor;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

public class RenderUtils {

    // tooltip colors for item screenshot creation. somewhat hacky solution to get around transparency issues -
    // these colors were chosen to best match how tooltips are displayed in-game
    private static final CustomColor BACKGROUND = CustomColor.fromInt(0xFF100010);
    private static final CustomColor BORDER_START = CustomColor.fromInt(0xFF25005B);
    private static final CustomColor BORDER_END = CustomColor.fromInt(0xFF180033);

    // number of possible segments for arc drawing
    private static final float MAX_CIRCLE_STEPS = 16f;

    // See https://github.com/MinecraftForge/MinecraftForge/issues/8083 as to why this uses TRIANGLE_STRIPS.
    // TLDR: New OpenGL only supports TRIANGLES and Minecraft patched QUADS to be usable ATM, but LINES patch is broken
    // and you can't use it.
    // (This also means that using QUADS is probably not the best idea)
    public static void drawLine(
            PoseStack poseStack, CustomColor color, float x1, float y1, float x2, float y2, float z, float width) {
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

        float halfWidth = width / 2;

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        if (x1 == x2) {
            if (y2 < y1) {
                float tmp = y1;
                y1 = y2;
                y2 = tmp;
            }
            bufferBuilder
                    .vertex(matrix, x1 - halfWidth, y1, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 - halfWidth, y2, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x1 + halfWidth, y1, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 + halfWidth, y2, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
        } else if (y1 == y2) {
            if (x2 < x1) {
                float tmp = x1;
                x1 = x2;
                x2 = tmp;
            }

            bufferBuilder
                    .vertex(matrix, x1, y1 - halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x1, y1 + halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2, y2 - halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2, y2 + halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
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
                    .vertex(matrix, x1 + halfWidth, y1 - halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x1 - halfWidth, y1 + halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 + halfWidth, y2 - halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 - halfWidth, y2 + halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
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
                    .vertex(matrix, x1 + halfWidth, y1 + halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x1 - halfWidth, y1 - halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 + halfWidth, y2 + halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 - halfWidth, y2 - halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
        }

        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);
        RenderSystem.disableBlend();
    }

    public static void drawRectBorders(
            PoseStack poseStack, CustomColor color, float x1, float y1, float x2, float y2, float z, float lineWidth) {
        drawLine(poseStack, color, x1, y1, x2, y1, z, lineWidth);
        drawLine(poseStack, color, x2, y1, x2, y2, z, lineWidth);
        drawLine(poseStack, color, x2, y2, x1, y2, z, lineWidth);
        drawLine(poseStack, color, x1, y2, x1, y1, z, lineWidth);
    }

    public static void drawRect(CustomColor color, int x, int y, int z, int width, int height) {
        drawRect(new PoseStack(), color, x, y, z, width, height);
    }

    public static void drawRect(PoseStack poseStack, CustomColor color, int x, int y, int z, int width, int height) {
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder
                .vertex(matrix, x, y + height, z)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x + width, y + height, z)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x + width, y, z)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x, y, z)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);
        RenderSystem.disableBlend();
    }

    public static void drawTexturedRect(
            PoseStack poseStack,
            ResourceLocation tex,
            int x,
            int y,
            int width,
            int height,
            int textureWidth,
            int textureHeight) {
        drawTexturedRect(poseStack, tex, x, y, 0, width, height, 0, 0, width, height, textureWidth, textureHeight);
    }

    public static void drawTexturedRect(
            PoseStack poseStack,
            ResourceLocation tex,
            int x,
            int y,
            int z,
            int width,
            int height,
            int textureWidth,
            int textureHeight) {
        drawTexturedRect(poseStack, tex, x, y, z, width, height, 0, 0, width, height, textureWidth, textureHeight);
    }

    public static void drawTexturedRect(
            PoseStack poseStack,
            ResourceLocation tex,
            int x,
            int y,
            int z,
            int width,
            int height,
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
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder
                .vertex(matrix, x, y + height, z)
                .uv(uOffset * uScale, (vOffset + v) * vScale)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x + width, y + height, z)
                .uv((uOffset + u) * uScale, (vOffset + v) * vScale)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x + width, y, z)
                .uv((uOffset + u) * uScale, vOffset * vScale)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x, y, z)
                .uv(uOffset * uScale, vOffset * vScale)
                .endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);
    }

    public static void drawTexturedRectWithColor(
            ResourceLocation tex,
            CustomColor color,
            int x,
            int y,
            int z,
            int width,
            int height,
            int textureWidth,
            int textureHeight) {
        drawTexturedRectWithColor(
                new PoseStack(), tex, color, x, y, z, width, height, 0, 0, width, height, textureWidth, textureHeight);
    }

    public static void drawTexturedRectWithColor(
            PoseStack poseStack,
            ResourceLocation tex,
            CustomColor color,
            int x,
            int y,
            int z,
            int width,
            int height,
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
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder
                .vertex(matrix, x, y + height, z)
                .uv(uOffset * uScale, (vOffset + v) * vScale)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x + width, y + height, z)
                .uv((uOffset + u) * uScale, (vOffset + v) * vScale)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x + width, y, z)
                .uv((uOffset + u) * uScale, vOffset * vScale)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x, y, z)
                .uv(uOffset * uScale, vOffset * vScale)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);
        RenderSystem.disableBlend();
    }

    public static void drawArc(CustomColor color, int x, int y, int z, float fill, int radius) {
        drawArc(new PoseStack(), color, x, y, z, fill, radius);
    }

    public static void drawArc(PoseStack poseStack, CustomColor color, int x, int y, int z, float fill, int radius) {
        // keeps arc from overlapping itself
        int segments = (int) Math.min(fill * MAX_CIRCLE_STEPS, MAX_CIRCLE_STEPS - 1);

        // each section of arc texture is 64 wide, ordered left to right
        int uOffset = 64 * segments;

        // render arc texture
        drawTexturedRectWithColor(
                poseStack,
                Texture.ARC.resource(),
                color,
                x,
                y,
                z,
                radius * 2,
                radius * 2,
                uOffset,
                0,
                64,
                64,
                Texture.ARC.width(),
                Texture.ARC.height());
    }

    public static void drawTooltip(List<ClientTooltipComponent> lines, PoseStack poseStack, Font font) {
        int tooltipWidth = 0;
        int tooltipHeight = lines.size() == 1 ? -2 : 0;

        for (ClientTooltipComponent clientTooltipComponent : lines) {
            int lineWidth = clientTooltipComponent.getWidth(font);
            if (lineWidth > tooltipWidth) {
                tooltipWidth = lineWidth;
            }
            tooltipHeight += clientTooltipComponent.getHeight();
        }

        // background box
        poseStack.pushPose();
        int tooltipX = 4;
        int tooltipY = 4;
        int zLevel = 400;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix4f = poseStack.last().pose();
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 3,
                tooltipY - 4,
                tooltipX + tooltipWidth + 3,
                tooltipY - 3,
                zLevel,
                BACKGROUND,
                BACKGROUND);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 3,
                tooltipY + tooltipHeight + 3,
                tooltipX + tooltipWidth + 3,
                tooltipY + tooltipHeight + 4,
                zLevel,
                BACKGROUND,
                BACKGROUND);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 3,
                tooltipY - 3,
                tooltipX + tooltipWidth + 3,
                tooltipY + tooltipHeight + 3,
                zLevel,
                BACKGROUND,
                BACKGROUND);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 4,
                tooltipY - 3,
                tooltipX - 3,
                tooltipY + tooltipHeight + 3,
                zLevel,
                BACKGROUND,
                BACKGROUND);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX + tooltipWidth + 3,
                tooltipY - 3,
                tooltipX + tooltipWidth + 4,
                tooltipY + tooltipHeight + 3,
                zLevel,
                BACKGROUND,
                BACKGROUND);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 3,
                tooltipY - 3 + 1,
                tooltipX - 3 + 1,
                tooltipY + tooltipHeight + 3 - 1,
                zLevel,
                BORDER_START,
                BORDER_END);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX + tooltipWidth + 2,
                tooltipY - 3 + 1,
                tooltipX + tooltipWidth + 3,
                tooltipY + tooltipHeight + 3 - 1,
                zLevel,
                BORDER_START,
                BORDER_END);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 3,
                tooltipY - 3,
                tooltipX + tooltipWidth + 3,
                tooltipY - 3 + 1,
                zLevel,
                BORDER_START,
                BORDER_START);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 3,
                tooltipY + tooltipHeight + 2,
                tooltipX + tooltipWidth + 3,
                tooltipY + tooltipHeight + 3,
                zLevel,
                BORDER_END,
                BORDER_END);
        RenderSystem.enableDepthTest();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();

        // text
        MultiBufferSource.BufferSource bufferSource =
                MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        poseStack.translate(0.0, 0.0, 400.0);
        int s = tooltipY;
        boolean first = true;
        for (ClientTooltipComponent line : lines) {
            line.renderText(font, tooltipX, s, matrix4f, bufferSource);
            s += line.getHeight() + (first ? 2 : 0);
            first = false;
        }
        bufferSource.endBatch();
        poseStack.popPose();
    }

    /** drawProgressBar
     * Draws a progress bar(textureY1 and textureY2 now specify both textures with background being on top of the bar)
     *
     * @param poseStack poseStack to use
     * @param texture the texture to use
     * @param x1 left x on screen
     * @param y1 top y on screen
     * @param x2 right x on screen
     * @param y2 bottom right on screen
     * @param textureX1 texture left x for the part
     * @param textureY1 texture top y for the part(top of background)
     * @param textureX2 texture right x for the part
     * @param textureY2 texture bottom y for the part(bottom of bar)
     * @param progress progress of the bar, 0.0f to 1.0f is left to right and 0.0f to -1.0f is right to left
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
        int half = (textureY1 + textureY2) / 2;
        drawProgressBar(poseStack, texture, x1, y1, x2, y2, textureX1, textureY1, textureX2, half + 1, progress, true);
        drawProgressBar(poseStack, texture, x1, y1, x2, y2, textureX1, half + 1, textureX2, textureY2, progress, false);
    }

    /** drawProgressBar
     * Draws a textured progress bar.
     *
     * @param poseStack poseStack to use
     * @param texture the texture to use
     * @param x1 left x on screen
     * @param y1 top y on screen
     * @param x2 right x on screen
     * @param y2 bottom right on screen
     * @param textureX1 texture left x for the part
     * @param textureY1 texture top y for the part
     * @param textureX2 texture right x for the part
     * @param textureY2 texture bottom y for the part
     * @param progress progress of the bar, 0.0f to 1.0f is left to right and 0.0f to -1.0f is right to left
     * @param background is it drawing the background or not(whole ty part is background)
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
            float progress,
            boolean background) {
        if (background) {
            Matrix4f matrix = poseStack.last().pose();

            RenderSystem.enableTexture();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, texture.resource());
            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            float xMin = Math.min(x1, x2),
                    xMax = Math.max(x1, x2),
                    yMin = Math.min(y1, y2),
                    yMax = Math.max(y1, y2),
                    txMin = (float) Math.min(textureX1, textureX2) / texture.width(),
                    txMax = (float) Math.max(textureX1, textureX2) / texture.height(),
                    tyMin = (float) Math.min(textureY1, textureY2) / texture.height(),
                    tyMax = (float) Math.max(textureY1, textureY2) / texture.height();

            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferBuilder.vertex(matrix, xMin, yMin, 0).uv(txMin, tyMin).endVertex();
            bufferBuilder.vertex(matrix, xMin, yMax, 0).uv(txMin, tyMax).endVertex();
            bufferBuilder.vertex(matrix, xMax, yMax, 0).uv(txMax, tyMax).endVertex();
            bufferBuilder.vertex(matrix, xMax, yMin, 0).uv(txMax, tyMin).endVertex();
            bufferBuilder.end();
            BufferUploader.end(bufferBuilder);
        } else if (progress != 0.0f) {
            Matrix4f matrix = poseStack.last().pose();

            RenderSystem.enableTexture();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, texture.resource());
            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            float xMin = Math.min(x1, x2),
                    xMax = Math.max(x1, x2),
                    yMin = Math.min(y1, y2),
                    yMax = Math.max(y1, y2) - 1.5f,
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

            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferBuilder.vertex(matrix, xMin, yMin, 0).uv(txMin, tyMin).endVertex();
            bufferBuilder.vertex(matrix, xMin, yMax, 0).uv(txMin, tyMax).endVertex();
            bufferBuilder.vertex(matrix, xMax, yMax, 0).uv(txMax, tyMax).endVertex();
            bufferBuilder.vertex(matrix, xMax, yMin, 0).uv(txMax, tyMin).endVertex();
            bufferBuilder.end();
            BufferUploader.end(bufferBuilder);
        }
    }

    public static void fillGradient(
            Matrix4f matrix,
            BufferBuilder builder,
            int x1,
            int y1,
            int x2,
            int y2,
            int blitOffset,
            CustomColor colorA,
            CustomColor colorB) {
        builder.vertex(matrix, x2, y1, blitOffset)
                .color(colorA.r, colorA.g, colorA.b, colorA.a)
                .endVertex();
        builder.vertex(matrix, x1, y1, blitOffset)
                .color(colorA.r, colorA.g, colorA.b, colorA.a)
                .endVertex();
        builder.vertex(matrix, x1, y2, blitOffset)
                .color(colorB.r, colorB.g, colorB.b, colorB.a)
                .endVertex();
        builder.vertex(matrix, x2, y2, blitOffset)
                .color(colorB.r, colorB.g, colorB.b, colorB.a)
                .endVertex();
    }

    public static void copyImageToClipboard(BufferedImage bi) {
        class ClipboardImage implements Transferable {
            Image image;

            public ClipboardImage(Image image) {
                this.image = image;
            }

            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[] {DataFlavor.imageFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.imageFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (!DataFlavor.imageFlavor.equals(flavor)) throw new UnsupportedFlavorException(flavor);
                return this.image;
            }
        }

        ClipboardImage ci = new ClipboardImage(bi);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ci, null);
    }

    public static BufferedImage createScreenshot(RenderTarget fb) {
        BufferedImage bufferedimage = new BufferedImage(fb.width, fb.height, BufferedImage.TYPE_INT_ARGB);
        try (NativeImage image = new NativeImage(fb.width, fb.height, false)) {
            RenderSystem.bindTexture(fb.getColorTextureId());
            image.downloadTexture(0, false);
            image.flipY();

            int[] pixelValues = image.makePixelArray();

            bufferedimage.setRGB(0, 0, fb.width, fb.height, pixelValues, 0, fb.width);
        }
        return bufferedimage;
    }
}
