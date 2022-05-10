/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraftforge.eventbus.api.Event;

public class RenderLevelLastEvent extends Event {
    private final LevelRenderer levelRenderer;
    private final PoseStack poseStack;
    private final float partialTick;
    private final Matrix4f projectionMatrix;
    private final long startNanos;

    public RenderLevelLastEvent(
            LevelRenderer levelRenderer,
            PoseStack poseStack,
            float partialTick,
            Matrix4f projectionMatrix,
            long startNanos) {
        this.levelRenderer = levelRenderer;
        this.poseStack = poseStack;
        this.partialTick = partialTick;
        this.projectionMatrix = projectionMatrix;
        this.startNanos = startNanos;
    }

    public LevelRenderer getLevelRenderer() {
        return this.levelRenderer;
    }

    public PoseStack getPoseStack() {
        return this.poseStack;
    }

    public float getPartialTick() {
        return this.partialTick;
    }

    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }

    public long getStartNanos() {
        return this.startNanos;
    }
}
