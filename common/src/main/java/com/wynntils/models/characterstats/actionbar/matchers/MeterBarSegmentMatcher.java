/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.characterstats.actionbar.matchers;

import com.wynntils.core.WynntilsMod;
import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.models.characterstats.actionbar.segments.MeterBarSegment;
import com.wynntils.models.characterstats.type.MeterBarInfo;
import com.wynntils.utils.type.CappedValue;
import java.util.List;

public class MeterBarSegmentMatcher extends AbstractMeterSegmentMatcher {
    // Number of steps in the meter bar
    private static final int ACTION_STEPS = 25;
    private static final CappedValue FULL_METER = new CappedValue(ACTION_STEPS, ACTION_STEPS);

    // All possible meter character ranges, without the animation characters, extracted from the resource pack/font
    private static final char FULL_METER_CHARACTER = '\uE090';
    private static final char EMPTY_METER_CHARACTER = '\uE091';
    private static final String DEFAULT_METER_CHARACTERS = FULL_METER_CHARACTER + "-" + EMPTY_METER_CHARACTER;

    // Sprint meter characters
    private static final String SPRINT_METER_CHARACTERS = "\uE0A0-\uE0B8";

    // Breath meter characters
    private static final String BREATH_METER_CHARACTERS = "\uE0C0-\uE0D8";

    private static final List<String> METER_CHARACTERS =
            List.of(DEFAULT_METER_CHARACTERS, SPRINT_METER_CHARACTERS, BREATH_METER_CHARACTERS);

    @Override
    protected List<String> getCharacterRange() {
        return METER_CHARACTERS;
    }

    @Override
    protected ActionBarSegment createSegment(String segmentText, String segmentValue) {
        MeterBarInfo meterBarInfo = fromSegmentText(segmentValue);
        return new MeterBarSegment(segmentText, meterBarInfo);
    }

    private MeterBarInfo fromSegmentText(String segmentValue) {
        // Find the action type from the character range
        MeterBarInfo.MeterActionType actionType;
        char firstActionCharacter;

        char meterChar = segmentValue.charAt(0);

        // Check if the meter bar is empty or full
        if (meterChar == EMPTY_METER_CHARACTER) {
            return MeterBarInfo.EMPTY;
        }
        if (meterChar == FULL_METER_CHARACTER) {
            return new MeterBarInfo(MeterBarInfo.MeterActionType.BOTH, FULL_METER);
        }

        if (segmentValue.matches("[" + SPRINT_METER_CHARACTERS + "]")) {
            actionType = MeterBarInfo.MeterActionType.SPRINT;
            firstActionCharacter = SPRINT_METER_CHARACTERS.charAt(0);
        } else if (segmentValue.matches("[" + BREATH_METER_CHARACTERS + "]")) {
            actionType = MeterBarInfo.MeterActionType.BREATH;
            firstActionCharacter = BREATH_METER_CHARACTERS.charAt(0);
        } else {
            WynntilsMod.warn("Unknown meter bar action type: " + segmentValue);
            return MeterBarInfo.EMPTY;
        }

        // Calculate the current action step
        int actionStep = ACTION_STEPS - (meterChar - firstActionCharacter);

        // Sanity check the action step
        if (actionStep < 0 || actionStep > ACTION_STEPS) {
            WynntilsMod.warn("Invalid meter bar action step: " + actionStep);
            return MeterBarInfo.EMPTY;
        }

        // Return the action type and step
        return new MeterBarInfo(actionType, new CappedValue(actionStep, ACTION_STEPS));
    }
}
