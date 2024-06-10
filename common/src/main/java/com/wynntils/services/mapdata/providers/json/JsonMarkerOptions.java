/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.json;

import com.wynntils.services.mapdata.attributes.type.MarkerOptions;
import com.wynntils.utils.colors.CustomColor;
import java.util.Optional;

public class JsonMarkerOptions implements MarkerOptions {
    private final Float innerRadius;
    private final Float outerRadius;
    private final Float fade;
    private final CustomColor beaconColor;
    private final Boolean renderLabel;
    private final Boolean renderDistance;
    private final Boolean renderIcon;

    public JsonMarkerOptions(
            Float innerRadius,
            Float outerRadius,
            Float fade,
            CustomColor beaconColor,
            Boolean renderLabel,
            Boolean renderDistance,
            Boolean renderIcon) {
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.fade = fade;
        this.beaconColor = beaconColor;
        this.renderLabel = renderLabel;
        this.renderDistance = renderDistance;
        this.renderIcon = renderIcon;
    }

    @Override
    public Optional<Float> getInnerRadius() {
        return Optional.ofNullable(innerRadius);
    }

    @Override
    public Optional<Float> getOuterRadius() {
        return Optional.ofNullable(outerRadius);
    }

    @Override
    public Optional<Float> getFade() {
        return Optional.ofNullable(fade);
    }

    @Override
    public Optional<CustomColor> getBeaconColor() {
        return Optional.ofNullable(beaconColor);
    }

    @Override
    public Optional<Boolean> renderLabel() {
        return Optional.ofNullable(renderLabel);
    }

    @Override
    public Optional<Boolean> renderDistance() {
        return Optional.ofNullable(renderDistance);
    }

    @Override
    public Optional<Boolean> renderIcon() {
        return Optional.ofNullable(renderIcon);
    }
}
