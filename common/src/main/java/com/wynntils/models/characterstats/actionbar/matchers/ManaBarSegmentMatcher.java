/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.matchers;

import com.wynntils.core.WynntilsMod;
import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.handlers.actionbar.ActionBarSegmentMatcher;
import com.wynntils.models.characterstats.actionbar.segments.ManaBarSegment;
import java.util.regex.Pattern;

public class ManaBarSegmentMatcher implements ActionBarSegmentMatcher {
    // This is the expected string for the mana bar background (spacer + mana bar background)
    private static final String BACKGROUND_STRING = "\uDB00\uDC1C\uE089";

    // This is the last character of the mana bar
    private static final String LAST_SPACE_STRING = "\uDAFF\uDFA6";

    // These are the character ranges that build the mana bar, collected from the resource pack
    private static final String MANA_BAR_CHARS = "\uE080-\uE088";

    // The mana bar should have 10 spacer+bar pairs
    private static final Pattern MANA_BAR_PATTERN = Pattern.compile("(.[" + MANA_BAR_CHARS + "]){10}");

    @Override
    public ActionBarSegment parse(String actionBar) {
        if (!actionBar.contains(BACKGROUND_STRING)) {
            return null;
        }

        int beginIndex = actionBar.indexOf(BACKGROUND_STRING);
        int endIndex = actionBar.indexOf(LAST_SPACE_STRING, beginIndex);

        if (endIndex == -1) {
            WynntilsMod.warn("Found mana bar background, but couldn't find the end of the segment: " + actionBar);
            return null;
        }

        String segmentText = actionBar.substring(beginIndex, endIndex + LAST_SPACE_STRING.length());

        // Remove the background and last space characters to get the characters that build the mana bar
        String manaBarText =
                segmentText.substring(BACKGROUND_STRING.length(), segmentText.length() - LAST_SPACE_STRING.length());

        if (!MANA_BAR_PATTERN.matcher(manaBarText).matches()) {
            WynntilsMod.warn("Mana bar segment seems to match, but the bar text is not expected: " + manaBarText);
            return null;
        }

        return new ManaBarSegment(segmentText);
    }
}
