/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes.type;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.type.TextShadow;
import java.util.Optional;

public interface MapLocationAttributes extends MapAttributes {
    // If this is MapFeatureIcon.NO_ICON_ID ("none"), then no icon will be displayed
    Optional<String> getIconId();

    Optional<MapVisibility> getLabelVisibility();

    Optional<CustomColor> getLabelColor();

    Optional<TextShadow> getLabelShadow();

    Optional<MapVisibility> getIconVisibility();

    Optional<CustomColor> getIconColor();

    Optional<MapDecoration> getIconDecoration();
}
