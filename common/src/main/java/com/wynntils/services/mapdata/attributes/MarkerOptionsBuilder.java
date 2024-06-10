/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes;

import com.wynntils.services.mapdata.attributes.type.MarkerOptions;
import com.wynntils.utils.colors.CustomColor;
import java.util.Optional;

public class MarkerOptionsBuilder implements MarkerOptions {
    private Float innerRadius = null;
    private Float outerRadius = null;
    private Float fade = null;
    private CustomColor beaconColor = null;
    private Boolean renderLabel = null;
    private Boolean renderDistance = null;
    private Boolean renderIcon = null;

    public MarkerOptionsBuilder withInnerRadius(float innerRadius) {
        this.innerRadius = innerRadius;
        return this;
    }

    public MarkerOptionsBuilder withOuterRadius(float outerRadius) {
        this.outerRadius = outerRadius;
        return this;
    }

    public MarkerOptionsBuilder withFade(float fade) {
        this.fade = fade;
        return this;
    }

    public MarkerOptionsBuilder withBeaconColor(CustomColor beaconColor) {
        this.beaconColor = beaconColor;
        return this;
    }

    public MarkerOptionsBuilder withRenderLabel(boolean renderLabel) {
        this.renderLabel = renderLabel;
        return this;
    }

    public MarkerOptionsBuilder withRenderDistance(boolean renderDistance) {
        this.renderDistance = renderDistance;
        return this;
    }

    public MarkerOptionsBuilder withRenderIcon(boolean renderIcon) {
        this.renderIcon = renderIcon;
        return this;
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
