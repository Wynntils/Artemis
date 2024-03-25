/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.json;

import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapDecoration;
import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.type.RangedValue;

public class JsonAttributes implements MapAttributes {
    private final String label;
    private final String icon;
    private final int priority;
    private final RangedValue level;
    private final CustomColor labelColor;
    private final TextShadow labelShadow;
    private final MapVisibility labelVisibility;
    private final CustomColor iconColor;
    private final MapVisibility iconVisibility;

    public JsonAttributes(
            String label,
            String icon,
            int priority,
            RangedValue level,
            CustomColor labelColor,
            TextShadow labelShadow,
            MapVisibility labelVisibility,
            CustomColor iconColor,
            MapVisibility iconVisibility) {
        this.label = label;
        this.icon = icon;
        this.priority = priority;
        this.level = level;
        this.labelColor = labelColor;
        this.labelShadow = labelShadow;
        this.labelVisibility = labelVisibility;
        this.iconColor = iconColor;
        this.iconVisibility = iconVisibility;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getIconId() {
        return icon;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public RangedValue getLevelRange() {
        return level;
    }

    @Override
    public MapVisibility getLabelVisibility() {
        return labelVisibility;
    }

    @Override
    public CustomColor getLabelColor() {
        return labelColor;
    }

    @Override
    public TextShadow getLabelShadow() {
        return labelShadow;
    }

    @Override
    public MapVisibility getIconVisibility() {
        return iconVisibility;
    }

    @Override
    public CustomColor getIconColor() {
        return iconColor;
    }

    @Override
    public MapDecoration getIconDecoration() {
        // json files can never provide icon decorations; those are only for dynamic features
        return null;
    }
}
