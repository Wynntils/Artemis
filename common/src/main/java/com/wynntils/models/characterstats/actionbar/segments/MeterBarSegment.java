/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.segments;

import com.wynntils.models.characterstats.type.MeterBarInfo;

public class MeterBarSegment extends AbstractMeterSegment {
    private final MeterBarInfo meterBarInfo;

    public MeterBarSegment(String segmentText, MeterBarInfo meterBarInfo) {
        super(segmentText);
        this.meterBarInfo = meterBarInfo;
    }

    public MeterBarInfo getMeterBarInfo() {
        return meterBarInfo;
    }

    @Override
    public String toString() {
        return "MeterBarSegment{" + "meterBarInfo=" + meterBarInfo + ", segmentText='" + segmentText + '\'' + '}';
    }
}
