/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wynntils.services.lootrunpaths.LootrunPathInstance;
import com.wynntils.services.map.MapTexture;
import com.wynntils.services.map.pois.Poi;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.VectorUtils;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.buffered.CustomRenderType;
import com.wynntils.utils.render.type.PointerType;
import com.wynntils.utils.type.BoundingBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public final class MapRenderer {
    // The possible zoom levels range between [1, ZOOM_LEVELS] (inclusive).
    // Levels can be thought of as a percentage of the zoom,
    // with 1 being the minimum zoom, and ZOOM_LEVELS being the maximum zoom.
    public static final int ZOOM_LEVELS = 100;

    // This value should be the nearest step to the default zoom, 1.0f.
    public static final float DEFAULT_ZOOM_LEVEL = 60;

    // The minimum and maximum zoom values. This is the range of the zoom.
    // The minimum zoom is where the map is at its smallest, and the maximum zoom is where the map is at its largest.
    // The ratio is 20:1 when the zoom is at it's minimum, 1:10 when the zoom is at it's maximum.
    // The zoom is adjusted by GUI scale, to make sure these ratios are consistent across all GUI scales.
    public static final float MIN_ZOOM = 0.2f;
    public static final float MAX_ZOOM = 10f;

    // These don't have significance, they are just used to calculate the zoom,
    // and are cached for performance.
    private static final double MIN_ZOOM_LOG = Math.log(MIN_ZOOM);
    private static final double MAX_ZOOM_LOG = Math.log(MAX_ZOOM);

    private static final float CHUNK_LINE_WIDTH = 1.0f;

    // Zoom is calculated using exponential interpolation between MIN_ZOOM and MAX_ZOOM.
    // The result is that the zoom increases uniformly for all levels, no matter the current zoom.
    // To achieve this, we need to exponentially increase the zoom value for each step.
    // - Taking the log of the zoom value, and then linearly interpolating between the log values.
    // - This means that the zoom values for each level become exponentially larger.
    // - Steps are 1-based (1 to ZOOM_LEVELS), so we subtract 1 from the level to get the correct zoom value,
    //   to ensure that the real zoom values are in the range [MIN_ZOOM, MAX_ZOOM] (including the boundaries).
    public static float getZoomRenderScaleFromLevel(float zoomLevel) {
        double guiScale = McUtils.guiScale();
        double logGuiScale = Math.log(guiScale);

        // log(MIN_ZOOM / guiScale) = log(MIN_ZOOM) - log(guiScale)
        double logMinZoomGuiScale = MIN_ZOOM_LOG - logGuiScale;
        // log(MAX_ZOOM / guiScale) = log(MAX_ZOOM) - log(guiScale)
        double logMaxZoomGuiScale = MAX_ZOOM_LOG - logGuiScale;

        return (float) Math.exp(
                logMinZoomGuiScale + (logMaxZoomGuiScale - logMinZoomGuiScale) * (zoomLevel - 1) / (ZOOM_LEVELS - 1));
    }

    public static void renderMapQuad(
            MapTexture map,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float centerX,
            float centerZ,
            float textureX,
            float textureZ,
            float width,
            float height,
            float scale) {
        VertexConsumer buffer = bufferSource.getBuffer(CustomRenderType.getMapPositionTextureQuad(map.resource()));

        renderMap(map, poseStack, buffer, centerX, centerZ, textureX, textureZ, width, height, scale);
    }

    public static void renderMapQuad(
            MapTexture map,
            PoseStack poseStack,
            float centerX,
            float centerZ,
            float textureX,
            float textureZ,
            float width,
            float height,
            float scale) {
        RenderSystem.disableBlend();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, map.resource());

        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

        // clamp map rendering
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL13.GL_CLAMP_TO_BORDER);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL13.GL_CLAMP_TO_BORDER);

        BufferBuilder builder =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        renderMap(map, poseStack, builder, centerX, centerZ, textureX, textureZ, width, height, scale);

        BufferUploader.drawWithShader(builder.build());
    }

    private static void renderMap(
            MapTexture map,
            PoseStack poseStack,
            VertexConsumer buffer,
            float centerX,
            float centerZ,
            float textureX,
            float textureZ,
            float width,
            float height,
            float scale) {
        float uScale = 1f / map.getTextureWidth();
        float vScale = 1f / map.getTextureHeight();

        float halfRenderedWidth = width / 2f;
        float halfRenderedHeight = height / 2f;
        float halfTextureWidth = halfRenderedWidth * scale;
        float halfTextureHeight = halfRenderedHeight * scale;

        Matrix4f matrix = poseStack.last().pose();

        buffer.addVertex(matrix, (centerX - halfRenderedWidth), (centerZ + halfRenderedHeight), 0)
                .setUv((textureX - halfTextureWidth) * uScale, (textureZ + halfTextureHeight) * vScale);
        buffer.addVertex(matrix, (centerX + halfRenderedWidth), (centerZ + halfRenderedHeight), 0)
                .setUv((textureX + halfTextureWidth) * uScale, (textureZ + halfTextureHeight) * vScale);
        buffer.addVertex(matrix, (centerX + halfRenderedWidth), (centerZ - halfRenderedHeight), 0)
                .setUv((textureX + halfTextureWidth) * uScale, (textureZ - halfTextureHeight) * vScale);
        buffer.addVertex(matrix, (centerX - halfRenderedWidth), (centerZ - halfRenderedHeight), 0)
                .setUv((textureX - halfTextureWidth) * uScale, (textureZ - halfTextureHeight) * vScale);
    }

    public static void renderCursor(
            PoseStack poseStack,
            float renderX,
            float renderY,
            float pointerScale,
            CustomColor pointerColor,
            PointerType pointerType,
            boolean followPlayerRotation) {
        float rotationAngle;
        if (followPlayerRotation) {
            rotationAngle = McUtils.player().getYRot()
                    - McUtils.mc().gameRenderer.getMainCamera().getYRot();
        } else {
            rotationAngle = 180 + McUtils.player().getYRot();
        }

        poseStack.pushPose();
        RenderUtils.rotatePose(poseStack, renderX, renderY, rotationAngle);

        float renderedWidth = pointerType.width * pointerScale;
        float renderedHeight = pointerType.height * pointerScale;

        RenderUtils.drawTexturedRectWithColor(
                poseStack,
                Texture.MAP_POINTERS.resource(),
                pointerColor,
                renderX - renderedWidth / 2f,
                renderY - renderedHeight / 2f,
                0,
                renderedWidth,
                renderedHeight,
                0,
                pointerType.textureY,
                pointerType.width,
                pointerType.height,
                Texture.MAP_POINTERS.width(),
                Texture.MAP_POINTERS.height());

        poseStack.popPose();
    }

    public static void renderChunks(
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            BoundingBox renderedWorldBoundingBox,
            Set<Long> mappedChunks,
            float mapCenterX,
            float centerX,
            float mapCenterZ,
            float centerZ,
            float zoomRenderScale) {
        ChunkPos topLeft =
                new ChunkPos(new BlockPos((int) renderedWorldBoundingBox.x1(), 0, (int) renderedWorldBoundingBox.z1()));
        ChunkPos bottomRight =
                new ChunkPos(new BlockPos((int) renderedWorldBoundingBox.x2(), 0, (int) renderedWorldBoundingBox.z2()));

        // Render the chunk grid, with a 1px border around each chunk.
        for (int x = topLeft.x; x <= bottomRight.x; x++) {
            for (int z = topLeft.z; z <= bottomRight.z; z++) {
                ChunkPos chunkPos = new ChunkPos(x, z);

                float worldX1 = chunkPos.getMinBlockX() - 1;
                float worldX2 = chunkPos.getMaxBlockX() + 1;
                float worldZ1 = chunkPos.getMinBlockZ() - 1;
                float worldZ2 = chunkPos.getMaxBlockZ() + 1;

                float x1 = getRenderX((int) worldX1, mapCenterX, centerX, zoomRenderScale);
                float x2 = getRenderX((int) worldX2, mapCenterX, centerX, zoomRenderScale);
                float z1 = getRenderZ((int) worldZ1, mapCenterZ, centerZ, zoomRenderScale);
                float z2 = getRenderZ((int) worldZ2, mapCenterZ, centerZ, zoomRenderScale);

                CustomColor renderColor =
                        mappedChunks.contains(chunkPos.toLong()) ? CommonColors.GREEN : CommonColors.RED;

                CustomColor topRenderColor =
                        mappedChunks.contains(new ChunkPos(x, z - 1).toLong()) ? CommonColors.GREEN : renderColor;
                CustomColor leftRenderColor =
                        mappedChunks.contains(new ChunkPos(x - 1, z).toLong()) ? CommonColors.GREEN : renderColor;

                // Render the top and left borders of the chunk
                BufferedRenderUtils.drawLine(
                        poseStack, bufferSource, topRenderColor, x1, z1, x2, z1, 0, CHUNK_LINE_WIDTH);
                BufferedRenderUtils.drawLine(
                        poseStack, bufferSource, leftRenderColor, x1, z1, x1, z2, 0, CHUNK_LINE_WIDTH);

                // Render the right border, if the chunk is the rightmost chunk
                if (x == bottomRight.x) {
                    // Check if the chunk on the right is mapped, if it is, render with the correct color
                    CustomColor rightRenderColor =
                            mappedChunks.contains(new ChunkPos(x + 1, z).toLong()) ? CommonColors.GREEN : renderColor;

                    BufferedRenderUtils.drawLine(
                            poseStack, bufferSource, rightRenderColor, x2, z1, x2, z2, 0, CHUNK_LINE_WIDTH);
                }

                // Render the bottom border, if the chunk is the bottommost chunk
                if (z == bottomRight.z) {
                    // Check if the chunk on the top is mapped, if it is, render with the correct color
                    CustomColor bottomRenderColor =
                            mappedChunks.contains(new ChunkPos(x, z + 1).toLong()) ? CommonColors.GREEN : renderColor;

                    BufferedRenderUtils.drawLine(
                            poseStack, bufferSource, bottomRenderColor, x1, z2, x2, z2, 0, CHUNK_LINE_WIDTH);
                }
            }
        }
    }

    public static void renderLootrunLine(
            LootrunPathInstance lootrun,
            float lootrunWidth,
            float outlineWidth,
            PoseStack poseStack,
            float centerX,
            float centerZ,
            float mapTextureX,
            float mapTextureZ,
            float currentZoom,
            int lootrunColor,
            int outlineColor) {
        if (lootrun.simplifiedPath().size() < 3) return;

        BufferBuilder bufferBuilder =
                Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableCull();

        List<Vector2f> points = new ArrayList<>();

        List<Vector2f> middlePoints = new ArrayList<>();

        Vector2f last = null;
        for (Vector2d point : lootrun.simplifiedPath()) {
            Vector2f screenPos = new Vector2f(
                    getRenderX((int) point.x(), mapTextureX, centerX, currentZoom),
                    getRenderZ((int) point.y(), mapTextureZ, centerZ, currentZoom));

            if (last == null) {
                last = screenPos;
                points.add(screenPos);
                continue;
            }

            if (new Vector2f(last).sub(screenPos).length() > 2) {
                last = screenPos;
                points.add(screenPos);
            }
        }

        for (int i = 0; i < points.size() - 1; i++) {
            if (i == 0) {
                middlePoints.add(points.getFirst());
            } else if (i == points.size() - 2) {
                middlePoints.add(points.getLast());
            } else {
                middlePoints.add(
                        new Vector2f(points.get(i)).add(points.get(i + 1)).mul(0.5f));
            }
        }

        for (int i = 1; i < middlePoints.size(); i++) {
            drawTriangles(
                    bufferBuilder,
                    poseStack,
                    middlePoints.get(i - 1),
                    points.get(i),
                    middlePoints.get(i),
                    outlineColor,
                    outlineWidth);
            drawTriangles(
                    bufferBuilder,
                    poseStack,
                    middlePoints.get(i - 1),
                    points.get(i),
                    middlePoints.get(i),
                    lootrunColor,
                    lootrunWidth);
        }

        BufferUploader.drawWithShader(bufferBuilder.build());
        RenderSystem.enableCull();
    }

    private static void drawTriangles(
            BufferBuilder bufferBuilder,
            PoseStack poseStack,
            Vector2f p0,
            Vector2f p1,
            Vector2f p2,
            int color,
            float lineWidth) {
        Vector2f t0 = new Vector2f();
        Vector2f t2 = new Vector2f();

        p1.sub(p0, t0);
        p2.sub(p1, t2);

        t0 = new Vector2f(-t0.y(), t0.x());
        t2 = new Vector2f(-t2.y(), t2.x());

        if (MathUtils.signedArea(p0, p1, p2) > 0) {
            t0.mul(-1);
            t2.mul(-1);
        }

        t0.normalize();
        t2.normalize();
        t0.mul(lineWidth);
        t2.mul(lineWidth);

        Vector2f lineIntersection = VectorUtils.lineIntersection(
                new Vector2f(p0).add(t0), new Vector2f(p1).add(t0), new Vector2f(p2).add(t2), new Vector2f(p1).add(t2));

        Vector2f anchor = new Vector2f();
        float anchorLength = Float.MAX_VALUE;

        if (lineIntersection != null) {
            lineIntersection.sub(p1, anchor);
            anchorLength = lineIntersection.length();
        }

        Vector2f p0p1 = new Vector2f(p0).sub(p1);
        Vector2f p1p2 = new Vector2f(p1).sub(p2);

        if (anchorLength > p0p1.length() || anchorLength > p1p2.length()) {
            addVertex(bufferBuilder, new Vector2f(p0).add(t0), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p0).sub(t0), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p1).add(t0), color, poseStack);

            addVertex(bufferBuilder, new Vector2f(p0).sub(t0), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p1).add(t0), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p1).sub(t0), color, poseStack);

            drawRoundJoint(p1, new Vector2f(p1).add(t0), new Vector2f(p1).add(t2), p2, bufferBuilder, color, poseStack);

            addVertex(bufferBuilder, new Vector2f(p2).add(t2), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p1).sub(t2), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p1).add(t2), color, poseStack);

            addVertex(bufferBuilder, new Vector2f(p2).add(t2), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p1).sub(t2), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p2).sub(t2), color, poseStack);
        } else {
            addVertex(bufferBuilder, new Vector2f(p0).add(t0), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p0).sub(t0), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p1).sub(anchor), color, poseStack);

            addVertex(bufferBuilder, new Vector2f(p0).add(t0), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p1).sub(anchor), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p1).add(t0), color, poseStack);

            Vector2f rP0 = new Vector2f(p1).add(t0);
            Vector2f rP1 = new Vector2f(p1).add(t2);
            Vector2f rP2 = new Vector2f(p1).sub(anchor);

            addVertex(bufferBuilder, rP0, color, poseStack);
            addVertex(bufferBuilder, p1, color, poseStack);
            addVertex(bufferBuilder, rP2, color, poseStack);

            drawRoundJoint(p1, rP0, rP1, rP2, bufferBuilder, color, poseStack);

            addVertex(bufferBuilder, p1, color, poseStack);
            addVertex(bufferBuilder, rP1, color, poseStack);
            addVertex(bufferBuilder, rP2, color, poseStack);

            addVertex(bufferBuilder, new Vector2f(p2).add(t2), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p1).sub(anchor), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p1).add(t2), color, poseStack);

            addVertex(bufferBuilder, new Vector2f(p2).add(t2), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p1).sub(anchor), color, poseStack);
            addVertex(bufferBuilder, new Vector2f(p2).sub(t2), color, poseStack);
        }
    }

    private static void drawRoundJoint(
            Vector2f center,
            Vector2f p0,
            Vector2f p1,
            Vector2f nextPointInLine,
            BufferBuilder bufferBuilder,
            int color,
            PoseStack poseStack) {
        float radius = new Vector2f(center).sub(p0).length();

        float angle0 = (float) Math.atan2((p1.y() - center.y()), (p1.x() - center.x()));
        float angle1 = (float) Math.atan2((p0.y() - center.y()), (p0.x() - center.x()));
        float orgAngle0 = angle0;

        if (angle1 > angle0) {
            while (angle1 - angle0 >= Math.PI - 0.001f) {
                angle1 = (float) (angle1 - 2 * Math.PI);
            }
        } else {
            while (angle0 - angle1 >= Math.PI - 0.001f) {
                angle0 = (float) (angle0 - 2 * Math.PI);
            }
        }

        float angleDiff = angle1 - angle0;

        if (Math.abs(angleDiff) >= Math.PI - 0.001f && Math.abs(angleDiff) <= Math.PI + 0.001f) {
            Vector2f r1 = new Vector2f(center).sub(nextPointInLine);

            if (r1.x() == 0) {
                if (r1.y() > 0) {
                    angleDiff = -angleDiff;
                }
            } else if (r1.x() >= -0.001f) {
                angleDiff = -angleDiff;
            }
        }

        int nSegments = (int) Math.abs(angleDiff * radius / 2);
        nSegments++;

        float angleInc = angleDiff / nSegments;

        for (int i = 0; i < nSegments; i++) {
            addVertex(bufferBuilder, center, color, poseStack);
            addVertex(
                    bufferBuilder,
                    new Vector2f((float) (center.x() + radius * Math.cos(orgAngle0 + angleInc * i)), (float)
                            (center.y() + radius * Math.sin(orgAngle0 + angleInc * i))),
                    color,
                    poseStack);
            addVertex(
                    bufferBuilder,
                    new Vector2f((float) (center.x() + radius * Math.cos(orgAngle0 + angleInc * (1 + i))), (float)
                            (center.y() + radius * Math.sin(orgAngle0 + angleInc * (1 + i)))),
                    color,
                    poseStack);
        }
    }

    private static void addVertex(BufferBuilder bufferBuilder, Vector2f pos, int color, PoseStack poseStack) {
        bufferBuilder.addVertex(poseStack.last().pose(), pos.x(), pos.y(), 0).setColor(color);
    }

    /**
     * {@param poi} POI that we get the render coordinate for
     * {@param mapCenterX} center coordinates of map (in-game coordinates)
     * {@param centerX} center coordinates of map (screen render coordinates)
     * {@param currentZoom} the bigger, the more detailed the map is
     */
    public static float getRenderX(Poi poi, float mapCenterX, float centerX, float currentZoom) {
        double distanceX = poi.getLocation().getX() - mapCenterX;
        return (float) (centerX + distanceX * currentZoom);
    }

    public static float getRenderX(int worldX, float mapCenterX, float centerX, float currentZoom) {
        double distanceX = worldX - mapCenterX;
        return (float) (centerX + distanceX * currentZoom);
    }

    /**
     * {@param poi} POI that we get the render coordinate for
     * {@param mapCenterZ} center coordinates of map (in-game coordinates)
     * {@param centerZ} center coordinates of map (screen render coordinates)
     * {@param currentZoom} the bigger, the more detailed the map is
     */
    public static float getRenderZ(Poi poi, float mapCenterZ, float centerZ, float currentZoom) {
        double distanceZ = poi.getLocation().getZ() - mapCenterZ;
        return (float) (centerZ + distanceZ * currentZoom);
    }

    public static float getRenderZ(int worldZ, float mapCenterZ, float centerZ, float currentZoom) {
        double distanceZ = worldZ - mapCenterZ;
        return (float) (centerZ + distanceZ * currentZoom);
    }
}
