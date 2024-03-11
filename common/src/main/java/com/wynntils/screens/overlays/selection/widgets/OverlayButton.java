/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.overlays.selection.widgets;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.consumers.overlays.CustomNameProperty;
import com.wynntils.core.consumers.overlays.Overlay;
import com.wynntils.core.persisted.config.OverlayGroupHolder;
import com.wynntils.core.text.StyledText;
import com.wynntils.features.overlays.CustomBarsOverlayFeature;
import com.wynntils.features.overlays.InfoBoxFeature;
import com.wynntils.overlays.custombars.CustomBarOverlayBase;
import com.wynntils.overlays.infobox.InfoBoxOverlay;
import com.wynntils.screens.base.widgets.WynntilsButton;
import com.wynntils.screens.overlays.selection.OverlaySelectionScreen;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.ComponentUtils;
import com.wynntils.utils.mc.KeyboardUtils;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class OverlayButton extends WynntilsButton {
    private static final CustomColor ENABLED_COLOR = new CustomColor(0, 116, 0, 255);
    private static final CustomColor DISABLED_COLOR = new CustomColor(60, 60, 60, 255);
    private static final CustomColor DISABLED_FEATURE_COLOR = new CustomColor(120, 0, 0, 255);
    private static final CustomColor ENABLED_COLOR_BORDER = new CustomColor(0, 220, 0, 255);
    private static final CustomColor DISABLED_COLOR_BORDER = new CustomColor(0, 0, 0, 255);
    private static final CustomColor DISABLED_FEATURE_COLOR_BORDER = new CustomColor(255, 0, 0, 255);

    private final float translationX;
    private final float translationY;
    private final int overlayId;
    private final List<Component> descriptionTooltip;
    private final Overlay overlay;
    private final String textToRender;

    public OverlayButton(int x, int y, int width, int height, Overlay overlay, float translationX, float translationY) {
        super(x, y, width, height, Component.literal(overlay.getTranslatedName()));

        this.overlay = overlay;
        this.translationX = translationX;
        this.translationY = translationY;

        // Use custom name of overlay if present
        if (overlay instanceof CustomNameProperty customNameProperty) {
            if (!customNameProperty.getCustomName().get().isEmpty()) {
                textToRender = customNameProperty.getCustomName().get();
            } else {
                textToRender = overlay.getTranslatedName();
            }
        } else {
            textToRender = overlay.getTranslatedName();
        }

        // Display a tooltip with delete instructions for info boxes and custom bars.
        // Also get the ID to be used when deleting
        if (overlay instanceof InfoBoxOverlay infoBoxOverlay) {
            descriptionTooltip = ComponentUtils.wrapTooltips(
                    List.of(Component.translatable("screens.wynntils.overlaySelection.delete", textToRender)), 150);

            overlayId = infoBoxOverlay.getId();
        } else if (overlay instanceof CustomBarOverlayBase customBarOverlayBase) {
            descriptionTooltip = ComponentUtils.wrapTooltips(
                    List.of(Component.translatable("screens.wynntils.overlaySelection.delete", textToRender)), 150);

            overlayId = customBarOverlayBase.getId();
        } else {
            descriptionTooltip = List.of();
            overlayId = -1;
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();
        boolean enabled = Managers.Overlay.isEnabled(overlay);

        RenderUtils.drawRect(poseStack, getBorderColor(enabled).withAlpha(100), getX(), getY(), 0, width, height);

        RenderUtils.drawRectBorders(
                poseStack, getRectColor(enabled), getX(), getY(), getX() + width, getY() + height, 1, 2);

        FontRenderer.getInstance()
                .renderScrollingString(
                        poseStack,
                        StyledText.fromString(textToRender),
                        getX() + 2,
                        getY() + (height / 2f),
                        width - 4,
                        translationX,
                        translationY,
                        CommonColors.WHITE,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL,
                        1.0f);

        // Display tooltip, if ID is not -1 then it should be an info box/custom bar
        if (isHovered && overlayId != -1) {
            McUtils.mc()
                    .screen
                    .setTooltipForNextRenderPass(Lists.transform(descriptionTooltip, Component::getVisualOrderText));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Delete overlay when shift right clicked, if ID is not -1 then it should be an info box/custom bar
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && KeyboardUtils.isShiftDown() && overlayId != -1) {
            deleteOverlay();
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onPress() {
        if (McUtils.mc().screen instanceof OverlaySelectionScreen overlaySelectionScreen) {
            overlaySelectionScreen.setSelectedOverlay(overlay);
        }
    }

    public Overlay getOverlay() {
        return overlay;
    }

    private void deleteOverlay() {
        // Get the group holder
        OverlayGroupHolder overlayGroupHolder = getGroupHolder();

        if (overlayGroupHolder == null) {
            WynntilsMod.error("Failed to delete, overlay group not found for overlay " + overlay.getJsonName());
            return;
        }

        // Delete the overlay
        Managers.Overlay.removeIdFromOverlayGroup(overlayGroupHolder, overlayId);

        // Reload config
        Managers.Config.loadConfigOptions(true, false);
        Managers.Config.saveConfig();

        // Remove the overlay from the list
        if (McUtils.mc().screen instanceof OverlaySelectionScreen overlaySelectionScreen) {
            overlaySelectionScreen.deleteOverlay(overlay);
        }
    }

    private OverlayGroupHolder getGroupHolder() {
        // Get the parent feature of the overlay
        Feature feature = overlay instanceof InfoBoxOverlay
                ? Managers.Feature.getFeatureInstance(InfoBoxFeature.class)
                : Managers.Feature.getFeatureInstance(CustomBarsOverlayFeature.class);

        // Loop through holders, if holder contains this overlay then that is the one
        for (OverlayGroupHolder group : Managers.Overlay.getFeatureOverlayGroups(feature)) {
            if (group.getOverlays().contains(overlay)) {
                return group;
            }
        }

        return null;
    }

    private CustomColor getBorderColor(boolean enabled) {
        if (McUtils.mc().screen instanceof OverlaySelectionScreen overlaySelectionScreen) {
            if (overlaySelectionScreen.getSelectedOverlay() == overlay) {
                return CommonColors.GRAY;
            }
        }

        if (!overlay.isParentEnabled()) return DISABLED_FEATURE_COLOR_BORDER;

        return enabled ? ENABLED_COLOR_BORDER : DISABLED_COLOR_BORDER;
    }

    private CustomColor getRectColor(boolean enabled) {
        if (McUtils.mc().screen instanceof OverlaySelectionScreen overlaySelectionScreen) {
            if (overlaySelectionScreen.getSelectedOverlay() == overlay) {
                return CommonColors.WHITE;
            }
        }

        if (!overlay.isParentEnabled()) return DISABLED_FEATURE_COLOR;

        return enabled ? ENABLED_COLOR : DISABLED_COLOR;
    }
}
