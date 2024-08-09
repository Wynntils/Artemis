/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.Event;

public abstract class SlotRenderEvent extends Event {
    private final GuiGraphics guiGraphics;
    private final Screen screen;
    private final Slot slot;

    protected SlotRenderEvent(GuiGraphics guiGraphics, Screen screen, Slot slot) {
        this.guiGraphics = guiGraphics;
        this.screen = screen;
        this.slot = slot;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public PoseStack getPoseStack() {
        return guiGraphics.pose();
    }

    public Screen getScreen() {
        return screen;
    }

    public Slot getSlot() {
        return slot;
    }

    public static class Pre extends SlotRenderEvent {
        public Pre(GuiGraphics guiGraphics, Screen screen, Slot slot) {
            super(guiGraphics, screen, slot);
        }
    }

    public static class CountPre extends SlotRenderEvent {
        public CountPre(GuiGraphics guiGraphics, Screen screen, Slot slot) {
            super(guiGraphics, screen, slot);
        }
    }

    public static class Post extends SlotRenderEvent {
        public Post(GuiGraphics guiGraphics, Screen screen, Slot slot) {
            super(guiGraphics, screen, slot);
        }
    }
}
