/*
 * Copyright © Wynntils 2023-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes;

import com.wynntils.services.mapdata.attributes.type.MapAttributes;
import com.wynntils.services.mapdata.attributes.type.MapDecoration;
import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import com.wynntils.services.mapdata.attributes.type.MarkerOptions;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.type.TextShadow;
import java.util.Optional;

public abstract class AbstractMapAttributes implements MapAttributes {
    @Override
    public Optional<String> getLabel() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getIconId() {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getPriority() {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getLevel() {
        return Optional.empty();
    }

    @Override
    public Optional<MapVisibility> getLabelVisibility() {
        return Optional.empty();
    }

    @Override
    public Optional<CustomColor> getLabelColor() {
        return Optional.empty();
    }

    @Override
    public Optional<TextShadow> getLabelShadow() {
        return Optional.empty();
    }

    @Override
    public Optional<MapVisibility> getIconVisibility() {
        return Optional.empty();
    }

    @Override
    public Optional<CustomColor> getIconColor() {
        return Optional.empty();
    }

    @Override
    public Optional<MapDecoration> getIconDecoration() {
        return Optional.empty();
    }

    @Override
    public Optional<MarkerOptions> getMarkerOptions() {
        return Optional.empty();
    }
}
