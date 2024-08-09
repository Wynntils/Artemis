/*
 * Copyright © Wynntils 2022-2024.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.objectives;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.scoreboard.ScoreboardPart;
import com.wynntils.handlers.scoreboard.ScoreboardSegment;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractObjectivesScoreboardPart extends ScoreboardPart {
    // §b is guild objective, §a is normal objective and §c is daily objective
    private static final Pattern OBJECTIVE_PATTERN_ONE_LINE =
            Pattern.compile("^§([abc])[- ]\\s§7(.*): *§f(\\d+)§7/(\\d+)$");
    private static final Pattern OBJECTIVE_PATTERN_MULTILINE_START = Pattern.compile("^§([abc])[- ]\\s§7(.*)$");
    private static final Pattern OBJECTIVE_PATTERN_MULTILINE_END = Pattern.compile(".*§f(\\d+)§7/(\\d+)$");
    private static final Pattern SEGMENT_HEADER = Pattern.compile("^§.§l[A-Za-z ]+:.*$");

    private static final StyledText ALL_DONE = StyledText.fromString("§c- §7All done");

    protected List<WynnObjective> parseObjectives(ScoreboardSegment segment) {
        List<WynnObjective> parsedObjectives = new ArrayList<>();

        List<StyledText> actualContent = new ArrayList<>();
        StringBuilder multiLine = new StringBuilder();

        for (StyledText line : segment.getContent()) {
            if (line.matches(OBJECTIVE_PATTERN_ONE_LINE)) {
                actualContent.add(line);
                continue;
            }

            if (line.matches(OBJECTIVE_PATTERN_MULTILINE_START)) {
                if (!multiLine.isEmpty()) {
                    WynntilsMod.error("ObjectiveManager: Multi-line objective start repeatedly:");
                    WynntilsMod.error("Already got: " + multiLine);
                    WynntilsMod.error("Next line: " + line);
                }

                multiLine = new StringBuilder(line.getString());
                continue;
            }

            // If we have started collecting a multiline, keep building it
            if (!multiLine.isEmpty()) {
                multiLine.append(line.getString());
            }

            if (line.getMatcher(OBJECTIVE_PATTERN_MULTILINE_END).matches()) {
                actualContent.add(
                        StyledText.fromString(multiLine.toString().trim().replaceAll(" +", " ")));
                multiLine = new StringBuilder();
            }
        }

        if (!multiLine.isEmpty() && !SEGMENT_HEADER.matcher(multiLine).matches()) {
            WynntilsMod.error("ObjectiveManager: Got a not finished multi-line objective: " + multiLine);
        }

        for (StyledText line : actualContent) {
            Matcher objectiveMatcher = line.getMatcher(OBJECTIVE_PATTERN_ONE_LINE);
            if (!objectiveMatcher.matches()) {
                WynntilsMod.error("ObjectiveManager: Broken objective stored: " + line);
                continue;
            }

            // Determine objective type with the formatting code
            boolean isGuildObjective = Objects.equals(objectiveMatcher.group(1), "b");
            WynnObjective parsed = WynnObjective.parseObjectiveLine(line, isGuildObjective);

            parsedObjectives.add(parsed);
        }
        return parsedObjectives;
    }

    protected static boolean isSegmentAllDone(ScoreboardSegment segment) {
        return segment.getContent().size() == 1
                && segment.getContent().getFirst().equals(ALL_DONE);
    }

    @Override
    public void reset() {
        Models.Objectives.resetObjectives();
    }
}
