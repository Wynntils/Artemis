/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.lists.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.config.ConfigManager;
import com.wynntils.core.features.overlays.Overlay;
import com.wynntils.core.features.overlays.OverlayManager;
import com.wynntils.mc.render.FontRenderer;
import com.wynntils.mc.render.RenderUtils;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.screens.OverlayManagementScreen;
import com.wynntils.screens.lists.OverlayList;
import com.wynntils.utils.objects.CommonColors;
import com.wynntils.utils.objects.CustomColor;
import java.util.List;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class OverlayEntry extends ContainerObjectSelectionList.Entry<OverlayEntry> {
    private static final CustomColor ENABLED_COLOR = new CustomColor(0, 116, 0, 255);
    private static final CustomColor DISABLED_COLOR = new CustomColor(116, 0, 0, 255);
    private static final CustomColor ENABLED_COLOR_BORDER = new CustomColor(0, 220, 0, 255);
    private static final CustomColor DISABLED_COLOR_BORDER = new CustomColor(220, 0, 0, 255);

    private static final List<Component> HELP_TOOLTIP_LINES = List.of(
            new TextComponent("Left click on the overlay to edit it."),
            new TextComponent("Right click on the overlay to disable/enable it."));

    private final Overlay overlay;

    public OverlayEntry(Overlay overlay) {
        this.overlay = overlay;
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return ImmutableList.of();
    }

    @Override
    public void render(
            PoseStack poseStack,
            int index,
            int top,
            int left,
            int width,
            int height,
            int mouseX,
            int mouseY,
            boolean isMouseOver,
            float partialTick) {
        poseStack.pushPose();
        poseStack.translate(left, top, 0);

        boolean enabled = OverlayManager.isEnabled(this.overlay);
        int y = index != 0 ? 2 : 0;

        RenderUtils.drawRect(
                poseStack,
                (enabled ? ENABLED_COLOR_BORDER : DISABLED_COLOR_BORDER).withAlpha(100),
                0,
                y,
                0,
                width,
                height - y);
        RenderUtils.drawRectBorders(poseStack, enabled ? ENABLED_COLOR : DISABLED_COLOR, 0, y, width, height, 1, 2);

        poseStack.translate(0, 0, 1);
        String translatedName = this.overlay.getTranslatedName();
        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        translatedName,
                        3,
                        (OverlayList.getItemHeight()
                                        - FontRenderer.getInstance()
                                                        .calculateRenderHeight(List.of(translatedName), width)
                                                / 2f)
                                / 2f,
                        width,
                        CommonColors.WHITE,
                        FontRenderer.TextAlignment.LEFT_ALIGNED,
                        FontRenderer.TextShadow.NORMAL);

        poseStack.popPose();

        if (this.isMouseOver(mouseX, mouseY)) {
            RenderUtils.drawTooltipAt(
                    poseStack,
                    mouseX,
                    mouseY,
                    100,
                    HELP_TOOLTIP_LINES,
                    FontRenderer.getInstance().getFont(),
                    false);
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return ImmutableList.of();
    }

    public Overlay getOverlay() {
        return overlay;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // right click
        if (button == 1) {
            ConfigManager.getConfigHolders().stream()
                    .filter(configHolder -> configHolder.getParent() == overlay
                            && configHolder.getFieldName().equals("userEnabled"))
                    .findFirst()
                    .ifPresent(configHolder -> configHolder.setValue(!overlay.isEnabled()));
            ConfigManager.saveConfig();
            return true;
        }

        if (!overlay.isEnabled()) return false;

        McUtils.mc().setScreen(new OverlayManagementScreen(this.overlay));
        return true;
    }
}
