/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.lists;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.core.features.overlays.OverlayManager;
import com.wynntils.mc.render.Texture;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.screens.OverlaySelectionScreen;
import com.wynntils.screens.lists.entries.OverlayEntry;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;

public class OverlayList extends ContainerObjectSelectionList<OverlayEntry> {
    private static final int ITEM_HEIGHT = 25;
    private final Set<Overlay> overlays;

    public OverlayList(OverlaySelectionScreen screen) {
        super(
                McUtils.mc(),
                screen.width,
                screen.height,
                screen.height / 10 + 15,
                screen.height / 10 + Texture.OVERLAY_SELECTION_GUI.height() - 15,
                ITEM_HEIGHT);

        this.overlays = new HashSet<>(OverlayManager.getOverlays());

        for (Overlay overlay : this.overlays) {
            this.addEntry(new OverlayEntry(overlay));
        }

        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderList(PoseStack poseStack, int x, int y, int mouseX, int mouseY, float partialTick) {
        int itemCount = this.getItemCount();

        int renderedCount = 0;

        for (int i = 0; i < itemCount; i++) {
            int top = this.y0 + 1 + renderedCount * this.itemHeight + this.headerHeight;
            int bottom = top + this.itemHeight;
            if (getRowTop(i) < this.y0 || bottom > this.y1) continue;

            OverlayEntry entry = this.getEntry(i);

            entry.render(
                    poseStack,
                    i,
                    top + 1,
                    this.getRowLeft(),
                    this.getRowWidth(),
                    this.itemHeight,
                    mouseX,
                    mouseY,
                    Objects.equals(this.getHovered(), entry),
                    partialTick);

            renderedCount++;
        }
    }

    @Override
    protected int getScrollbarPosition() {
        return super.getScrollbarPosition() - 43;
    }

    @Override
    protected int getRowTop(int index) {
        return this.y0 - (int) this.getScrollAmount() + index * this.itemHeight + this.headerHeight + 1;
    }

    @Override
    public int getRowWidth() {
        return 161;
    }

    @Override
    public int getRowLeft() {
        return this.x0 + this.width / 2 - this.getRowWidth() / 2 - 10;
    }

    public static int getItemHeight() {
        return ITEM_HEIGHT;
    }
}
