/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.services.mapdata.attributes.type.MapDecoration;
import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.services.mapdata.attributes.type.ResolvedMapAttributes;
import com.wynntils.services.mapdata.type.MapFeature;
import com.wynntils.services.mapdata.type.MapLocation;
import com.wynntils.utils.MathUtils;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Vector2f;
import org.joml.Vector2i;

public class MapFeatureRenderer {
    private static final int SPACING = 2;
    private static final float TEXT_SCALE = 1f;

    // Small/Large enough alphas are turned into 0/255, so trying to render them results in visual glitches
    // Generally <0.1f -> 0f, >0.9f -> 1f
    private static final float MINIMUM_RENDER_ALPHA = 0.1f;

    public static void renderMapFeature(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            MapFeature feature,
            ResolvedMapAttributes attributes,
            Vector2f mapCenter,
            Vector2f screenCenter,
            Vector2f rotationVector,
            float zoomLevel,
            float zoomRenderScale,
            float featureRenderScale,
            boolean hovered,
            boolean fullscreenMap) {
        if (feature instanceof MapLocation location) {
            renderMapLocation(
                    poseStack,
                    bufferSource,
                    location,
                    attributes,
                    mapCenter,
                    screenCenter,
                    rotationVector,
                    zoomLevel,
                    zoomRenderScale,
                    featureRenderScale,
                    hovered,
                    fullscreenMap);
        } else {
            WynntilsMod.warn(
                    "Could not render feature of type " + feature.getClass().getSimpleName() + " with ID "
                            + feature.getCategoryId() + ":" + feature.getFeatureId() + "!");
        }
    }

    public static void renderMapLocation(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            MapLocation location,
            ResolvedMapAttributes attributes,
            Vector2f mapCenter,
            Vector2f screenCenter,
            Vector2f rotationVector,
            float zoomLevel,
            float zoomRenderScale,
            float featureRenderScale,
            boolean hovered,
            boolean fullscreenMap) {
        float renderScale = hovered ? featureRenderScale * 1.05f : featureRenderScale;
        int labelHeight = (int) (FontRenderer.getInstance().getFont().lineHeight * renderScale * TEXT_SCALE);

        int yOffset = 0;

        Location featureLocation = location.getLocation();

        float dX = (featureLocation.x() - mapCenter.x()) * zoomRenderScale;
        float dZ = (featureLocation.z() - mapCenter.y()) * zoomRenderScale;

        float tempdX = dX * rotationVector.x() - dZ * rotationVector.y();
        dZ = dX * rotationVector.y() + dZ * rotationVector.x();
        dX = tempdX;

        float renderX = screenCenter.x() + dX;
        float renderZ = screenCenter.y() + dZ;

        poseStack.pushPose();
        // z-index for rendering
        poseStack.translate(renderX, renderZ, attributes.priority());
        poseStack.scale(renderScale, renderScale, 1);

        // Draw icon, if applicable
        float iconAlpha = Services.MapData.calculateVisibility(attributes.iconVisibility(), zoomLevel);
        Optional<MapIcon> icon = Services.MapData.getIcon(attributes.iconId());
        boolean drawIcon = iconAlpha > MINIMUM_RENDER_ALPHA;
        if (icon.isPresent() && drawIcon) {
            int iconWidth = icon.get().getWidth();
            int iconHeight = icon.get().getHeight();

            if (hovered) {
                // If this is hovered, show with full alpha
                iconAlpha = 1f;
            }

            BufferedRenderUtils.drawColoredTexturedRect(
                    poseStack,
                    bufferSource,
                    icon.get().getResourceLocation(),
                    attributes.iconColor(),
                    iconAlpha,
                    0 - iconWidth / 2f,
                    yOffset - iconHeight / 2f,
                    0,
                    iconWidth,
                    iconHeight);
            yOffset += (iconHeight + labelHeight) / 2 + SPACING;
        }

        // Draw label, if applicable
        float labelAlpha = Services.MapData.calculateVisibility(attributes.labelVisibility(), zoomLevel);
        // Always draw labels for hovered icons, regardless of label visibility rules
        boolean drawLabel = labelAlpha > MINIMUM_RENDER_ALPHA || (drawIcon && hovered);
        if (!attributes.label().isEmpty() && drawLabel && fullscreenMap) {
            if (drawIcon && hovered) {
                // If this is hovered, show with full alpha
                labelAlpha = 1f;
            }

            CustomColor color = attributes.labelColor().withAlpha(labelAlpha);

            BufferedFontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            bufferSource,
                            StyledText.fromString(attributes.label()),
                            0,
                            yOffset,
                            color,
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            attributes.labelShadow(),
                            TEXT_SCALE);
            yOffset += labelHeight + SPACING;
        }

        // Draw level, if applicable
        int level = attributes.level();
        // Show level only for features that are displayed and hovered
        boolean drawLevel = hovered && (drawIcon || drawLabel);
        if (level >= 1 && drawLevel) {
            BufferedFontRenderer.getInstance()
                    .renderText(
                            poseStack,
                            bufferSource,
                            StyledText.fromString("[Lv. " + level + "]"),
                            0,
                            yOffset,
                            attributes.labelColor(),
                            HorizontalAlignment.CENTER,
                            VerticalAlignment.MIDDLE,
                            attributes.labelShadow(),
                            TEXT_SCALE);
        }

        // Draw decoration, if applicable
        MapDecoration decoration = attributes.iconDecoration();
        if (decoration.isVisible()) {
            decoration.render(poseStack, bufferSource, hovered, fullscreenMap, zoomLevel);
        }

        poseStack.popPose();
    }

    public static boolean isHovered(
            MapFeature mapFeature,
            ResolvedMapAttributes attributes,
            Vector2f mapCenter,
            Vector2f screenCenter,
            Vector2f rotationVector,
            Vector2i mousePos,
            float zoomRenderScale,
            float zoomLevel,
            float featureRenderScale) {
        if (mapFeature instanceof MapLocation location) {
            Location featureLocation = location.getLocation();
            float dX = (featureLocation.x() - mapCenter.x()) * zoomRenderScale;
            float dZ = (featureLocation.z() - mapCenter.y()) * zoomRenderScale;

            float tempdX = dX * rotationVector.x() - dZ * rotationVector.y();
            dZ = dX * rotationVector.y() + dZ * rotationVector.x();
            dX = tempdX;

            float renderX = screenCenter.x() + dX;
            float renderZ = screenCenter.y() + dZ;

            int labelHeight = (int) (FontRenderer.getInstance().getFont().lineHeight * featureRenderScale * TEXT_SCALE);

            int yOffset = 0;

            float iconAlpha = Services.MapData.calculateVisibility(attributes.iconVisibility(), zoomLevel);
            Optional<MapIcon> icon = Services.MapData.getIcon(attributes.iconId());
            boolean drawIcon = iconAlpha > MINIMUM_RENDER_ALPHA;
            if (icon.isPresent() && drawIcon) {
                int iconWidth = (int) (icon.get().getWidth() * featureRenderScale);
                int iconHeight = (int) (icon.get().getHeight() * featureRenderScale);

                if (MathUtils.isInside(
                        mousePos.x(),
                        mousePos.y(),
                        (int) renderX - iconWidth / 2,
                        (int) renderX + iconWidth / 2,
                        (int) renderZ - iconHeight / 2,
                        (int) renderZ + iconHeight / 2)) {
                    return true;
                }

                yOffset += (iconHeight + labelHeight) / 2 + SPACING;
            }

            float labelAlpha = Services.MapData.calculateVisibility(attributes.labelVisibility(), zoomLevel);
            boolean drawLabel = labelAlpha > MINIMUM_RENDER_ALPHA;
            if (!attributes.label().isEmpty() && drawLabel) {
                int labelWidth =
                        (int) (FontRenderer.getInstance().getFont().width(attributes.label()) * featureRenderScale);
                return MathUtils.isInside(
                        mousePos.x(),
                        mousePos.y(),
                        (int) renderX - labelWidth / 2,
                        (int) renderX + labelWidth / 2,
                        (int) renderZ + yOffset - labelHeight / 2,
                        (int) renderZ + yOffset + labelHeight / 2);
            }

            // Level and decoration are not considered for hovering

            return false;
        }

        WynntilsMod.warn(
                "Could not check if feature of type " + mapFeature.getClass().getSimpleName() + " with ID "
                        + mapFeature.getCategoryId() + ":" + mapFeature.getFeatureId() + " is hovered!");
        return false;
    }
}
