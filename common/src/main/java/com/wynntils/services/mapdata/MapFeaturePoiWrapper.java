/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.services.map.pois.Poi;
import com.wynntils.services.map.type.DisplayPriority;
import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.services.mapdata.type.MapFeature;
import com.wynntils.services.mapdata.type.MapLocation;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.type.PoiLocation;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;

public class MapFeaturePoiWrapper implements Poi {
    public static final String FALLBACK_ICON_ID = "wynntils:icon:symbols:waypoint";
    public static final int SPACING = 2;
    public static final float TEXT_SCALE = 1f;
    private final MapFeature feature;
    private final MapAttributes attributes;

    public MapFeaturePoiWrapper(MapFeature feature) {
        this.feature = feature;
        attributes = Services.MapData.getFullFeatureAttributes(this.feature);
    }

    @Override
    public PoiLocation getLocation() {
        if (feature instanceof MapLocation mapLocation) {
            return PoiLocation.fromLocation(mapLocation.getLocation());
        }

        return null;
    }

    @Override
    public DisplayPriority getDisplayPriority() {
        return DisplayPriority.HIGHEST;
    }

    @Override
    public boolean hasStaticLocation() {
        return false;
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
        float renderScale = hovered ? scale * 1.05f : scale;
        // this is the default alpha for labels
        float alpha = hovered ? 1f : 0.9f;
        int labelHeight = getLabelHeight(renderScale);

        String iconId = attributes.getIconId();
        String label = attributes.getLabel();
        int level = attributes.getLevel();

        int yOffset = 0;

        poseStack.pushPose();
        // z-index for rendering
        poseStack.translate(renderX, renderY, getDisplayPriority().ordinal());
        poseStack.scale(renderScale, renderScale, renderScale);

        if (hasIcon(iconId)) {
            MapIcon icon = getIcon(iconId);

            float iconWidth = icon.width();
            float iconHeight = icon.height();

            CustomColor iconColor = attributes.getIconColor();
            if (iconColor == null) {
                iconColor = CommonColors.WHITE;
            }
            CustomColor color = iconColor.withAlpha(alpha);

            BufferedRenderUtils.drawColoredTexturedRect(
                    poseStack,
                    bufferSource,
                    icon.getResourceLocation(),
                    color,
                    this.getIconAlpha(mapZoom),
                    0 - iconWidth / 2,
                    yOffset - iconHeight / 2,
                    0,
                    iconWidth,
                    iconHeight);
            yOffset += (iconHeight + labelHeight) / 2 + SPACING;
        }

        if (hasLabel(label)) {
            CustomColor labelColor = attributes.getLabelColor();
            if (labelColor == null) {
                labelColor = CommonColors.WHITE;
            }
            TextShadow labelShadow = attributes.getLabelShadow();
            if (labelShadow == null) {
                labelShadow = TextShadow.OUTLINE;
            }
            CustomColor color = labelColor.withAlpha(alpha);

            BufferedFontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            bufferSource,
                            StyledText.fromString(label),
                            0,
                            yOffset,
                            color,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            labelShadow,
                            TEXT_SCALE);
            yOffset += labelHeight + SPACING;
        }

        if (hovered && level != 0) {
            CustomColor labelColor = attributes.getLabelColor();
            if (labelColor == null) {
                labelColor = CommonColors.WHITE;
            }
            TextShadow labelShadow = attributes.getLabelShadow();
            if (labelShadow == null) {
                labelShadow = TextShadow.OUTLINE;
            }
            CustomColor color = labelColor.withAlpha(alpha);

            BufferedFontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            bufferSource,
                            StyledText.fromString("[Lv " + level + "]"),
                            0,
                            yOffset,
                            color,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            labelShadow,
                            TEXT_SCALE);
        }

        poseStack.popPose();
    }

    private static MapIcon getIcon(String iconId) {
        Optional<MapIcon> icon = Services.MapData.getIcon(iconId);
        if (icon.isEmpty()) {
            // FIXME: Use a fallback icon for now, since the code is not prepared to handle
            // locations without an icon.
            return Services.MapData.getIcon(FALLBACK_ICON_ID).orElse(null);
        }

        return icon.get();
    }

    private boolean hasIcon(String iconId) {
        return !(iconId == null || iconId.equals(MapIcon.NO_ICON_ID));
    }

    private boolean hasLabel(String label) {
        return label != null && !label.isEmpty();
    }

    private float getIconAlpha(float mapZoom) {
        // FIXME: Depend on icon visibility
        return 1f;
    }

    @Override
    public int getWidth(float mapZoom, float scale) {
        String iconId = attributes.getIconId();
        String label = attributes.getLabel();

        if (!hasIcon(iconId) && hasLabel(label)) {
            // Use label for measurements
            return (int) (FontRenderer.getInstance().getFont().width(attributes.getLabel()) * scale);
        }

        MapIcon icon = getIcon(iconId);
        if (icon == null) return 32;

        return (int) (icon.width() * scale);
    }

    @Override
    public int getHeight(float mapZoom, float scale) {
        String iconId = attributes.getIconId();
        String label = attributes.getLabel();

        if (!hasIcon(iconId) && hasLabel(label)) {
            // Use label for measurements
            return getLabelHeight(scale);
        }

        MapIcon icon = getIcon(iconId);
        if (icon == null) return 32;

        return (int) (icon.height() * scale);
    }

    private int getLabelHeight(float scale) {
        return (int) (FontRenderer.getInstance().getFont().lineHeight * scale * TEXT_SCALE);
    }

    @Override
    public String getName() {
        return "Wrapped MapFeature [" + feature.getFeatureId() + "]";
    }
}
