/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes.impl;

import com.wynntils.services.mapdata.attributes.type.MapVisibility;

public class AlwaysMapVisibility implements MapVisibility {
    @Override
    public float getVisibility(int zoomStep) {
        return 1f;
    }
}
