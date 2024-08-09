/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.spells.actionbar.matchers;

import com.wynntils.core.WynntilsMod;
import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.handlers.actionbar.ActionBarSegmentMatcher;
import com.wynntils.models.spells.actionbar.segments.SpellSegment;
import com.wynntils.models.spells.type.SpellDirection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpellSegmentMatcher implements ActionBarSegmentMatcher {
    // The start and end of a spell segment, a spacer
    private static final String SEGMENT_SEPARATOR = "\uDAFF\uDFE0";

    // The character for left click
    private static final String LEFT_CLICK = "[\uE100\uE103]";

    // The character for right click
    private static final String RIGHT_CLICK = "[\uE101|\uE104]";

    // The character for no click (yet)
    private static final String NO_CLICK = "[\uE102\uE105]";

    // The separator between the spell clicks (space + arrow)
    private static final String SEPARATOR = "\\s\uE106\\s";

    private static final Pattern SPELL_REGEX = Pattern.compile(SEGMENT_SEPARATOR
            + "(?<first>" + LEFT_CLICK + "|" + RIGHT_CLICK + "|" + NO_CLICK + ")" + SEPARATOR
            + "(?<second>" + LEFT_CLICK + "|" + RIGHT_CLICK + "|" + NO_CLICK + ")" + SEPARATOR
            + "(?<third>" + LEFT_CLICK + "|" + RIGHT_CLICK + "|" + NO_CLICK + ")" + SEGMENT_SEPARATOR);

    @Override
    public ActionBarSegment parse(String actionBar) {
        Matcher matcher = SPELL_REGEX.matcher(actionBar);
        if (!matcher.find()) return null;

        SpellDirection first = fromCharacter(matcher.group("first"));
        SpellDirection second = fromCharacter(matcher.group("second"));
        SpellDirection third = fromCharacter(matcher.group("third"));

        SpellDirection[] directions;

        if (first == null) {
            directions = SpellDirection.NO_SPELL;
        } else if (second == null) {
            directions = new SpellDirection[] {first};
        } else if (third == null) {
            directions = new SpellDirection[] {first, second};
        } else {
            directions = new SpellDirection[] {first, second, third};
        }

        return new SpellSegment(matcher.group(), directions);
    }

    private SpellDirection fromCharacter(String spellCharacter) {
        if (spellCharacter.matches(LEFT_CLICK)) {
            return SpellDirection.LEFT;
        } else if (spellCharacter.matches(RIGHT_CLICK)) {
            return SpellDirection.RIGHT;
        } else if (spellCharacter.matches(NO_CLICK)) {
            return null;
        }

        WynntilsMod.warn("Unknown spell character: " + spellCharacter);
        return null;
    }
}
