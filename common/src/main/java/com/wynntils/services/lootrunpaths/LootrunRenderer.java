/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.lootrunpaths;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wynntils.core.components.Managers;
import com.wynntils.features.LootrunFeature;
import com.wynntils.services.lootrunpaths.type.BlockValidness;
import com.wynntils.services.lootrunpaths.type.ColoredPath;
import com.wynntils.services.lootrunpaths.type.ColoredPosition;
import com.wynntils.services.lootrunpaths.type.LootrunNote;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.PosUtils;
import com.wynntils.utils.render.buffered.CustomRenderType;
import com.wynntils.utils.type.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class LootrunRenderer {
    private static final MultiBufferSource.BufferSource BUFFER_SOURCE =
            MultiBufferSource.immediate(new ByteBufferBuilder(256));

    public static void renderLootrun(PoseStack poseStack, LootrunPathInstance lootrun, int color) {
        if (lootrun == null) {
            return;
        }

        ClientLevel level = McUtils.mc().level;

        if (level == null) {
            return;
        }

        poseStack.pushPose();

        Camera camera = McUtils.mc().gameRenderer.getMainCamera();

        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));

        poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);

        Long2ObjectMap<List<ColoredPath>> points = lootrun.points();
        int renderDistance = McUtils.options().renderDistance().get();
        BlockPos pos = camera.getBlockPosition();
        ChunkPos origin = new ChunkPos(pos);

        for (int i = 0; i <= renderDistance; i++) {
            for (int j = 0; j <= renderDistance; j++) {
                int x = j + origin.x - (renderDistance / 2);
                int z = i + origin.z - (renderDistance / 2);
                ChunkPos chunk = new ChunkPos(x, z);
                if (!level.hasChunk(chunk.x, chunk.z)) {
                    continue;
                }

                long chunkLong = chunk.toLong();

                if (points.containsKey(chunkLong)) {
                    renderPoints(poseStack, points, chunkLong);
                }

                if (lootrun.chests().containsKey(chunkLong)) {
                    renderChests(poseStack, lootrun, color, chunkLong);
                }

                if (Managers.Feature.getFeatureInstance(LootrunFeature.class)
                                .showNotes
                                .get()
                        && lootrun.notes().containsKey(chunkLong)) {
                    renderNotes(poseStack, lootrun, color, chunkLong);
                }
            }
        }

        poseStack.popPose();
    }

    private static void renderNotes(PoseStack poseStack, LootrunPathInstance lootrun, int color, long chunkLong) {
        List<LootrunNote> notes = lootrun.notes().get(chunkLong);

        Font font = McUtils.mc().font;

        for (LootrunNote note : notes) {
            Position position = note.position();
            poseStack.pushPose();
            poseStack.translate(position.x(), position.y() + 2, position.z());
            poseStack.mulPose(McUtils.mc().gameRenderer.getMainCamera().rotation());
            poseStack.scale(-0.025f, -0.025f, 0.025f);
            Matrix4f pose = poseStack.last().pose();
            List<FormattedCharSequence> lines = font.split(note.component(), 200);
            int offsetY = -(font.lineHeight * lines.size()) / 2;
            for (FormattedCharSequence line : lines) {
                int offsetX = -font.width(line) / 2;
                font.drawInBatch(
                        line,
                        offsetX,
                        offsetY,
                        color,
                        false,
                        pose,
                        BUFFER_SOURCE,
                        Font.DisplayMode.NORMAL,
                        0x80000000,
                        0xf000f0);
                offsetY += font.lineHeight + 2;
            }
            poseStack.popPose();
        }
    }

    private static void renderChests(PoseStack poseStack, LootrunPathInstance lootrun, int color, long chunkLong) {
        VertexConsumer consumer = BUFFER_SOURCE.getBuffer(RenderType.lines());
        Set<BlockPos> chests = lootrun.chests().get(chunkLong);

        float red = ((float) FastColor.ARGB32.red(color)) / 255;
        float green = ((float) FastColor.ARGB32.green(color)) / 255;
        float blue = ((float) FastColor.ARGB32.blue(color)) / 255;

        for (BlockPos chest : chests) {
            // Wynncraft requested that chest highlights are not rendered on these blocks
            BlockState block = McUtils.mc().level.getBlockState(chest);
            if (block.is(Blocks.BARRIER) || block.is(Blocks.AIR)) continue;

            LevelRenderer.renderLineBox(poseStack, consumer, new AABB(chest), red, green, blue, 1f);
        }

        BUFFER_SOURCE.endBatch();
    }

    private static void renderPoints(PoseStack poseStack, Long2ObjectMap<List<ColoredPath>> points, long chunkLong) {
        List<ColoredPath> locations = points.get(chunkLong);

        Level level = McUtils.mc().level;
        if (level == null) return;

        switch (Managers.Feature.getFeatureInstance(LootrunFeature.class)
                .pathType
                .get()) {
            case TEXTURED -> renderTexturedLootrunPoints(poseStack, locations, level, CustomRenderType.LOOTRUN_QUAD);
            case LINE -> renderNonTexturedLootrunPoints(poseStack, locations, level, CustomRenderType.LOOTRUN_LINE);
        }
    }

    private static void renderNonTexturedLootrunPoints(
            PoseStack poseStack, List<ColoredPath> locations, Level level, RenderType renderType) {
        for (ColoredPath locationsInRoute : locations) {
            VertexConsumer consumer = BUFFER_SOURCE.getBuffer(renderType);
            Matrix4f lastMatrix = poseStack.last().pose();
            boolean sourceBatchEnded = false;

            ColoredPath toRender = new ColoredPath(new ArrayList<>());

            boolean pauseDraw = false;
            BlockPos lastBlockPos = null;

            for (ColoredPosition point : locationsInRoute.points()) {
                BlockPos blockPos = PosUtils.newBlockPos(point.position());

                if (blockPos.equals(lastBlockPos)) { // Do not recalculate block validness
                    if (!toRender.points().isEmpty()) {
                        toRender.points().add(point);
                    }
                } else {
                    BlockValidness blockValidness = BlockValidness.checkBlockValidness(level, point);

                    if (blockValidness == BlockValidness.VALID) {
                        pauseDraw = false;
                        if (sourceBatchEnded) {
                            consumer = BUFFER_SOURCE.getBuffer(renderType);
                            sourceBatchEnded = false;
                        }
                        renderQueuedPoints(consumer, lastMatrix, toRender);
                        toRender.points().clear();
                    } else if (blockValidness == BlockValidness.HAS_BARRIER) {
                        pauseDraw = true;
                        toRender.points().clear();
                    } else {
                        pauseDraw = false;
                        toRender.points().add(point);
                        continue;
                    }
                }

                lastBlockPos = blockPos;

                if (!pauseDraw) {
                    renderPoint(consumer, lastMatrix, point);
                } else if (!sourceBatchEnded) {
                    BUFFER_SOURCE.endBatch();
                    sourceBatchEnded = true;
                }
            }
            if (!sourceBatchEnded) {
                renderQueuedPoints(consumer, lastMatrix, toRender);
                BUFFER_SOURCE.endBatch();
            }
        }
    }

    private static void renderTexturedLootrunPoints(
            PoseStack poseStack, List<ColoredPath> locations, Level level, RenderType renderType) {
        Camera camera = McUtils.mc().gameRenderer.getMainCamera();
        poseStack.pushPose();
        poseStack.translate(camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        for (ColoredPath locationsInRoute : locations) {
            VertexConsumer consumer = BUFFER_SOURCE.getBuffer(renderType);
            List<Pair<ColoredPosition, ColoredPosition>> toRender = new ArrayList<>();
            boolean sourceBatchEnded = false;
            BlockPos lastBlockPos = null;

            boolean pauseDraw = false;

            for (int i = 0; i < locationsInRoute.points().size() - 1; i += 10) {
                ColoredPosition point = locationsInRoute.points().get(i);
                BlockPos blockPos = PosUtils.newBlockPos(point.position());

                ColoredPosition end = locationsInRoute
                        .points()
                        .get(Math.min(locationsInRoute.points().size() - 1, i + 1));
                Pair<ColoredPosition, ColoredPosition> pointPair = new Pair<>(point, end);

                if (blockPos.equals(lastBlockPos)) { // Do not recalculate block validness
                    if (!toRender.isEmpty()) {
                        toRender.add(pointPair);
                    }
                } else {
                    BlockValidness blockValidness = BlockValidness.checkBlockValidness(level, point);

                    if (blockValidness == BlockValidness.VALID) {
                        pauseDraw = false;
                        if (sourceBatchEnded) {
                            consumer = BUFFER_SOURCE.getBuffer(renderType);
                            sourceBatchEnded = false;
                        }
                        renderTexturedQueuedPoints(toRender, poseStack, consumer);
                        toRender.clear();
                    } else if (blockValidness == BlockValidness.HAS_BARRIER) {
                        pauseDraw = true;
                        toRender.clear();
                    } else {
                        pauseDraw = false;
                        toRender.add(pointPair);
                        continue;
                    }
                }

                lastBlockPos = blockPos;

                if (!pauseDraw) {
                    renderTexturedPoint(pointPair, poseStack, consumer);
                } else if (!sourceBatchEnded) {
                    BUFFER_SOURCE.endBatch();
                    sourceBatchEnded = true;
                }
            }
            if (!sourceBatchEnded) {
                renderTexturedQueuedPoints(toRender, poseStack, consumer);
                BUFFER_SOURCE.endBatch();
            }
        }
        poseStack.popPose();
    }

    private static void renderQueuedPoints(VertexConsumer consumer, Matrix4f lastMatrix, ColoredPath toRender) {
        for (ColoredPosition position : toRender.points()) {
            renderPoint(consumer, lastMatrix, position);
        }
    }

    private static void renderPoint(VertexConsumer consumer, Matrix4f lastMatrix, ColoredPosition coloredPosition) {
        Position position = coloredPosition.position();
        int pathColor = coloredPosition.color();
        consumer.addVertex(lastMatrix, (float) position.x(), (float) position.y(), (float) position.z())
                .setColor(pathColor)
                .setNormal(0, 0, 1);
    }

    private static void renderTexturedQueuedPoints(
            List<Pair<ColoredPosition, ColoredPosition>> pointPairList,
            PoseStack poseStack,
            VertexConsumer vertexConsumer) {
        for (Pair<ColoredPosition, ColoredPosition> pointPair : pointPairList) {
            renderTexturedPoint(pointPair.a(), pointPair.b(), poseStack, vertexConsumer);
        }
    }

    private static void renderTexturedPoint(
            Pair<ColoredPosition, ColoredPosition> pointPair, PoseStack poseStack, VertexConsumer vertexConsumer) {
        renderTexturedPoint(pointPair.a(), pointPair.b(), poseStack, vertexConsumer);
    }

    private static void renderTexturedPoint(
            ColoredPosition start, ColoredPosition end, PoseStack poseStack, VertexConsumer vertexConsumer) {
        Vector3f camPos =
                McUtils.mc().gameRenderer.getMainCamera().getPosition().toVector3f();
        Vector3f startVec = start.position().toVector3f();
        Vector3f endVec = end.position().toVector3f();
        int color = start.color();

        // vertex position delta to starting point
        Vector3f pos1 = new Vector3f(-0.5f, 0.24f, -0.5f);
        Vector3f pos2 = new Vector3f(0.5f, 0.24f, -0.5f);
        Vector3f pos3 = new Vector3f(0.5f, 0.24f, 0.5f);
        Vector3f pos4 = new Vector3f(-0.5f, 0.24f, 0.5f);

        Vector3f direction =
                new Vector3f(endVec.x, endVec.y, endVec.z).sub(startVec).normalize();

        // rotation angle to point surface normal to end position
        // rotate the angle so the arrow point to the end position instead of surface normal
        float xAngle = (float) ((float) Math.acos(direction.y / direction.length()) - Math.PI / 2);
        float yAngle = (float) Math.atan2(direction.x, direction.z);

        Quaternionf yRot = new Quaternionf().rotateY(yAngle);
        Vector3f xRotAxis = new Vector3f(1, 0, 0).rotate(yRot);
        Quaternionf xRot = new Quaternionf().rotateAxis(xAngle, xRotAxis);

        // apply vertex rotation
        pos1.rotate(yRot).rotate(xRot);
        pos2.rotate(yRot).rotate(xRot);
        pos3.rotate(yRot).rotate(xRot);
        pos4.rotate(yRot).rotate(xRot);

        // transform position back to world space and then to position camera delta
        pos1 = pos1.add(startVec).sub(camPos);
        pos2 = pos2.add(startVec).sub(camPos);
        pos3 = pos3.add(startVec).sub(camPos);
        pos4 = pos4.add(startVec).sub(camPos);

        vertexConsumer
                .addVertex(poseStack.last().pose(), pos1.x, pos1.y, pos1.z)
                .setUv(0, 1)
                .setColor(color);
        vertexConsumer
                .addVertex(poseStack.last().pose(), pos2.x, pos2.y, pos2.z)
                .setUv(0, 0)
                .setColor(color);
        vertexConsumer
                .addVertex(poseStack.last().pose(), pos3.x, pos3.y, pos3.z)
                .setUv(1, 0)
                .setColor(color);
        vertexConsumer
                .addVertex(poseStack.last().pose(), pos4.x, pos4.y, pos4.z)
                .setUv(1, 1)
                .setColor(color);
    }
}
