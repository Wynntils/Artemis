/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.segments;

public class MeterStateAnimationSegment extends AbstractMeterSegment {
    public MeterStateAnimationSegment(String segmentText) {
        super(segmentText);
    }

    @Override
    public String toString() {
        return "MeterStateAnimationSegment{" + "segmentText='" + segmentText + '\'' + '}';
    }
}
