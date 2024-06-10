/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.json;

import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapDecoration;
import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import com.wynntils.services.mapdata.attributes.type.MarkerOptions;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.type.TextShadow;
import java.util.Optional;

public class JsonMapAttributes implements MapAttributes {
    private final String label;
    private final String icon;
    private final int priority;
    private final int level;
    private final CustomColor labelColor;
    private final TextShadow labelShadow;
    private final JsonMapVisibility labelVisibility;
    private final CustomColor iconColor;
    private final JsonMapVisibility iconVisibility;
    private final JsonMarkerOptions markerOptions;

    public JsonMapAttributes(
            String label,
            String icon,
            int priority,
            int level,
            CustomColor labelColor,
            TextShadow labelShadow,
            JsonMapVisibility labelVisibility,
            CustomColor iconColor,
            JsonMapVisibility iconVisibility,
            JsonMarkerOptions markerOptions) {
        this.label = label;
        this.icon = icon;
        this.priority = priority;
        this.level = level;
        this.labelColor = labelColor;
        this.labelShadow = labelShadow;
        this.labelVisibility = labelVisibility;
        this.iconColor = iconColor;
        this.iconVisibility = iconVisibility;
        this.markerOptions = markerOptions;
    }

    @Override
    public Optional<String> getLabel() {
        return Optional.ofNullable(label);
    }

    @Override
    public Optional<String> getIconId() {
        return Optional.ofNullable(icon);
    }

    @Override
    public Optional<Integer> getPriority() {
        return Optional.of(priority);
    }

    @Override
    public Optional<Integer> getLevel() {
        return Optional.of(level);
    }

    @Override
    public Optional<MapVisibility> getLabelVisibility() {
        return Optional.ofNullable(labelVisibility);
    }

    @Override
    public Optional<CustomColor> getLabelColor() {
        return Optional.ofNullable(labelColor);
    }

    @Override
    public Optional<TextShadow> getLabelShadow() {
        return Optional.ofNullable(labelShadow);
    }

    @Override
    public Optional<MapVisibility> getIconVisibility() {
        return Optional.ofNullable(iconVisibility);
    }

    @Override
    public Optional<CustomColor> getIconColor() {
        return Optional.ofNullable(iconColor);
    }

    @Override
    public Optional<MarkerOptions> getMarkerOptions() {
        return Optional.ofNullable(markerOptions);
    }

    @Override
    public Optional<MapDecoration> getIconDecoration() {
        // json files can never provide icon decorations; those are only for dynamic features
        return Optional.empty();
    }
}
