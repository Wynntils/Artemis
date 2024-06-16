/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.type;

import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.type.BoundingShape;
import java.util.List;

public interface MapArea extends MapFeature {
    // The area is described by a polygon. This list is the sequence of
    // vertices of that polygon, ordered in a counterclockwise orientation.
    // The last segment of the polygon connects from the last vertice to the first.
    List<Location> getPolygonArea();

    @Override
    default boolean isVisible(BoundingShape boundingShape) {
        return false;
    }
}
