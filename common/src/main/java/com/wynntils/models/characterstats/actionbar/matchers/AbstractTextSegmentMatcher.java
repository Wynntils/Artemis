/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.matchers;

import com.wynntils.core.WynntilsMod;
import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.handlers.actionbar.ActionBarSegmentMatcher;
import com.wynntils.utils.type.CappedValue;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A common base class for matching health and mana text segments in the action bar.
 */
public abstract class AbstractTextSegmentMatcher implements ActionBarSegmentMatcher {
    // High surrogate characters for the positive and negative space characters
    // These are used to tell the difference between health and mana text
    protected static final char POSITIVE_SPACE_HIGH_SURROGATE = '\uDB00';
    protected static final char NEGATIVE_SPACE_HIGH_SURROGATE = '\uDAFF';

    // The separator between the display characters, usually before and after the "/" in the mana/health text
    private static final String SEPARATOR = "\uDB00\uDC02";

    // Possible display character range, extracted from the resource pack/font
    private static final char DISPLAY_CHARACTER_START = '\uE010';
    private static final char DISPLAY_CHARACTER_END = '\uE01F';

    // The translation of the display characters to the actual values
    private static final List<String> DISPLAY_CHARACTER_TRANSLATION =
            List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "k", "m", "b", "t", ".", "/");

    private static final Map<Character, Long> NUMBER_SHORT_MULTIPLIERS = Map.of(
            'k', 1_000L,
            'm', 1_000_000L,
            'b', 1_000_000_000L,
            't', 1_000_000_000_000L);

    private static final Pattern VALUE_PATTERN = Pattern.compile("(?<current>\\d+)/(?<max>\\d+)");

    /**
     * Get the separators of the segment text in the action bar.
     * Both the start and end characters appear only once in the segment.
     * Start and end characters are not always in pairs
     * (a start character does not always have the same, corresponding end character).
     * @return The separators of the segment text
     */
    protected abstract SegmentSeparators segmentSeparators();

    protected abstract ActionBarSegment createSegment(String segmentText, CappedValue value);

    // There is a spacing character before and after actual display characters
    private final Pattern segmentPattern =
            Pattern.compile(".(?<value>[" + DISPLAY_CHARACTER_START + "-" + DISPLAY_CHARACTER_END + SEPARATOR + "]+).");

    @Override
    public ActionBarSegment parse(String actionBar) {
        Matcher matcher = segmentPattern.matcher(actionBar);

        if (!matcher.find()) return null;

        // Loop, because we may find other text segments that we are not looking for
        // (e.g. health text when looking for mana text)
        do {
            String segmentText = matcher.group();

            // Verify that the segment text is surrounded by the correct separators for this segment
            SegmentSeparators separators = segmentSeparators();

            // First unicode character's high surrogate
            char startChar = segmentText.charAt(0);

            // Last unicode character's high surrogate
            char endChar = segmentText.charAt(segmentText.length() - 2);

            // Check if the segment text is surrounded by the correct separators
            boolean validStart = startChar == separators.segmentStart;
            boolean validEnd = endChar == separators.segmentEnd;

            if (!validStart || !validEnd) continue;

            // Parse the value from the display characters
            CappedValue value = valueFromDisplayCharacters(matcher.group("value"));

            return createSegment(segmentText, value);
        } while (matcher.find());

        return null;
    }

    private CappedValue valueFromDisplayCharacters(String displayCharacters) {
        // Remove all the separators
        displayCharacters = displayCharacters.replace(SEPARATOR, "");

        StringBuilder valueBuilder = new StringBuilder();
        for (char current : displayCharacters.toCharArray()) {
            int index = current - DISPLAY_CHARACTER_START;
            if (index < 0 || DISPLAY_CHARACTER_TRANSLATION.size() <= index) {
                WynntilsMod.warn("Unknown display character: " + current);
                continue;
            }

            valueBuilder.append(DISPLAY_CHARACTER_TRANSLATION.get(index));
        }

        String value = valueBuilder.toString();

        // Parse the value as a capped value
        Matcher matcher = VALUE_PATTERN.matcher(value);

        if (!matcher.matches()) {
            WynntilsMod.warn("Could not parse text action bar segment value as capped: " + value);
            return CappedValue.EMPTY;
        }

        long current = parseShortNumber(matcher.group("current"));
        long max = parseShortNumber(matcher.group("max"));

        if (current > Integer.MAX_VALUE) {
            WynntilsMod.warn("Current health/mana value is too large: " + current);
            return CappedValue.EMPTY;
        }

        if (max > Integer.MAX_VALUE) {
            WynntilsMod.warn("Max health/mana value is too large: " + max);
            return CappedValue.EMPTY;
        }

        // The value is capped at 100
        return new CappedValue((int) current, (int) max);
    }

    /**
     * Parses a possibly short number from a string. If the string is not a valid short number, returns 0.
     * @param value The string to parse
     * @return The parsed short number
     */
    private static long parseShortNumber(String value) {
        if (value.isEmpty()) return 0;

        char lastChar = value.charAt(value.length() - 1);
        long multiplier = NUMBER_SHORT_MULTIPLIERS.getOrDefault(lastChar, 1L);

        if (multiplier == 1) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        try {
            return Long.parseLong(value.substring(0, value.length() - 1)) * multiplier;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    protected record SegmentSeparators(char segmentStart, char segmentEnd) {}
}
