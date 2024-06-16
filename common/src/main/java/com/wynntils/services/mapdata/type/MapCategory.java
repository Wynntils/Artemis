/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.type;

import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import java.util.Optional;

public interface MapCategory extends MapDataProvidedType {
    String getCategoryId();

    Optional<String> getName();

    Optional<MapAttributes> getAttributes();
}
