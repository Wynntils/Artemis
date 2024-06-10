/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes.type;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.type.TextShadow;

public record ResolvedMapAttributes(
        String label,
        String iconId,
        int priority,
        int level,
        ResolvedMapVisibility labelVisibility,
        CustomColor labelColor,
        TextShadow labelShadow,
        ResolvedMapVisibility iconVisibility,
        CustomColor iconColor,
        MapDecoration iconDecoration,
        ResolvedMarkerOptions markerOptions) {}
