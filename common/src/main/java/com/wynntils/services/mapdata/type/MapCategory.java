/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.type;

import com.wynntils.services.mapdata.attributes.type.MapAreaAttributes;
import com.wynntils.services.mapdata.attributes.type.MapLocationAttributes;
import com.wynntils.services.mapdata.attributes.type.MapPathAttributes;
import java.util.Optional;

public interface MapCategory extends MapDataProvidedType {
    String getCategoryId();

    Optional<String> getName();

    default Optional<MapLocationAttributes> getLocationAttributes() {
        return Optional.empty();
    }

    default Optional<MapAreaAttributes> getAreaAttributes() {
        return Optional.empty();
    }

    default Optional<MapPathAttributes> getPathAttributes() {
        return Optional.empty();
    }
}
