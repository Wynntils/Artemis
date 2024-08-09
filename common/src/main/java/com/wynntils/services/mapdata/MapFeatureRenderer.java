/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Services;
import com.wynntils.core.text.StyledText;
import com.wynntils.services.mapdata.attributes.type.MapDecoration;
import com.wynntils.services.mapdata.attributes.type.MapIcon;
import com.wynntils.services.mapdata.attributes.type.ResolvedMapAttributes;
import com.wynntils.services.mapdata.type.MapFeature;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.buffered.BufferedFontRenderer;
import com.wynntils.utils.render.buffered.BufferedRenderUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;

public final class MapFeatureRenderer {
    private static final int SPACING = 2;
    private static final float TEXT_SCALE = 1f;

    public static void renderMapFeature(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            MapFeature feature,
            ResolvedMapAttributes attributes,
            float renderX,
            float renderY,
            boolean hovered,
            float scale,
            float zoomLevel,
            boolean showLabels) {
        float renderScale = hovered ? scale * 1.05f : scale;
        float hoverAlphaFactor = hovered ? 1f : 0.9f;
        int labelHeight = (int) (FontRenderer.getInstance().getFont().lineHeight * renderScale * TEXT_SCALE);

        int yOffset = 0;

        poseStack.pushPose();
        // z-index for rendering
        poseStack.translate(renderX, renderY, attributes.priority());
        poseStack.scale(renderScale, renderScale, renderScale);

        // Draw icon, if applicable
        float iconAlpha =
                hoverAlphaFactor * Services.MapData.calculateVisibility(attributes.iconVisibility(), zoomLevel);
        Optional<MapIcon> icon = Services.MapData.getIcon(attributes.iconId());
        boolean drawIcon = iconAlpha > 0.01;
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
        float labelAlpha =
                hoverAlphaFactor * Services.MapData.calculateVisibility(attributes.labelVisibility(), zoomLevel);
        // Small enough alphas are turned into 255, so trying to render them results in
        // visual glitches
        // Always draw labels for hovered icons, regardless of label visibility rules
        boolean drawLabel = labelAlpha > 0.01 || (drawIcon && hovered);
        if (!attributes.label().isEmpty() && drawLabel && showLabels) {
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
            decoration.render(poseStack, bufferSource, hovered, zoomLevel);
        }

        poseStack.popPose();
    }
}
