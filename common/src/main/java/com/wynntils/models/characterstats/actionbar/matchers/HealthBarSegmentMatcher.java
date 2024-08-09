/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.matchers;

import com.wynntils.core.WynntilsMod;
import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.handlers.actionbar.ActionBarSegmentMatcher;
import com.wynntils.models.characterstats.actionbar.segments.HealthBarSegment;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HealthBarSegmentMatcher implements ActionBarSegmentMatcher {
    // The health bar can have multiple different background characters, collected from the resource pack
    private static final String HEALTH_BACKGROUND_CHARS = "\uE029\uE039\uE049\uE059\uE069";

    // This is the spacer before the health bar background
    private static final String HEALTH_BACKGROUND_SPACER = "\uDAFF\uDF9C";

    // This is the expected pattern for the health bar background (spacer + health bar background)
    private static final Pattern BACKGROUND_PATTERN =
            Pattern.compile(HEALTH_BACKGROUND_SPACER + "[" + HEALTH_BACKGROUND_CHARS + "]");

    // These can be the last character of the health bar
    // It's either a normal bar with an end character, or a highlighted one, when critical damage is taken
    private static final Pattern BAR_END_PATTERN =
            Pattern.compile("(?<normalEnd>\uDB00\uDC20)|(?<highlightedEnd>\uDAFF\uDFBC\uE069\uDB00\uDC1B)");

    // These are the characters that build the health bar, collected from the resource pack
    private static final List<String> HEALTH_BAR_CHARS =
            List.of("\uE020-\uE028", "\uE030-\uE038", "\uE040-\uE048", "\uE051-\uE058");

    // The health bar should have 10 spacer+bar pairs
    private static final Pattern HEALTH_BAR_PATTERN =
            Pattern.compile("(.[" + String.join("", HEALTH_BAR_CHARS) + "]){10}");

    @Override
    public ActionBarSegment parse(String actionBar) {
        Matcher matcher = BACKGROUND_PATTERN.matcher(actionBar);

        if (!matcher.find()) {
            return null;
        }

        int beginIndex = actionBar.indexOf(matcher.group());
        Matcher endMatcher = BAR_END_PATTERN.matcher(actionBar);

        if (!endMatcher.find(beginIndex)) {
            WynntilsMod.warn("Found health bar background, but couldn't find the end of the segment: " + actionBar);
            return null;
        }

        String segmentText = actionBar.substring(beginIndex, endMatcher.end());

        // Remove the background and last space characters to get the characters that build the health bar
        String healthBarText = segmentText.substring(
                matcher.group().length(),
                segmentText.length() - endMatcher.group().length());

        if (!HEALTH_BAR_PATTERN.matcher(healthBarText).matches()) {
            WynntilsMod.warn("Health bar segment seems to match, but the bar text is not expected: " + healthBarText);
            return null;
        }

        return new HealthBarSegment(segmentText);
    }
}
