/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.events.WynntilsEvent;
import net.minecraft.client.gui.screens.Screen;

public class ScreenRenderEvent extends WynntilsEvent {
    private final Screen screen;
    private final PoseStack poseStack;
    private final int mouseX;
    private final int mouseY;
    private final float partialTick;

    public ScreenRenderEvent(Screen screen, PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.screen = screen;
        this.poseStack = poseStack;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.partialTick = partialTick;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public float getPartialTick() {
        return partialTick;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public Screen getScreen() {
        return screen;
    }
}
