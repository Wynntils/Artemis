/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.providers.json;

import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import java.util.Optional;

public final class JsonMapVisibility implements MapVisibility {
    private final Float min;
    private final Float max;
    private final Float fade;

    public JsonMapVisibility(Float min, Float max, Float fade) {
        this.min = min;
        this.max = max;
        this.fade = fade;
    }

    public JsonMapVisibility(MapVisibility visibility) {
        this(
                visibility.getMin().orElse(null),
                visibility.getMax().orElse(null),
                visibility.getFade().orElse(null));
    }

    @Override
    public Optional<Float> getMin() {
        return Optional.ofNullable(min);
    }

    @Override
    public Optional<Float> getMax() {
        return Optional.ofNullable(max);
    }

    @Override
    public Optional<Float> getFade() {
        return Optional.ofNullable(fade);
    }
}
