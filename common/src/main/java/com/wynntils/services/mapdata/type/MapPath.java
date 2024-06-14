/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.type;

import com.wynntils.services.mapdata.attributes.type.MapPathAttributes;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.type.BoundingShape;
import java.util.List;
import java.util.Optional;

public interface MapPath extends MapFeature {
    // The path is described by a sequence of locations
    List<Location> getPath();

    Optional<MapPathAttributes> getAttributes();

    @Override
    default boolean isVisible(BoundingShape boundingShape) {
        return false;
    }
}
