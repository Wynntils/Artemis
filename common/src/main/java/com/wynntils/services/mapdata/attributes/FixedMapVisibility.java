/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.services.mapdata.attributes;

import com.wynntils.services.mapdata.attributes.type.MapVisibility;
import java.util.Optional;

public final class FixedMapVisibility implements MapVisibility {
    public static final MapVisibility ICON_ALWAYS = new FixedMapVisibility(0f, 100f, 6f);
    public static final MapVisibility ICON_NEVER = new FixedMapVisibility(100f, 0f, 6f);

    public static final MapVisibility LABEL_ALWAYS = new FixedMapVisibility(0f, 100f, 3f);
    public static final MapVisibility LABEL_NEVER = new FixedMapVisibility(100f, 0f, 3f);

    private final Float min;
    private final Float max;
    private final Float fade;

    private FixedMapVisibility(Float min, Float max, Float fade) {
        this.min = min;
        this.max = max;
        this.fade = fade;
    }

    @Override
    public Optional<Float> getMin() {
        return Optional.of(min);
    }

    @Override
    public Optional<Float> getMax() {
        return Optional.of(max);
    }

    @Override
    public Optional<Float> getFade() {
        return Optional.of(fade);
    }
}
