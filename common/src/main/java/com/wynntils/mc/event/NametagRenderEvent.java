/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class NametagRenderEvent extends Event {
    private final AbstractClientPlayer entity;
    private final Component displayName;
    private final PoseStack poseStack;
    private final MultiBufferSource buffer;
    private final int packedLight;

    private final List<MutableComponent> injectedLines;
    private float injectedLinesScale;

    public NametagRenderEvent(
            AbstractClientPlayer entity,
            Component displayName,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight) {
        this.entity = entity;
        this.displayName = displayName;
        this.poseStack = poseStack;
        this.buffer = buffer;
        this.packedLight = packedLight;
        this.injectedLines = new ArrayList<>();
        this.injectedLinesScale = 1f;
    }

    public AbstractClientPlayer getEntity() {
        return entity;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public int getPackedLight() {
        return packedLight;
    }

    public MultiBufferSource getBuffer() {
        return buffer;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public List<MutableComponent> getInjectedLines() {
        return injectedLines;
    }

    public float getInjectedLinesScale() {
        return injectedLinesScale;
    }

    public void addInjectedLine(MutableComponent component) {
        injectedLines.add(component);
    }

    public void setInjectedLinesScale(float scale) {
        injectedLinesScale = scale;
    }
}
