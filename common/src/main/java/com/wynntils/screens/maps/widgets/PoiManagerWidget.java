/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.screens.maps.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.features.map.MapFeature;
import com.wynntils.models.map.pois.CustomPoi;
import com.wynntils.screens.maps.PoiCreationScreen;
import com.wynntils.screens.maps.PoiManagementScreen;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.mc.RenderedStringUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

public class PoiManagerWidget extends AbstractWidget {
    private CustomPoi poi;
    private Button editButton;
    private Button deleteButton;
    private Button upButton;
    private Button downButton;
    private int row;
    private CustomColor color;
    private int spacingMultiplier = 20;
    private static final int ungroupedIndex = Managers.Feature.getFeatureInstance(MapFeature.class)
            .customPois
            .get().size();
    private int group = ungroupedIndex;
    private boolean decreasedSize = (spacingMultiplier == 14);
    private PoiManagementScreen managementScreen;
    private List<CustomPoi> pois;

    public PoiManagerWidget(
            float x, float y, int width, int height, CustomPoi poi, int row, PoiManagementScreen managementScreen) {
        super((int) x, (int) y, width, height, Component.literal(poi.getName()));
        this.poi = poi;
        this.row = row;
        this.managementScreen = managementScreen;

        pois = Managers.Feature.getFeatureInstance(MapFeature.class).customPois.get();

        color = CustomColor.fromInt(0xFFFFFF);

        if (poi.getVisibility() == CustomPoi.Visibility.HIDDEN) {
            color = CustomColor.fromInt(0x636363);
        }

        int groupShift = group == ungroupedIndex ? 20 : 0;

        this.editButton = new Button.Builder(
                Component.translatable("screens.wynntils.poiManagementGui.edit"),
                (button) -> McUtils.mc().setScreen(PoiCreationScreen.create(managementScreen, poi)))
                .pos(this.width/2 + 85 + groupShift, 54 + spacingMultiplier * row)
                .size((int) Math.round(40.0 * (decreasedSize ? 0.7 : 1.0)), (int) Math.round(20.0 * (decreasedSize ? 0.6 : 1.0)))
                .build();

        this.deleteButton = new Button.Builder(
                Component.translatable("screens.wynntils.poiManagementGui.delete"),
                (button) -> {
                    Managers.Feature.getFeatureInstance(MapFeature.class)
                            .customPois
                            .get()
                            .remove(poi);
                    Managers.Config.saveConfig();
                    managementScreen.populatePois();
                })
                .pos(this.width/2 + 130 + groupShift, 54 + spacingMultiplier * row)
                .size((int) Math.round(40.0 * (decreasedSize ? 0.9 : 1.0)), (int) Math.round(20.0 * (decreasedSize ? 0.6 : 1.0)))
                .build();

        this.upButton = new Button.Builder(
                Component.literal("\u2303"),
                (button) -> {
                    updateIndex(-1);
                })
                .pos(this.width/2 + 172 + groupShift, 54 + spacingMultiplier * row)
                .size((int)Math.round(9 * (decreasedSize ? 0.75 : 1.0)), (int)Math.round(9 * (decreasedSize ? 0.75 : 1.0)))
                .build();

        this.downButton = new Button.Builder(
                Component.literal("\u2304"),
                (button) -> {
                    updateIndex(1);
                })
                .pos(this.width/2 + 172 + groupShift, 54 + spacingMultiplier * row + (int)Math.round(9 * (decreasedSize ? 0.75 : 1.0)))
                .size((int)Math.round(9 * (decreasedSize ? 0.75 : 1.0)), (int)Math.round(9 * (decreasedSize ? 0.75 : 1.0)))
                .build();

        if (pois.indexOf(poi) == 0) {
            upButton.active = false;
        } else if (pois.indexOf(poi) == (pois.size() - 1)) {
            downButton.active = false;
        }
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        MultiBufferSource.BufferSource bufferSource =
                McUtils.mc().renderBuffers().bufferSource();

        float centreZ = 64 + spacingMultiplier * row;

        poi.renderAt(poseStack, bufferSource, this.width / 2f - 151, centreZ, false, 1f, 1f);

        int maxTextWidth = 90;
        String poiName = RenderedStringUtils.getMaxFittingText(poi.getName(), maxTextWidth, McUtils.mc().font);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        poiName,
                        this.width/2f - 130,
                        60 + spacingMultiplier * row,
                        color,
                        HorizontalAlignment.Left,
                        VerticalAlignment.Top,
                        TextShadow.NORMAL);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        String.valueOf(poi.getLocation().getX()),
                        this.width/2f - 15,
                        60 + spacingMultiplier * row,
                        color,
                        HorizontalAlignment.Center,
                        VerticalAlignment.Top,
                        TextShadow.NORMAL);

        Optional<Integer> y = poi.getLocation().getY();

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        y.isPresent() ? String.valueOf(y.get()) : "",
                        this.width/2f + 40,
                        60 + spacingMultiplier * row,
                        color,
                        HorizontalAlignment.Center,
                        VerticalAlignment.Top,
                        TextShadow.NORMAL);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        String.valueOf(poi.getLocation().getZ()),
                        this.width/2f + 80,
                        60 + spacingMultiplier * row,
                        color,
                        HorizontalAlignment.Center,
                        VerticalAlignment.Top,
                        TextShadow.NORMAL);

        editButton.render(poseStack, mouseX, mouseY, partialTick);
        deleteButton.render(poseStack, mouseX, mouseY, partialTick);
        upButton.render(poseStack, mouseX, mouseY, partialTick);
        downButton.render(poseStack, mouseX, mouseY, partialTick);
    }

    private void updateIndex(int direction) {
        int indexToSet = pois.indexOf(poi) + direction;
        pois.remove(poi);
        pois.add(indexToSet, poi);
        managementScreen.populatePois();
        Managers.Config.saveConfig();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return editButton.mouseClicked(mouseX, mouseY, button)
                || deleteButton.mouseClicked(mouseX, mouseY, button)
                || upButton.mouseClicked(mouseX, mouseY, button)
                || downButton.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}
}
