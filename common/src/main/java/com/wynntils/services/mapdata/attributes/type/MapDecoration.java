/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes.type;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

// Allow dynamic map features to arbitrarily extend the rendering
public interface MapDecoration {
    MapDecoration NONE = new MapDecoration() {
        @Override
        public boolean isVisible() {
            return false;
        }

        @Override
        public void render(
                PoseStack poseStack,
                MultiBufferSource bufferSource,
                boolean hovered,
                boolean fullscreenMap,
                float zoomLevel) {}
    };

    boolean isVisible();

    void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            boolean hovered,
            boolean fullscreenMap,
            float zoomLevel);
}
