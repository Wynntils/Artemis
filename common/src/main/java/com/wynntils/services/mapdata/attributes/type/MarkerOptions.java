/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes.type;

import com.wynntils.services.mapdata.attributes.MarkerOptionsBuilder;
import com.wynntils.utils.colors.CustomColor;
import java.util.Optional;

public interface MarkerOptions {
    MarkerOptions NONE = new MarkerOptions() {
        @Override
        public Optional<Float> getInnerRadius() {
            return Optional.of(0f);
        }

        @Override
        public Optional<Float> getOuterRadius() {
            return Optional.of(0f);
        }

        @Override
        public Optional<Float> getFade() {
            return Optional.of(0f);
        }

        @Override
        public Optional<CustomColor> getBeaconColor() {
            return Optional.of(CustomColor.NONE);
        }

        @Override
        public Optional<Boolean> renderLabel() {
            return Optional.of(false);
        }

        @Override
        public Optional<Boolean> renderDistance() {
            return Optional.of(false);
        }

        @Override
        public Optional<Boolean> renderIcon() {
            return Optional.of(false);
        }
    };

    // The marker starts to fade in when the player is within the outer radius (- fade),
    // and starts to fade out when approaching the inner radius (- fade)

    // The inner radius for the marker visibility circle
    // If inside the inner radius, the marker starts to fade out
    Optional<Float> getInnerRadius();

    // The outer radius for the marker visibility circle
    // If outside the outer radius, the marker is invisible
    Optional<Float> getOuterRadius();

    // The fade distance for the marker visibility circles
    Optional<Float> getFade();

    // The color of the beacon beam
    // If empty, or alpha is 0, no beacon beam will be rendered
    Optional<CustomColor> getBeaconColor();

    // Whether to render the label
    Optional<Boolean> renderLabel();

    // Whether to render the distance (below the label)
    Optional<Boolean> renderDistance();

    // Whether to render the icon
    Optional<Boolean> renderIcon();

    static MarkerOptionsBuilder builder() {
        return new MarkerOptionsBuilder();
    }
}
