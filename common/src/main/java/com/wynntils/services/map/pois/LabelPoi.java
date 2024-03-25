/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.map.pois;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.text.StyledText;
import com.wynntils.services.map.Label;
import com.wynntils.services.map.type.DisplayPriority;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import net.minecraft.client.renderer.MultiBufferSource;

public class LabelPoi implements Poi {
    private static final CustomColor AQUA = new CustomColor(0f, 0.8f, 0.8f);
    private static final CustomColor YELLOW = new CustomColor(1f, 1f, 0f);
    private static final CustomColor WHITE = new CustomColor(1f, 1f, 1f);

    private final PoiLocation location;
    private final Label label;

    public LabelPoi(Label label) {
        location = new PoiLocation(label.getX(), null, label.getZ());
        this.label = label;
    }

    @Override
    public boolean hasStaticLocation() {
        return true;
    }

    @Override
    public PoiLocation getLocation() {
        return location;
    }

    @Override
    public int getWidth(float mapZoom, float scale) {
        return (int) (FontRenderer.getInstance().getFont().width(label.getName()) * scale);
    }

    @Override
    public int getHeight(float mapZoom, float scale) {
        return (int) (FontRenderer.getInstance().getFont().lineHeight * scale);
    }

    @Override
    public DisplayPriority getDisplayPriority() {
        return DisplayPriority.HIGH;
    }

    public float getAlphaFromScale(float zoom) {
        float alpha;
        if (zoom >= 1) {
            // Fade out when zoomed in
            alpha = switch (label.getLayer()) {
                case PROVINCE -> 0; // Never visible at high zoom
                case CITY -> MathUtils.map(zoom, 1.0f, 1.3f, 1f, 0f);
                case TOWN_OR_PLACE -> MathUtils.map(zoom, 1.5f, 2.3f, 1f, 0f);};
        } else {
            // Fade out/in when zoomed out
            alpha = switch (label.getLayer()) {
                case PROVINCE -> MathUtils.map(zoom, 0.2f, 0.25f, 1f, 0f);
                case CITY -> 1; // always visible at low zoom
                case TOWN_OR_PLACE -> MathUtils.map(zoom, 0.2f, 0.25f, 0f, 1f);};
        }

        return MathUtils.clamp(alpha, 0f, 1f) * 0.9f;
    }

    private TextShadow getTextShadow() {
        return TextShadow.OUTLINE;
    }

    private CustomColor getRenderedColor(float alpha) {
        CustomColor color =
                switch (label.getLayer()) {
                    case PROVINCE -> AQUA;
                    case CITY -> YELLOW;
                    case TOWN_OR_PLACE -> WHITE;
                };

        return color.withAlpha(alpha);
    }

    @Override
    public void renderAt(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float renderX,
            float renderY,
            boolean hovered,
            float scale,
            float mapZoom) {
        float alpha = getAlphaFromScale(mapZoom);
        if (alpha < 0.01) {
            return; // small enough alphas are turned into 255
        }
        float modifier = scale;
        if (hovered) {
            modifier *= 1.05;
            alpha = 1f;
        }
        CustomColor color = getRenderedColor(alpha);

        poseStack.pushPose();
        poseStack.translate(renderX, renderY, getDisplayPriority().ordinal());
        poseStack.scale(modifier, modifier, modifier);

        BufferedFontRenderer.getInstance()
                .renderText(
                        poseStack,
                        bufferSource,
                        StyledText.fromString(label.getName()),
                        0,
                        0,
                        color,
                        HorizontalAlignment.CENTER,
                        VerticalAlignment.MIDDLE,
                        getTextShadow(),
                        1f);
        if (hovered) {
            String level = label.getLevelString();
            if (!level.isEmpty()) {
                BufferedFontRenderer.getInstance()
                        .renderText(
                                poseStack,
                                bufferSource,
                                StyledText.fromString("[Lv " + level + "]"),
                                0,
                                10,
                                color,
                                HorizontalAlignment.CENTER,
                                VerticalAlignment.MIDDLE,
                                getTextShadow(),
                                1f);
            }
        }
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
