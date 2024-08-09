/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class ContainerLabelRenderEvent extends Event implements ICancellableEvent {
    private final AbstractContainerScreen<?> screen;
    private final GuiGraphics guiGraphics;
    private final float x;
    private final float y;

    private int color;

    protected ContainerLabelRenderEvent(
            AbstractContainerScreen<?> screen, GuiGraphics guiGraphics, float x, float y, int color) {
        this.screen = screen;
        this.guiGraphics = guiGraphics;
        this.x = x;
        this.y = y;

        this.color = color;
    }

    public AbstractContainerScreen<?> getScreen() {
        return screen;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public PoseStack getPoseStack() {
        return guiGraphics.pose();
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public static class ContainerLabel extends ContainerLabelRenderEvent {
        private Component containerLabel;

        public ContainerLabel(
                AbstractContainerScreen<?> screen,
                GuiGraphics guiGraphics,
                int color,
                float x,
                float y,
                Component containerLabel) {
            super(screen, guiGraphics, x, y, color);
            this.containerLabel = containerLabel;
        }

        public Component getContainerLabel() {
            return containerLabel;
        }

        public void setContainerLabel(Component containerLabel) {
            this.containerLabel = containerLabel;
        }
    }

    public static class InventoryLabel extends ContainerLabelRenderEvent {
        private Component inventoryLabel;

        public InventoryLabel(
                AbstractContainerScreen<?> screen,
                GuiGraphics guiGraphics,
                int color,
                float x,
                float y,
                Component inventoryLabel) {
            super(screen, guiGraphics, x, y, color);
            this.inventoryLabel = inventoryLabel;
        }

        public Component getInventoryLabel() {
            return inventoryLabel;
        }

        public void setInventoryLabel(Component inventoryLabel) {
            this.inventoryLabel = inventoryLabel;
        }
    }
}
