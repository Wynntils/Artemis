/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.matchers;

import com.wynntils.core.WynntilsMod;
import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.handlers.actionbar.ActionBarSegmentMatcher;
import com.wynntils.models.characterstats.actionbar.segments.LevelSegment;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LevelSegmentMatcher implements ActionBarSegmentMatcher {
    // The start and end characters' high surrogate for the level segment
    protected static final char SPACER_HIGH_SURROGATE = '\uDAFF';

    // A separator between the level characters
    private static final String SEPARATOR = "\uDAFF\uDFFE";

    // Possible character range for the level segment, collected from the resource pack/font
    private static final char LEVEL_CHAR_START = '\uE000';
    private static final char LEVEL_CHAR_END = '\uE009';

    // The expected pattern for the level segment
    // Allow 6 characters for the level, because the level is 3 digits + 2-3 separator characters
    // There is also a space character at the start and end of the segment
    private static final Pattern LEVEL_PATTERN =
            Pattern.compile(".(?<level>([" + LEVEL_CHAR_START + "-" + LEVEL_CHAR_END + "]" + SEPARATOR + "?){1,6}).");

    @Override
    public ActionBarSegment parse(String actionBar) {
        Matcher matcher = LEVEL_PATTERN.matcher(actionBar);
        if (!matcher.find()) return null;

        String segmentText = matcher.group();

        // First unicode character's high surrogate
        char startChar = segmentText.charAt(0);

        // Last unicode character's high surrogate
        char endChar = segmentText.charAt(segmentText.length() - 2);

        // Check if the segment text is surrounded by the correct separators
        boolean validStart = startChar == SPACER_HIGH_SURROGATE;
        boolean validEnd = endChar == SPACER_HIGH_SURROGATE;

        if (!validStart || !validEnd) return null;

        int level = parseLevel(matcher.group("level"));

        return new LevelSegment(matcher.group(), level);
    }

    private int parseLevel(String levelText) {
        try {
            // Remove the separators from the level text
            levelText = levelText.replace(SEPARATOR, "");

            StringBuilder levelBuilder = new StringBuilder();

            for (char current : levelText.toCharArray()) {
                if (current >= LEVEL_CHAR_START && current <= LEVEL_CHAR_END) {
                    // Each character is a digit, so we can just subtract the start character to get the digit
                    // to get the actual number
                    levelBuilder.append(current - LEVEL_CHAR_START);
                } else {
                    WynntilsMod.warn("Found unexpected character in level segment: " + current);
                }
            }

            return Integer.parseInt(levelBuilder.toString());
        } catch (NumberFormatException e) {
            WynntilsMod.warn("Failed to parse level from segment: " + levelText);
            return 0;
        }
    }
}
