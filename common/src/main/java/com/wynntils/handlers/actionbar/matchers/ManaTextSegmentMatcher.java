/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.handlers.actionbar.matchers;

import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.handlers.actionbar.segments.ManaTextSegment;

public class ManaTextSegmentMatcher extends AbstractTextSegmentMatcher {
    private static final SegmentSeparators SEPARATORS =
            new SegmentSeparators(POSITIVE_SPACE_HIGH_SURROGATE, NEGATIVE_SPACE_HIGH_SURROGATE);

    @Override
    protected SegmentSeparators segmentSeparators() {
        return SEPARATORS;
    }

    @Override
    protected ActionBarSegment createSegment(String segmentText) {
        return new ManaTextSegment(segmentText);
    }
}
