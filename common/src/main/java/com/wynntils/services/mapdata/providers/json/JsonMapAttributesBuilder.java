/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.json;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.type.TextShadow;

public class JsonMapAttributesBuilder {
    private String label;
    private String icon;
    private int priority;
    private int level;
    private CustomColor labelColor;
    private TextShadow labelShadow;
    private JsonMapVisibility labelVisibility;
    private CustomColor iconColor;
    private JsonMapVisibility iconVisibility;

    public JsonMapAttributesBuilder setLabel(String label) {
        this.label = label;
        return this;
    }

    public JsonMapAttributesBuilder setIcon(String icon) {
        this.icon = icon;
        return this;
    }

    public JsonMapAttributesBuilder setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public JsonMapAttributesBuilder setLevel(int level) {
        this.level = level;
        return this;
    }

    public JsonMapAttributesBuilder setLabelColor(CustomColor labelColor) {
        this.labelColor = labelColor;
        return this;
    }

    public JsonMapAttributesBuilder setLabelShadow(TextShadow labelShadow) {
        this.labelShadow = labelShadow;
        return this;
    }

    public JsonMapAttributesBuilder setLabelVisibility(JsonMapVisibility labelVisibility) {
        this.labelVisibility = labelVisibility;
        return this;
    }

    public JsonMapAttributesBuilder setIconColor(CustomColor iconColor) {
        this.iconColor = iconColor;
        return this;
    }

    public JsonMapAttributesBuilder setIconVisibility(JsonMapVisibility iconVisibility) {
        this.iconVisibility = iconVisibility;
        return this;
    }

    public JsonMapAttributes build() {
        return new JsonMapAttributes(
                label, icon, priority, level, labelColor, labelShadow, labelVisibility, iconColor, iconVisibility);
    }
}
