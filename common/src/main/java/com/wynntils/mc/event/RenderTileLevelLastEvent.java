/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.neoforged.bus.api.Event;
import org.joml.Matrix4f;

public class RenderTileLevelLastEvent extends Event {
    private final LevelRenderer levelRenderer;
    private final PoseStack poseStack;
    private final DeltaTracker deltaTracker;
    private final Matrix4f projectionMatrix;
    private final Camera camera;

    public RenderTileLevelLastEvent(
            LevelRenderer levelRenderer,
            PoseStack poseStack,
            DeltaTracker deltaTracker,
            Matrix4f projectionMatrix,
            Camera camera) {
        this.levelRenderer = levelRenderer;
        this.poseStack = poseStack;
        this.deltaTracker = deltaTracker;
        this.projectionMatrix = projectionMatrix;
        this.camera = camera;
    }

    public LevelRenderer getLevelRenderer() {
        return this.levelRenderer;
    }

    public PoseStack getPoseStack() {
        return this.poseStack;
    }

    public DeltaTracker getDeltaTracker() {
        return deltaTracker;
    }

    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }

    public Camera getCamera() {
        return camera;
    }
}
