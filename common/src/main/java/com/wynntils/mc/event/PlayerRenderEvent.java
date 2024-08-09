/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.bus.api.Event;

public class PlayerRenderEvent extends Event {
    private final AbstractClientPlayer entity;
    private final float entityYaw;
    private final float partialTicks;
    private final PoseStack poseStack;
    private final MultiBufferSource buffer;
    private final int packedLight;

    public PlayerRenderEvent(
            AbstractClientPlayer entity,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        this.entity = entity;
        this.entityYaw = entityYaw;
        this.partialTicks = partialTicks;
        this.poseStack = poseStack;
        this.buffer = buffer;
        this.packedLight = packedLight;
    }

    public AbstractClientPlayer getPlayer() {
        return entity;
    }

    public float getEntityYaw() {
        return entityYaw;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public MultiBufferSource getBuffer() {
        return buffer;
    }

    public int getPackedLight() {
        return packedLight;
    }
}
