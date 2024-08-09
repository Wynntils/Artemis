/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.matchers;

import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.models.characterstats.actionbar.segments.MeterEdgeAnimationSegment;
import java.util.List;

public class MeterEdgeAnimationSegmentMatcher extends AbstractMeterSegmentMatcher {
    // Meter edge-animation characters
    // extracted from the resource pack/font
    private static final List<String> METER_ANIMATION_CHARACTERS = List.of("\uE092-\uE095");

    @Override
    protected List<String> getCharacterRange() {
        return METER_ANIMATION_CHARACTERS;
    }

    @Override
    protected ActionBarSegment createSegment(String segmentText, String segmentValue) {
        return new MeterEdgeAnimationSegment(segmentText);
    }
}
