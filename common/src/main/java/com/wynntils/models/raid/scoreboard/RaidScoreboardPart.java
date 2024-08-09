/*
 * Copyright © Wynntils 2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.raid.scoreboard;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.scoreboard.ScoreboardPart;
import com.wynntils.handlers.scoreboard.ScoreboardSegment;
import com.wynntils.handlers.scoreboard.type.SegmentMatcher;
import com.wynntils.utils.type.CappedValue;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RaidScoreboardPart extends ScoreboardPart {
    private static final SegmentMatcher RAID_MATCHER = SegmentMatcher.fromPattern("Raid:");
    // Text is split onto two lines, 2nd line says "to the exit"
    private static final Pattern BUFF_PATTERN = Pattern.compile("^Choose a buff or go$");
    private static final Pattern CHALLENGE_COMPLETED_PATTERN = Pattern.compile("^Challenge Completed!$");
    private static final Pattern CHALLENGES_PATTERN = Pattern.compile("^[-—] Challenges: (\\d+)/(\\d+)$");
    private static final Pattern EXIT_PATTERN = Pattern.compile("^Go to the exit$");
    // Split onto two lines, 2nd says "died"
    private static final Pattern FAILED_PATTERN = Pattern.compile("^Too many players have$");
    // Sometimes this will show between challenges so we don't want this change to trigger a new challenge
    private static final Pattern PREPARE_PATTERN = Pattern.compile("^Prepare for the next$");
    private static final Pattern TIMER_PATTERN = Pattern.compile(
            "^[-—] Time Left: (?<hours>\\d+:)?(?<minutes>\\d+):(?<seconds>\\d+)(?: \\[\\+\\d+[msMS]\\])?$");

    @Override
    public SegmentMatcher getSegmentMatcher() {
        return RAID_MATCHER;
    }

    @Override
    public void onSegmentChange(ScoreboardSegment newValue) {
        List<StyledText> content = newValue.getContent();

        if (content.isEmpty()) {
            WynntilsMod.warn("Raid scoreboard segment content is empty");
            return;
        }

        StyledText currentStateLine = content.getFirst();

        if (currentStateLine.matches(EXIT_PATTERN, PartStyle.StyleType.NONE)) {
            Models.Raid.tryEnterChallengeIntermission();
        } else if (currentStateLine.matches(CHALLENGE_COMPLETED_PATTERN, PartStyle.StyleType.NONE)) {
            Models.Raid.completeChallenge();
        } else if (currentStateLine.matches(BUFF_PATTERN, PartStyle.StyleType.NONE)) {
            Models.Raid.enterBuffRoom();
        } else if (currentStateLine.matches(FAILED_PATTERN, PartStyle.StyleType.NONE)) {
            Models.Raid.failedRaid();
        } else if (Models.Raid.getCurrentRaid() != null
                && currentStateLine.matches(
                        Models.Raid.getCurrentRaid().getBossScoreboardPattern(), PartStyle.StyleType.NONE)) {
            Models.Raid.startBossFight();
        } else if (!currentStateLine.matches(PREPARE_PATTERN, PartStyle.StyleType.NONE)) {
            Models.Raid.tryStartChallenge();
        }

        // Some challenges have instructions that take up more than 1 line so we need to loop through the remaining
        // content to find the time and challenges lines
        if (content.size() < 3) {
            WynntilsMod.warn("Raid scoreboard segment content is too short; less than 3");
            return;
        }

        for (StyledText line : content.subList(1, content.size())) {
            Matcher matcher = line.getMatcher(TIMER_PATTERN, PartStyle.StyleType.NONE);
            if (matcher.matches()) {
                int minutes = Integer.parseInt(matcher.group("minutes"));
                int seconds = Integer.parseInt(matcher.group("seconds"));

                if (matcher.group("hours") != null) {
                    int hours = Integer.parseInt(matcher.group("hours"));
                    minutes += hours * 60;
                }

                Models.Raid.setTimeLeft(minutes * 60 + seconds);
                continue;
            }

            // Challenges line should be last so no need to break
            matcher = line.getMatcher(CHALLENGES_PATTERN, PartStyle.StyleType.NONE);
            if (matcher.matches()) {
                Models.Raid.setChallenges(
                        new CappedValue(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
            }
        }
    }

    @Override
    public void onSegmentRemove(ScoreboardSegment segment) {
        // Do nothing here, we will know when the raid is over from patterns
    }

    @Override
    public void reset() {
        // Do nothing here, we will know when the raid is over from patterns
    }

    @Override
    public String toString() {
        return "RaidScoreboardPart{}";
    }
}
