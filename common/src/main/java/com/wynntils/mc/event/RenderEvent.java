/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

public abstract class RenderEvent extends Event {
    private final PoseStack poseStack;
    private final float partialTicks;
    private final Window window;
    private final ElementType type;

    public RenderEvent(PoseStack poseStack, float partialTicks, Window window, ElementType type) {
        this.poseStack = poseStack;
        this.partialTicks = partialTicks;
        this.window = window;
        this.type = type;
    }

    public ElementType getType() {
        return type;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public Window getWindow() {
        return window;
    }

    public enum ElementType {
        GUI, // This is called before and after Gui#render
        Crosshair,
        HealthBar,
        FoodBar
    }

    @Cancelable
    public static class Pre extends RenderEvent {
        public Pre(PoseStack poseStack, float partialTicks, Window window, ElementType type) {
            super(poseStack, partialTicks, window, type);
        }
    }

    public static class Post extends RenderEvent {
        public Post(PoseStack poseStack, float partialTicks, Window window, ElementType type) {
            super(poseStack, partialTicks, window, type);
        }
    }
}
