/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.marker.type;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.type.Location;
import com.wynntils.utils.render.Texture;

public record MarkerInfo(
        LocationSupplier locationSupplier, Texture texture, CustomColor beaconColor, CustomColor textureColor) {
    public Location location() {
        return locationSupplier.getLocation();
    }
}
