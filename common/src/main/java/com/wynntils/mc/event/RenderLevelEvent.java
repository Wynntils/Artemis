/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.neoforged.bus.api.Event;
import org.joml.Matrix4f;

// Note: Neither of these events provide a PoseStack, as it'd be just an empty stack.
public abstract class RenderLevelEvent extends Event {
    private final LevelRenderer levelRenderer;
    private final DeltaTracker deltaTracker;
    private final Matrix4f projectionMatrix;
    private final Camera camera;

    protected RenderLevelEvent(
            LevelRenderer levelRenderer, DeltaTracker deltaTracker, Matrix4f projectionMatrix, Camera camera) {
        this.levelRenderer = levelRenderer;
        this.deltaTracker = deltaTracker;
        this.projectionMatrix = projectionMatrix;
        this.camera = camera;
    }

    public LevelRenderer getLevelRenderer() {
        return this.levelRenderer;
    }

    public DeltaTracker getDeltaTracker() {
        return this.deltaTracker;
    }

    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }

    public Camera getCamera() {
        return camera;
    }

    public static class Pre extends RenderLevelEvent {
        public Pre(LevelRenderer levelRenderer, DeltaTracker deltaTracker, Matrix4f projectionMatrix, Camera camera) {
            super(levelRenderer, deltaTracker, projectionMatrix, camera);
        }
    }

    public static class Post extends RenderLevelEvent {
        public Post(LevelRenderer levelRenderer, DeltaTracker deltaTracker, Matrix4f projectionMatrix, Camera camera) {
            super(levelRenderer, deltaTracker, projectionMatrix, camera);
        }
    }
}
