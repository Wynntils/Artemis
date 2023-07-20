/*
 * Copyright © Wynntils 2023.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.mapdata.type.attributes;

import net.minecraft.resources.ResourceLocation;

public interface MapFeatureIcon {
    String NO_ICON_ID = "none";

    String getIconId();

    ResourceLocation getResourceLocation();

    int width();

    int height();
}
