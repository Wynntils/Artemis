/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.marker.type;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.render.type.AbstractTexture;

public record MarkerInfo(
        String name,
        LocationSupplier locationSupplier,
        AbstractTexture texture,
        CustomColor beaconColor,
        CustomColor textureColor,
        CustomColor textColor) {
    public Location location() {
        return locationSupplier.getLocation();
    }
}
