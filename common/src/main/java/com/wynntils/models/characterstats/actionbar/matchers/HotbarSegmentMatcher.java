/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.matchers;

import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.handlers.actionbar.ActionBarSegmentMatcher;
import com.wynntils.models.characterstats.actionbar.segments.HotbarSegment;

public class HotbarSegmentMatcher implements ActionBarSegmentMatcher {
    // This is the expected string for the hotbar, which should be "static" and not change
    private static final String HOTBAR_STRING = "\uDAFF\uDF98\uE00A\uDAFF\uDFFF\uDAFF\uDF98";

    @Override
    public ActionBarSegment parse(String actionBar) {
        if (!actionBar.contains(HOTBAR_STRING)) return null;
        return new HotbarSegment(HOTBAR_STRING);
    }
}
