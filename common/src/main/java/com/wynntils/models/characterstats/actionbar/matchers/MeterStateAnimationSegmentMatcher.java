/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.matchers;

import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.models.characterstats.actionbar.segments.MeterStateAnimationSegment;
import java.util.List;

public class MeterStateAnimationSegmentMatcher extends AbstractMeterSegmentMatcher {
    // Meter sprint+breath characters
    // extracted from the resource pack/font
    private static final List<String> METER_STATE_CHARACTERS = List.of("\uE0E0-\uE0E5", "\uE0F0-\uE0F5");

    @Override
    protected List<String> getCharacterRange() {
        return METER_STATE_CHARACTERS;
    }

    @Override
    protected ActionBarSegment createSegment(String segmentText, String segmentValue) {
        return new MeterStateAnimationSegment(segmentText);
    }
}
