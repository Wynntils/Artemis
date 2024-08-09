/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class EntityNameTagRenderEvent extends Event implements ICancellableEvent {
    private final Entity entity;
    private final Component displayName;
    private final PoseStack poseStack;
    private final MultiBufferSource buffer;
    private final int packedLight;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final Font font;

    private float backgroundOpacity;

    public EntityNameTagRenderEvent(
            Entity entity,
            Component displayName,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            EntityRenderDispatcher entityRenderDispatcher,
            Font font,
            float backgroundOpacity) {
        this.entity = entity;
        this.displayName = displayName;
        this.poseStack = poseStack;
        this.buffer = buffer;
        this.packedLight = packedLight;
        this.entityRenderDispatcher = entityRenderDispatcher;
        this.font = font;
        this.backgroundOpacity = backgroundOpacity;
    }

    public Entity getEntity() {
        return entity;
    }

    public Component getDisplayName() {
        return displayName;
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

    public EntityRenderDispatcher getEntityRenderDispatcher() {
        return entityRenderDispatcher;
    }

    public Font getFont() {
        return font;
    }

    public float getBackgroundOpacity() {
        return backgroundOpacity;
    }

    public void setBackgroundOpacity(float backgroundOpacity) {
        this.backgroundOpacity = backgroundOpacity;
    }
}
