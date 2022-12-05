/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.map.poi;

import com.mojang.blaze3d.vertex.PoseStack;

public interface Poi {

    PoiLocation getLocation();

    /**
     * Render priority is used to determine the order in which POIs are rendered.
     * A higher render priority means, that the POI is rendered later, so it will be on top of other POIs.
     */
    RenderPriority getRenderPriority();

    boolean hasStaticLocation();

    void renderAt(PoseStack poseStack, float renderX, float renderZ, boolean hovered, float scale, float mapZoom);

    int getWidth(float mapZoom, float scale);

    int getHeight(float mapZoom, float scale);

    String getName();
}
