/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes.type;

/**
 * Resolved attributes for a map feature. This class is used to cache the resolved attributes
 * for a map feature, so that they do not need to be recalculated every time they are requested.
 */
public interface ResolvedMapAttributes {
    String label();

    int priority();

    int level();
}
