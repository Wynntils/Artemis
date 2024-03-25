/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes.type;

@FunctionalInterface
public interface MapVisibility {
    /**
     * This method is used to determine the visibility of a feature at a given zoom level.
     * @param zoomStep The current zoom step of the map. You can read more about zoom steps in the {@link com.wynntils.utils.render.MapRenderer} class.
     * @return A float between 0 and 1, where 0 means the feature is not visible and 1 means the feature is fully visible. Values in between are used to determine the transparency of the feature.
     */
    float getVisibility(int zoomStep);
}
