/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes.type;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.type.RangedValue;

public interface MapAttributes {
    // If this is the empty string (""), then no label will be displayed
    // null means inherit
    String getLabel();

    // If this is MapFeatureIcon.NO_ICON_ID ("none"), then no icon will be displayed
    // null means inherit
    String getIconId();

    // 1-1000, 1000 is highest priority (drawn on top of everything else)
    // 0 means no value specified; inherit
    int getPriority();

    // the range of combat levels for which this feature is suitable
    // If one of the bounds of the range is undefined, Integer.MIN_VALUE or Integer.MAX_VALUE will be used
    // (eg. Corkus has a minimum level of 80, but no maximum level)
    // NONE (0-0) or a range of (Integer.MIN_VALUE-Integer.MAX_VALUE) means suitable for all combat levels
    // null means no information is available
    RangedValue getLevelRange();

    MapVisibility getLabelVisibility();

    CustomColor getLabelColor();

    TextShadow getLabelShadow();

    MapVisibility getIconVisibility();

    CustomColor getIconColor();

    MapDecoration getIconDecoration();
}
