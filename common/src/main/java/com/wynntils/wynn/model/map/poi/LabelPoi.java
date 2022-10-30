/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.map.poi;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.gui.render.FontRenderer;
import com.wynntils.gui.render.HorizontalAlignment;
import com.wynntils.gui.render.VerticalAlignment;
import com.wynntils.mc.objects.CustomColor;
import com.wynntils.utils.MathUtils;

public class LabelPoi implements Poi {
    MapLocation location;

    private static final int LABEL_Y = 64;

    private final Label label;

    public LabelPoi(Label label) {
        location = new MapLocation(label.getX(), LABEL_Y, label.getZ());
        this.label = label;
    }

    @Override
    public MapLocation getLocation() {
        return location;
    }

    @Override
    public int getWidth() {
        return FontRenderer.getInstance().getFont().width(label.getName());
    }

    @Override
    public int getHeight() {
        return FontRenderer.getInstance().getFont().lineHeight;
    }

    private float getAlphaFromScale(float zoom) {
        float alpha =
                switch (label.getLayer()) {
                    case PROVINCE -> MathUtils.map(zoom, 0.2f, 3f, 2f, -5f);
                    case CITY -> MathUtils.map(zoom, 0.2f, 3f, 0f, 5f);
                    case TOWN_OR_PLACE -> MathUtils.map(zoom, 0.2f, 3f, -0.5f, 6f);
                };

        return MathUtils.clamp(alpha, 0f, 1f);
    }

    private FontRenderer.TextShadow getTextShadow() {
        if (label.getLayer() == Label.LabelLayer.PROVINCE) {
            return FontRenderer.TextShadow.OUTLINE;
        }

        return FontRenderer.TextShadow.NORMAL;
    }

    private static final CustomColor GOLD = new CustomColor(1f, 0.6f, 0f);
    private static final CustomColor YELLOW = new CustomColor(1f, 1f, 0.3f);
    private static final CustomColor WHITE = new CustomColor(1f, 1f, 1f);

    private CustomColor getRenderedColor(float zoom, boolean hovered) {
        float alpha = getAlphaFromScale(zoom);
        CustomColor color =
                switch (label.getLayer()) {
                    case PROVINCE -> GOLD;
                    case CITY -> YELLOW;
                    case TOWN_OR_PLACE -> WHITE;
                };

        return color.withAlpha(alpha * (hovered ? 1f : 0.75f));
    }

    @Override
    public void renderAt(
            PoseStack poseStack, float renderX, float renderZ, boolean hovered, float scale, float mapZoom) {
        // TODO hovered behavior?
        // TODO reimplement minscaleforlabel through fading instead

        float modifier = scale;

        if (hovered) {
            modifier *= 1.05;
        }

        CustomColor color = getRenderedColor(mapZoom, hovered);

        if (color.a < 4) {
            return; // small enough alphas are turned into 255
        }

        poseStack.pushPose();
        poseStack.translate(renderX, renderZ, 0);
        poseStack.scale(modifier, modifier, modifier);

        FontRenderer.getInstance()
                .renderText(
                        poseStack,
                        label.getName(),
                        0,
                        0,
                        0,
                        color,
                        HorizontalAlignment.Center,
                        VerticalAlignment.Middle,
                        getTextShadow());
        poseStack.popPose();
    }

    @Override
    public String getName() {
        return label.getName();
    }

    public Label getLabel() {
        return label;
    }
}
