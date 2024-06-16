/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.maps.widgets;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.screens.maps.PoiManagementScreen;
import com.wynntils.screens.maps.WaypointCreationScreen;
import com.wynntils.services.map.pois.CustomPoi;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.RenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public class PoiManagerWidget extends AbstractWidget {
    private final boolean selected;
    private final boolean selectionMode;
    private final Button editButton;
    private final Button deleteButton;
    private final Button upButton;
    private final Button downButton;
    private final Button selectButton;
    private final CustomColor color;
    private final CustomPoi poi;
    private final float dividedWidth;
    private final PoiManagementScreen managementScreen;

    public PoiManagerWidget(
            int x,
            int y,
            int width,
            int height,
            CustomPoi poi,
            PoiManagementScreen managementScreen,
            float dividedWidth,
            boolean selectionMode,
            boolean selected) {
        super(x, y, width, height, Component.literal(poi.getName()));
        this.poi = poi;
        this.managementScreen = managementScreen;
        this.dividedWidth = dividedWidth;
        this.selectionMode = selectionMode;
        this.selected = selected;

        int manageButtonsWidth = (int) (dividedWidth * 4);

        color = poi.getVisibility() == CustomPoi.Visibility.HIDDEN ? CommonColors.GRAY : CommonColors.WHITE;

        editButton = new Button.Builder(
                        Component.translatable("screens.wynntils.poiManagementGui.edit"),
                        // FIXME: null should be a WaypointLocation object (fix when porting PoiManagementScreen
                        (button) -> McUtils.mc().setScreen(WaypointCreationScreen.create(managementScreen, null)))
                .pos(x + width - 20 - (manageButtonsWidth * 2), y)
                .size(manageButtonsWidth, 20)
                .build();

        deleteButton = new Button.Builder(
                        Component.translatable("screens.wynntils.poiManagementGui.delete"), (button) -> {
                            managementScreen.deletePoi(poi);
                        })
                .pos(x + width - 20 - manageButtonsWidth, y)
                .size(manageButtonsWidth, 20)
                .build();

        upButton = new Button.Builder(Component.literal("ʌ"), (button) -> managementScreen.updatePoiPosition(poi, -1))
                .pos(x + width - 20, y)
                .size(10, 20)
                .build();

        downButton = new Button.Builder(Component.literal("v"), (button) -> managementScreen.updatePoiPosition(poi, 1))
                .pos(x + width - 10, y)
                .size(10, 20)
                .build();

        Component selectButtonText = selected
                ? Component.translatable("screens.wynntils.poiManagementGui.deselect")
                : Component.translatable("screens.wynntils.poiManagementGui.select");

        selectButton = new Button.Builder(selectButtonText, (button) -> managementScreen.selectPoi(poi))
                .pos(x + width - (manageButtonsWidth * 2 + 20), y)
                .size(manageButtonsWidth * 2 + 20, 20)
                .build();

        List<CustomPoi> pois = managementScreen.getPois();

        // Don't allow pois to be moved if at the top/bottom of the list
        if (pois.indexOf(poi) == 0) {
            upButton.active = false;
        }

        if (pois.indexOf(poi) == (pois.size() - 1)) {
            downButton.active = false;
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        PoseStack poseStack = guiGraphics.pose();

        renderIcon(poseStack);

        FontRenderer.getInstance()
                .renderScrollingText(
                        poseStack,
                        StyledText.fromString(poi.getName()),
                        getX() + (int) (dividedWidth * 3),
                        getY() + 10,
                        (int) (dividedWidth * 15),
                        color,
                        HorizontalAlignment.LEFT,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL,
                        1f);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(String.valueOf(poi.getLocation().getX())),
                        getX() + (int) (dividedWidth * 20),
                        getY() + 10,
                        color,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);

        Optional<Integer> poiY = poi.getLocation().getY();

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        poiY.map(integer -> StyledText.fromString(String.valueOf(integer)))
                                .orElse(StyledText.EMPTY),
                        getX() + (int) (dividedWidth * 23),
                        getY() + 10,
                        color,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        StyledText.fromString(String.valueOf(poi.getLocation().getZ())),
                        getX() + (int) (dividedWidth * 26),
                        getY() + 10,
                        color,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        TextShadow.NORMAL);

        // In selection mode we don't want the edit/delete/move buttons
        if (selectionMode) {
            selectButton.render(guiGraphics, mouseX, mouseY, partialTick);

            // Border to show selected pois, orange when selected, white if not
            RenderUtils.drawRectBorders(
                    poseStack,
                    selected ? CommonColors.ORANGE : CommonColors.WHITE,
                    getX(),
                    getY() + 1,
                    getX() + width,
                    getY() + height - 1,
                    0,
                    1f);
        } else {
            editButton.render(guiGraphics, mouseX, mouseY, partialTick);
            deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
            upButton.render(guiGraphics, mouseX, mouseY, partialTick);
            downButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            McUtils.playSoundUI(SoundEvents.EXPERIENCE_ORB_PICKUP);

            Models.Marker.USER_WAYPOINTS_PROVIDER.addLocation(
                    poi.getLocation().asLocation(), poi.getIcon(), poi.getColor(), poi.getColor());
            return true;
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            Models.Marker.USER_WAYPOINTS_PROVIDER.removeLocation(
                    poi.getLocation().asLocation());
            return true;
        }

        boolean clickedButton;

        // Determine if a button was clicked or should we select the widget
        if (selectionMode) {
            clickedButton = selectButton.mouseClicked(mouseX, mouseY, button);
        } else {
            clickedButton = editButton.mouseClicked(mouseX, mouseY, button)
                    || deleteButton.mouseClicked(mouseX, mouseY, button)
                    || upButton.mouseClicked(mouseX, mouseY, button)
                    || downButton.mouseClicked(mouseX, mouseY, button);
        }

        if (clickedButton) {
            return clickedButton;
        } else {
            managementScreen.selectPoi(poi);
            return true;
        }
    }

    private void renderIcon(PoseStack poseStack) {
        float[] poiColor = CustomColor.fromInt(poi.getColor().asInt()).asFloatArray();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(poiColor[0], poiColor[1], poiColor[2], 1);

        RenderUtils.drawTexturedRect(
                poseStack,
                poi.getIcon(),
                getX() + dividedWidth - (poi.getIcon().width() / 2f),
                getY() + 10 - (poi.getIcon().height() / 2f));

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
