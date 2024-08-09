/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.segments;

import com.wynntils.utils.type.CappedValue;

public class HealthTextSegment extends AbstractTextSegment {
    private final CappedValue health;

    public HealthTextSegment(String segmentText, CappedValue health) {
        super(segmentText);
        this.health = health;
    }

    public CappedValue getHealth() {
        return health;
    }

    @Override
    public String toString() {
        return "HealthTextSegment{" + "health=" + health + ", segmentText='" + segmentText + '\'' + '}';
    }
}
