/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.mc.event;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * This event is fired when an item tooltip is rendered.
 * <p>
 * However, starting Minecraft 1.20, there is no longer a concrete way of rendering item tooltips.
 * We still have {@link GuiGraphics#renderTooltip(Font, ItemStack, int, int)}, but some screens tend to convert an item into tooltip themselves.
 * This leads to us having to call this event from 3 locations, which the secondary location being {@link net.minecraft.client.gui.screens.inventory.AbstractContainerScreen#renderTooltip(GuiGraphics, int, int)}.`
 * The third location is patched in by Forge, handled in {@link ForgeGuiGraphicsMixin}.
 */
public abstract class ItemTooltipRenderEvent extends Event {
    private final GuiGraphics guiGraphics;
    protected ItemStack itemStack;
    protected int mouseX;
    protected int mouseY;

    protected ItemTooltipRenderEvent(GuiGraphics guiGraphics, ItemStack itemStack, int mouseX, int mouseY) {
        this.guiGraphics = guiGraphics;
        this.itemStack = itemStack;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public PoseStack getPoseStack() {
        return guiGraphics.pose();
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getMouseX() {
        return mouseX;
    }

    public int getMouseY() {
        return mouseY;
    }

    public static class Pre extends ItemTooltipRenderEvent implements ICancellableEvent {
        private List<Component> tooltips;

        public Pre(GuiGraphics guiGraphics, ItemStack itemStack, List<Component> tooltips, int mouseX, int mouseY) {
            super(guiGraphics, itemStack, mouseX, mouseY);
            setTooltips(tooltips);
        }

        public List<Component> getTooltips() {
            return tooltips;
        }

        public void setTooltips(List<Component> tooltips) {
            this.tooltips = Collections.unmodifiableList(tooltips);
        }

        public void setMouseX(int mouseX) {
            this.mouseX = mouseX;
        }

        public void setMouseY(int mouseY) {
            this.mouseY = mouseY;
        }

        public void setItemStack(ItemStack itemStack) {
            this.itemStack = itemStack;
        }
    }

    public static class Post extends ItemTooltipRenderEvent {
        public Post(GuiGraphics guiGraphics, ItemStack itemStack, int mouseX, int mouseY) {
            super(guiGraphics, itemStack, mouseX, mouseY);
        }
    }
}
