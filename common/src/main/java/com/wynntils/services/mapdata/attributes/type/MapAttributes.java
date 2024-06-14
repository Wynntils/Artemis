/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes.type;

import java.util.Optional;

/**
 * Defines the possible attributes that can be defined. These can be contributed to
 * either directly in a map feature, or by any level of the category hierarchy.
 * To contribute a value, implementor should return a non-empty Optional value.
 * If the value is empty, the search will continue for another implementation,
 * i.e. the value is inherited.
 */
public interface MapAttributes {
    // If this is the empty string (""), then no label will be displayed
    Optional<String> getLabel();

    // 1-1000, 1000 is the highest priority (drawn on top of everything else)
    Optional<Integer> getPriority();

    // The minimum combat level for which this feature is suitable for
    // 0 means no information is available, or level is not applicable
    // 1 means suitable for all levels
    Optional<Integer> getLevel();
}
